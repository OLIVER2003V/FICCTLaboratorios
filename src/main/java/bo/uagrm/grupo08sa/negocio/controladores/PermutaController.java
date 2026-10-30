package bo.uagrm.grupo08sa.negocio.controladores;

import bo.uagrm.grupo08sa.datos.Fila;
import bo.uagrm.grupo08sa.datos.dao.MensajeDAO;
import bo.uagrm.grupo08sa.datos.dao.PermutaDAO;
import bo.uagrm.grupo08sa.datos.dao.TurnoDAO;
import bo.uagrm.grupo08sa.datos.modelo.Usuario;
import bo.uagrm.grupo08sa.negocio.Busquedas;
import bo.uagrm.grupo08sa.negocio.Comando;
import bo.uagrm.grupo08sa.negocio.Formato;
import bo.uagrm.grupo08sa.negocio.Manejador;
import bo.uagrm.grupo08sa.negocio.NegocioException;
import bo.uagrm.grupo08sa.negocio.Notificaciones;
import bo.uagrm.grupo08sa.negocio.Resultado;
import bo.uagrm.grupo08sa.negocio.Validar;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * CU5. Gestión de Permutas Temporales de Horario y Chat Interno:
 * SOLPMT, ACEPMT, RECPMT, APRPMT, LISPMT, ENVMSJ, LISMSJ.
 * Flujo: el auxiliar solicita → el reemplazante acepta → el Encargado aprueba. La permuta vale solo ese día.
 */
public class PermutaController {

    private static final String[] ESTADOS = {"PENDIENTE", "ACEPTADA", "APROBADA", "RECHAZADA"};
    private static final String[] COLUMNAS =
            {"ID", "Fecha", "Turno", "Lab", "Solicitante", "Reemplazante", "Estado"};

    private final PermutaDAO permutaDAO = new PermutaDAO();
    private final TurnoDAO turnoDAO = new TurnoDAO();
    private final MensajeDAO mensajeDAO = new MensajeDAO();
    private final Busquedas buscar = new Busquedas();
    private final Notificaciones notificaciones;

    public PermutaController(Notificaciones notificaciones) {
        this.notificaciones = notificaciones;
    }

    public void registrarEn(Map<String, Manejador> manejadores) {
        manejadores.put("SOLPMT", this::solicitar);
        manejadores.put("ACEPMT", this::aceptar);
        manejadores.put("RECPMT", this::rechazar);
        manejadores.put("APRPMT", this::aprobar);
        manejadores.put("LISPMT", this::listar);
        manejadores.put("ENVMSJ", this::enviarMensaje);
        manejadores.put("LISMSJ", this::listarMensajes);
    }

    // SOLPMT["15","2026-11-09","219010"]  (id del turno, fecha, código o correo del reemplazante)
    public Resultado solicitar(Comando c, Usuario u) throws NegocioException, SQLException {
        c.exigirParametros(3, "SOLPMT[\"id_turno\",\"fecha\",\"reemplazante\"] (vea sus turnos con LISTUR[\"*\"])");
        Fila sem = buscar.semestreActivo();
        Fila turno = turnoDAO.buscarPorId(Validar.id(c.param(0), "el turno"));
        if (turno == null || turno.entero("semestre_id") != sem.entero("id") || turno.entero("auxiliar_id") != u.getId()) {
            throw new NegocioException("El turno #" + c.param(0) + " no es uno de sus turnos del semestre activo. "
                    + "Vea sus turnos con LISTUR[\"*\"].");
        }
        LocalDate fecha = Validar.fecha(c.param(1), "fecha");
        if (!fecha.isAfter(LocalDate.now())) {
            throw new NegocioException("La permuta debe ser para una fecha futura.");
        }
        buscar.fechaEnSemestre(sem, fecha);
        int dia = turno.entero("dia_semana");
        if (fecha.getDayOfWeek().getValue() != dia) {
            throw new NegocioException("El turno #" + turno.texto("id") + " es los " + Formato.dia(dia) + " y el " + fecha
                    + " es " + Formato.dia(fecha.getDayOfWeek().getValue()) + ".");
        }
        Usuario reemplazante = buscar.usuarioActivoConRol(c.param(2), "AUX", "auxiliar");
        if (reemplazante.getId() == u.getId()) {
            throw new NegocioException("El reemplazante debe ser otro auxiliar.");
        }
        if (!turnoDAO.tieneTurnos(sem.entero("id"), reemplazante.getId())) {
            throw new NegocioException(reemplazante.getNombreCompleto() + " no tiene turnos asignados en el semestre activo.");
        }
        Fila choque = turnoDAO.choqueAuxiliar(sem.entero("id"), reemplazante.getId(), dia,
                turno.hora("hora_inicio"), turno.hora("hora_fin"));
        if (choque == null) {
            for (Fila t : turnoDAO.recibidosPorPermuta(reemplazante.getId(), fecha)) {
                if (t.hora("hora_inicio").isBefore(turno.hora("hora_fin")) && t.hora("hora_fin").isAfter(turno.hora("hora_inicio"))) {
                    choque = t;
                }
            }
        }
        if (choque != null) {
            throw new NegocioException(reemplazante.getNombreCompleto() + " ya tiene un turno en esa franja ("
                    + Formato.franja(choque.hora("hora_inicio"), choque.hora("hora_fin")) + ").");
        }
        Fila existente = permutaDAO.activa(turno.entero("id"), fecha);
        if (existente != null) {
            throw new NegocioException("Ya existe la permuta #" + existente.texto("id") + " (" + existente.texto("estado")
                    + ") para ese turno y fecha.");
        }
        int id = permutaDAO.insertar(turno.entero("id"), u.getId(), reemplazante.getId(), fecha);
        notificaciones.aUsuario(reemplazante.getId(), "Solicitud de permuta #" + id,
                u.getNombreCompleto() + " le pide cubrir su turno del " + Formato.dia(dia) + " " + fecha + " de "
                        + Formato.franja(turno.hora("hora_inicio"), turno.hora("hora_fin"))
                        + (turno.texto("lab").isEmpty() ? "" : " en " + turno.texto("lab"))
                        + ". Acepte con ACEPMT[\"" + id + "\"] o rechace con RECPMT[\"" + id + "\"].", null);
        return Resultado.tabla("Permuta solicitada", "Se notificó a " + reemplazante.getNombreCompleto()
                + ". Después de que acepte, el Encargado debe aprobarla.", COLUMNAS, List.<String[]>of(fila(permutaDAO.buscarPorId(id))));
    }

    // ACEPMT["7"]  (solo el reemplazante)
    public Resultado aceptar(Comando c, Usuario u) throws NegocioException, SQLException {
        c.exigirParametros(1, "ACEPMT[\"id\"]");
        Fila p = buscarPermuta(c.param(0));
        if (p.entero("reemplazante_id") != u.getId()) {
            throw new NegocioException("Solo el reemplazante (" + p.texto("reemplazante") + ") puede aceptar la permuta #"
                    + p.texto("id") + ".");
        }
        exigirEstado(p, "PENDIENTE");
        exigirFutura(p);
        permutaDAO.cambiarEstado(p.entero("id"), "ACEPTADA", null);
        notificaciones.aEncargados("Permuta #" + p.texto("id") + " por aprobar",
                p.texto("reemplazante") + " aceptó cubrir el turno de " + p.texto("solicitante") + " el " + p.texto("fecha")
                        + " de " + Formato.franja(p.hora("hora_inicio"), p.hora("hora_fin")) + ". Apruebe con APRPMT[\""
                        + p.texto("id") + "\"] o rechace con RECPMT[\"" + p.texto("id") + "\"].", null, u.getId());
        notificaciones.aUsuario(p.entero("solicitante_id"), "Permuta #" + p.texto("id") + " aceptada",
                p.texto("reemplazante") + " aceptó su permuta del " + p.texto("fecha") + ". Falta la aprobación del Encargado.", null);
        return Resultado.tabla("Permuta aceptada", "Se notificó al Encargado para su aprobación.",
                COLUMNAS, List.<String[]>of(fila(permutaDAO.buscarPorId(p.entero("id")))));
    }

    // RECPMT["7"]  (el reemplazante mientras está PENDIENTE, o el Encargado)
    public Resultado rechazar(Comando c, Usuario u) throws NegocioException, SQLException {
        c.exigirParametros(1, "RECPMT[\"id\"]");
        Fila p = buscarPermuta(c.param(0));
        boolean esEncargado = "ENC".equals(u.getRolCodigo());
        String estado = p.texto("estado");
        if (esEncargado) {
            if (!estado.equals("PENDIENTE") && !estado.equals("ACEPTADA")) {
                throw new NegocioException("La permuta #" + p.texto("id") + " ya fue resuelta (" + estado + ").");
            }
        } else if (p.entero("reemplazante_id") == u.getId()) {
            exigirEstado(p, "PENDIENTE");
        } else {
            throw new NegocioException("Solo el reemplazante o el Encargado pueden rechazar la permuta #" + p.texto("id") + ".");
        }
        permutaDAO.cambiarEstado(p.entero("id"), "RECHAZADA", esEncargado ? u.getId() : null);
        String texto = "La permuta #" + p.texto("id") + " del " + p.texto("fecha") + " fue rechazada por "
                + u.getNombreCompleto() + ". Cada auxiliar mantiene su turno.";
        notificaciones.aUsuario(p.entero("solicitante_id"), "Permuta #" + p.texto("id") + " rechazada", texto, null);
        if (esEncargado) {
            notificaciones.aUsuario(p.entero("reemplazante_id"), "Permuta #" + p.texto("id") + " rechazada", texto, null);
        }
        return Resultado.tabla("Permuta rechazada", "Se notificó a los involucrados.",
                COLUMNAS, List.<String[]>of(fila(permutaDAO.buscarPorId(p.entero("id")))));
    }

    // APRPMT["7"]  (Encargado; la permuta debe estar ACEPTADA)
    public Resultado aprobar(Comando c, Usuario u) throws NegocioException, SQLException {
        c.exigirParametros(1, "APRPMT[\"id\"]");
        Fila p = buscarPermuta(c.param(0));
        if ("PENDIENTE".equals(p.texto("estado"))) {
            throw new NegocioException("El reemplazante todavía no aceptó la permuta #" + p.texto("id") + ".");
        }
        exigirEstado(p, "ACEPTADA");
        exigirFutura(p);
        permutaDAO.cambiarEstado(p.entero("id"), "APROBADA", u.getId());
        String texto = "Permuta aprobada: el " + p.texto("fecha") + " de " + Formato.franja(p.hora("hora_inicio"), p.hora("hora_fin"))
                + " el turno de " + p.texto("solicitante") + " lo cubre " + p.texto("reemplazante")
                + ". La asistencia de ese día se controla con este cambio; al día siguiente cada uno vuelve a su horario.";
        notificaciones.aUsuario(p.entero("solicitante_id"), "Permuta #" + p.texto("id") + " APROBADA", texto, null);
        notificaciones.aUsuario(p.entero("reemplazante_id"), "Permuta #" + p.texto("id") + " APROBADA", texto, null);
        return Resultado.tabla("Permuta aprobada", "Se notificó a ambos auxiliares.",
                COLUMNAS, List.<String[]>of(fila(permutaDAO.buscarPorId(p.entero("id")))));
    }

    // LISPMT["*"]  LISPMT["ACEPTADA"]
    public Resultado listar(Comando c, Usuario u) throws NegocioException, SQLException {
        c.exigirEntre(0, 1, "LISPMT[\"*\"] o LISPMT[\"estado\"]");
        String estado = c.cantidad() == 1 ? Validar.opcion(c.param(0), "estado", ESTADOS) : null;
        boolean todas = "ENC".equals(u.getRolCodigo());
        List<String[]> filas = new ArrayList<>();
        for (Fila p : permutaDAO.listar(todas ? null : u.getId(), estado)) {
            filas.add(fila(p));
        }
        return Resultado.tabla(todas ? "Permutas" : "Mis permutas", filas.size() + " permuta(s).", COLUMNAS, filas);
    }

    // ENVMSJ["219010","¿Puedes cubrir mi turno del lunes?"]  (código o correo del destinatario)
    public Resultado enviarMensaje(Comando c, Usuario u) throws NegocioException, SQLException {
        c.exigirParametros(2, "ENVMSJ[\"destinatario\",\"mensaje\"]");
        Usuario destino = buscar.usuario(c.param(0));
        if (!destino.estaActivo()) {
            throw new NegocioException(destino.getNombreCompleto() + " está dado de baja.");
        }
        String contenido = Validar.texto(c.param(1), "mensaje", 2000);
        int id = mensajeDAO.insertar(u.getId(), destino.getId(), contenido);
        notificaciones.aUsuario(destino.getId(), "Mensaje interno de " + u.getNombreCompleto(),
                contenido + "  —  Responda con ENVMSJ[\"" + u.getCodigo() + "\",\"su respuesta\"]; "
                        + "vea todos sus mensajes con LISMSJ[\"*\"].", null);
        return Resultado.exito("Mensaje enviado", "Mensaje #" + id + " enviado a " + destino.getNombreCompleto() + ".");
    }

    // LISMSJ["*"]  LISMSJ["NOLEIDOS"]   (al listarlos quedan marcados como leídos)
    public Resultado listarMensajes(Comando c, Usuario u) throws NegocioException, SQLException {
        c.exigirEntre(0, 1, "LISMSJ[\"*\"] o LISMSJ[\"NOLEIDOS\"]");
        boolean soloNoLeidos = c.cantidad() == 1 && Validar.opcion(c.param(0), "filtro", "NOLEIDOS").equals("NOLEIDOS");
        List<String[]> filas = new ArrayList<>();
        int nuevos = 0;
        for (Fila m : mensajeDAO.recibidos(u.getId(), soloNoLeidos)) {
            boolean leido = Boolean.TRUE.equals(m.get("leido"));
            nuevos += leido ? 0 : 1;
            filas.add(new String[]{m.texto("id"), m.texto("enviado_en"), m.texto("remitente") + " (" + m.texto("remitente_codigo") + ")",
                    m.texto("contenido"), leido ? "Leído" : "NUEVO"});
        }
        mensajeDAO.marcarLeidos(u.getId());
        return Resultado.tabla("Mensajes recibidos", filas.size() + " mensaje(s), " + nuevos + " nuevo(s).",
                new String[]{"ID", "Fecha", "De", "Mensaje", "Estado"}, filas);
    }

    private Fila buscarPermuta(String id) throws NegocioException, SQLException {
        Fila p = permutaDAO.buscarPorId(Validar.id(id, "la permuta"));
        if (p == null) {
            throw new NegocioException("No existe la permuta #" + id + ".");
        }
        return p;
    }

    private static void exigirEstado(Fila p, String estado) throws NegocioException {
        if (!estado.equals(p.texto("estado"))) {
            throw new NegocioException("La permuta #" + p.texto("id") + " ya fue resuelta (" + p.texto("estado") + ").");
        }
    }

    private static void exigirFutura(Fila p) throws NegocioException {
        if (p.fecha("fecha").isBefore(LocalDate.now())) {
            throw new NegocioException("La permuta #" + p.texto("id") + " es de una fecha pasada.");
        }
    }

    private static String[] fila(Fila p) {
        return new String[]{p.texto("id"), p.texto("fecha"), Formato.dia(p.entero("dia_semana")) + " "
                + Formato.franja(p.hora("hora_inicio"), p.hora("hora_fin")), p.texto("lab"),
                p.texto("solicitante"), p.texto("reemplazante"), p.texto("estado")};
    }
}
