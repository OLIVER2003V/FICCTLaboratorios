package bo.uagrm.grupo08sa.negocio.controladores;

import bo.uagrm.grupo08sa.datos.Fila;
import bo.uagrm.grupo08sa.datos.dao.ReservaDAO;
import bo.uagrm.grupo08sa.datos.modelo.Usuario;
import bo.uagrm.grupo08sa.negocio.Busquedas;
import bo.uagrm.grupo08sa.negocio.Comando;
import bo.uagrm.grupo08sa.negocio.Disponibilidad;
import bo.uagrm.grupo08sa.negocio.Formato;
import bo.uagrm.grupo08sa.negocio.Manejador;
import bo.uagrm.grupo08sa.negocio.NegocioException;
import bo.uagrm.grupo08sa.negocio.Notificaciones;
import bo.uagrm.grupo08sa.negocio.Resultado;
import bo.uagrm.grupo08sa.negocio.Validar;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * CU4. Gestión de Reservas, Notificaciones y Resolución de Conflictos:
 * INSRES, APRRES, RECRES, CANRES, LISRES.
 * Ante un conflicto conserva el laboratorio el docente con horario asignado y se proponen laboratorios libres.
 */
public class ReservaController {

    private static final String[] TIPOS = {"EXAMEN", "RECUPERACION", "PRACTICA", "EVENTO"};
    private static final String[] ESTADOS = {"PENDIENTE", "APROBADA", "RECHAZADA", "CANCELADA"};
    private static final String[] COLUMNAS =
            {"ID", "Lab", "Fecha", "Inicio", "Fin", "Tipo", "Motivo", "Solicitante", "Estado", "Observación"};

    private final ReservaDAO reservaDAO = new ReservaDAO();
    private final Busquedas buscar = new Busquedas();
    private final Disponibilidad disponibilidad = new Disponibilidad();
    private final Notificaciones notificaciones;

    public ReservaController(Notificaciones notificaciones) {
        this.notificaciones = notificaciones;
    }

    public void registrarEn(Map<String, Manejador> manejadores) {
        manejadores.put("INSRES", this::solicitar);
        manejadores.put("APRRES", this::aprobar);
        manejadores.put("RECRES", this::rechazar);
        manejadores.put("CANRES", this::cancelar);
        manejadores.put("LISRES", this::listar);
    }

    // INSRES["LAB-41","2026-11-03","08:00","10:00","EXAMEN","Primer parcial INF513"]
    public Resultado solicitar(Comando c, Usuario u) throws NegocioException, SQLException {
        c.exigirParametros(6, "INSRES[\"laboratorio\",\"fecha\",\"inicio\",\"fin\",\"tipo\",\"motivo\"]");
        Fila lab = buscar.laboratorio(c.param(0));
        LocalDate fecha = Validar.fecha(c.param(1), "fecha");
        LocalTime inicio = Validar.hora(c.param(2), "hora de inicio");
        LocalTime fin = Validar.hora(c.param(3), "hora de fin");
        Validar.rangoHoras(inicio, fin);
        String tipo = Validar.opcion(c.param(4), "tipo", TIPOS);
        String motivo = Validar.texto(c.param(5), "motivo", 200);

        buscar.fechaEnSemestre(buscar.semestreActivo(), fecha);
        exigirFutura(fecha, inicio);
        Disponibilidad.Ocupacion o = disponibilidad.ocupacion(lab, fecha, inicio, fin, 0);
        if (o != null) {
            throw new NegocioException(rechazo(lab, fecha, inicio, fin, o));
        }

        int pendientes = reservaDAO.pendientesSuperpuestas(lab.entero("id"), fecha, inicio, fin, 0);
        int id = reservaDAO.insertar(lab.entero("id"), u.getId(), fecha, inicio, fin, tipo, motivo);
        notificaciones.aEncargados("Nueva solicitud de reserva #" + id,
                u.getNombreCompleto() + " (" + u.getRolNombre() + ") solicita " + lab.texto("codigo") + " el " + fecha + " de "
                        + Formato.franja(inicio, fin) + " para " + tipo + ": " + motivo + ". "
                        + "Apruebe con APRRES[\"" + id + "\"] o rechace con RECRES[\"" + id + "\",\"motivo\"].", id, u.getId());

        String nota = pendientes == 0 ? "" : " Hay " + pendientes
                + " otra(s) solicitud(es) pendiente(s) en esa franja; el Encargado decidirá.";
        return Resultado.tabla("Reserva solicitada", "La solicitud quedó PENDIENTE y se notificó al Encargado." + nota,
                COLUMNAS, List.<String[]>of(fila(reservaDAO.buscarPorId(id))));
    }

    // APRRES["12"]
    public Resultado aprobar(Comando c, Usuario u) throws NegocioException, SQLException {
        c.exigirParametros(1, "APRRES[\"id\"]");
        Fila r = buscarPendiente(c.param(0));
        LocalDate fecha = r.fecha("fecha");
        LocalTime inicio = r.hora("hora_inicio");
        LocalTime fin = r.hora("hora_fin");
        exigirFutura(fecha, inicio);
        Fila lab = buscar.laboratorio(r.texto("lab"));
        // Se vuelve a verificar: otra reserva pudo aprobarse después de esta solicitud
        Disponibilidad.Ocupacion o = disponibilidad.ocupacion(lab, fecha, inicio, fin, r.entero("id"));
        if (o != null) {
            throw new NegocioException("No se puede aprobar: " + rechazo(lab, fecha, inicio, fin, o)
                    + " Rechácela con RECRES[\"" + r.texto("id") + "\",\"motivo\"].");
        }
        reservaDAO.resolver(r.entero("id"), "APROBADA", u.getId(), null);

        String detalle = lab.texto("codigo") + " el " + fecha + " de " + Formato.franja(inicio, fin)
                + " (" + r.texto("tipo") + ": " + r.texto("motivo") + ")";
        notificaciones.aUsuario(r.entero("solicitante_id"), "Reserva #" + r.texto("id") + " APROBADA",
                "Su reserva de " + detalle + " fue aprobada por " + u.getNombreCompleto() + ".", r.entero("id"));
        for (int auxiliarId : buscar.auxiliaresDeTurno(lab.entero("id"), fecha, inicio, fin)) {
            notificaciones.aUsuario(auxiliarId, "Reserva aprobada en su turno",
                    "Se aprobó la reserva #" + r.texto("id") + " de " + r.texto("solicitante") + ": " + detalle
                            + ". Prepare el laboratorio.", r.entero("id"));
        }
        int pendientes = reservaDAO.pendientesSuperpuestas(lab.entero("id"), fecha, inicio, fin, r.entero("id"));
        String nota = pendientes == 0 ? "" : " Quedan " + pendientes
                + " solicitud(es) pendiente(s) en esa franja que ahora chocarán; rechácelas con RECRES.";
        return Resultado.tabla("Reserva aprobada", "Se notificó al solicitante y al auxiliar de turno." + nota,
                COLUMNAS, List.<String[]>of(fila(reservaDAO.buscarPorId(r.entero("id")))));
    }

    // RECRES["12","El laboratorio se usará para la PSA"]
    public Resultado rechazar(Comando c, Usuario u) throws NegocioException, SQLException {
        c.exigirParametros(2, "RECRES[\"id\",\"motivo\"]");
        Fila r = buscarPendiente(c.param(0));
        String motivo = Validar.texto(c.param(1), "motivo del rechazo", 200);
        reservaDAO.resolver(r.entero("id"), "RECHAZADA", u.getId(), motivo);
        notificaciones.aUsuario(r.entero("solicitante_id"), "Reserva #" + r.texto("id") + " RECHAZADA",
                "Su reserva de " + r.texto("lab") + " el " + r.texto("fecha") + " de "
                        + Formato.franja(r.hora("hora_inicio"), r.hora("hora_fin")) + " fue rechazada. Motivo: " + motivo
                        + ". Consulte otros laboratorios libres con CONDIS[\"" + r.texto("fecha") + "\",\""
                        + r.texto("hora_inicio") + "\",\"" + r.texto("hora_fin") + "\"].", r.entero("id"));
        return Resultado.tabla("Reserva rechazada", "Se notificó al solicitante.",
                COLUMNAS, List.<String[]>of(fila(reservaDAO.buscarPorId(r.entero("id")))));
    }

    // CANRES["12"]
    public Resultado cancelar(Comando c, Usuario u) throws NegocioException, SQLException {
        c.exigirParametros(1, "CANRES[\"id\"]");
        Fila r = buscarReserva(c.param(0));
        boolean esEncargado = "ENC".equals(u.getRolCodigo());
        if (r.entero("solicitante_id") != u.getId() && !esEncargado) {
            throw new NegocioException("Solo puede cancelar sus propias reservas.");
        }
        String estado = r.texto("estado");
        if (!estado.equals("PENDIENTE") && !estado.equals("APROBADA")) {
            throw new NegocioException("La reserva #" + r.texto("id") + " ya está " + estado + ".");
        }
        if (r.fecha("fecha").isBefore(LocalDate.now())) {
            throw new NegocioException("La reserva #" + r.texto("id") + " es de una fecha pasada; ya no se puede cancelar.");
        }
        reservaDAO.cambiarEstado(r.entero("id"), "CANCELADA");

        String detalle = "La reserva #" + r.texto("id") + " de " + r.texto("lab") + " el " + r.texto("fecha") + " de "
                + Formato.franja(r.hora("hora_inicio"), r.hora("hora_fin")) + " fue cancelada por " + u.getNombreCompleto() + ".";
        notificaciones.aEncargados("Reserva #" + r.texto("id") + " cancelada", detalle, r.entero("id"), u.getId());
        if (r.entero("solicitante_id") != u.getId()) {
            notificaciones.aUsuario(r.entero("solicitante_id"), "Reserva #" + r.texto("id") + " cancelada", detalle, r.entero("id"));
        }
        if (estado.equals("APROBADA")) {
            for (int auxiliarId : buscar.auxiliaresDeTurno(r.entero("laboratorio_id"), r.fecha("fecha"),
                    r.hora("hora_inicio"), r.hora("hora_fin"))) {
                notificaciones.aUsuario(auxiliarId, "Reserva cancelada en su turno", detalle, r.entero("id"));
            }
        }
        return Resultado.tabla("Reserva cancelada", "El laboratorio queda libre en esa franja.",
                COLUMNAS, List.<String[]>of(fila(reservaDAO.buscarPorId(r.entero("id")))));
    }

    // LISRES["*"]  LISRES["PENDIENTE"]   (el Encargado ve todas; los demás, solo las suyas)
    public Resultado listar(Comando c, Usuario u) throws NegocioException, SQLException {
        c.exigirEntre(0, 1, "LISRES[\"*\"] o LISRES[\"estado\"]");
        String estado = c.cantidad() == 1 ? Validar.opcion(c.param(0), "estado", ESTADOS) : null;
        boolean todas = "ENC".equals(u.getRolCodigo());
        List<String[]> filas = new ArrayList<>();
        for (Fila r : reservaDAO.listar(todas ? null : u.getId(), estado)) {
            filas.add(fila(r));
        }
        return Resultado.tabla(todas ? "Reservas" : "Mis reservas", filas.size() + " reserva(s)"
                + (estado == null ? "." : " en estado " + estado + "."), COLUMNAS, filas);
    }

    private String rechazo(Fila lab, LocalDate fecha, LocalTime inicio, LocalTime fin, Disponibilidad.Ocupacion o)
            throws SQLException {
        String texto = lab.texto("codigo") + " no está disponible el " + fecha + " de " + Formato.franja(inicio, fin)
                + ": " + o.detalle() + ".";
        if (o.tipo().equals("CLASE") || o.tipo().equals("RESERVA") || o.tipo().equals("ESTADO")) {
            texto += " Laboratorios libres en esa franja: " + Disponibilidad.codigos(disponibilidad.libres(fecha, inicio, fin)) + ".";
        }
        return texto;
    }

    private static void exigirFutura(LocalDate fecha, LocalTime inicio) throws NegocioException {
        if (LocalDateTime.of(fecha, inicio).isBefore(LocalDateTime.now())) {
            throw new NegocioException("La fecha y hora " + fecha + " " + Formato.hora(inicio) + " ya pasaron.");
        }
    }

    private Fila buscarReserva(String id) throws NegocioException, SQLException {
        Fila r = reservaDAO.buscarPorId(Validar.id(id, "la reserva"));
        if (r == null) {
            throw new NegocioException("No existe la reserva #" + id + ".");
        }
        return r;
    }

    private Fila buscarPendiente(String id) throws NegocioException, SQLException {
        Fila r = buscarReserva(id);
        if (!"PENDIENTE".equals(r.texto("estado"))) {
            throw new NegocioException("La reserva #" + id + " ya fue resuelta (" + r.texto("estado") + ").");
        }
        return r;
    }

    private static String[] fila(Fila r) {
        return new String[]{r.texto("id"), r.texto("lab"), r.texto("fecha"), r.texto("hora_inicio"), r.texto("hora_fin"),
                r.texto("tipo"), r.texto("motivo"), r.texto("solicitante"), r.texto("estado"), r.texto("observacion")};
    }
}
