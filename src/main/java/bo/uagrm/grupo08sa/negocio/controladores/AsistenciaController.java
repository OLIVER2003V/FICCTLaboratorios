package bo.uagrm.grupo08sa.negocio.controladores;

import bo.uagrm.grupo08sa.datos.Fila;
import bo.uagrm.grupo08sa.datos.dao.AsistenciaDAO;
import bo.uagrm.grupo08sa.datos.dao.FechaBloqueadaDAO;
import bo.uagrm.grupo08sa.datos.dao.PermisoDAO;
import bo.uagrm.grupo08sa.datos.dao.PermutaDAO;
import bo.uagrm.grupo08sa.datos.dao.SemestreDAO;
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
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * CU7. Gestión de Supervisión Asistencia de Auxiliares: INSTUR, LISTUR, MARENT, MARSAL, INSPMS, LISASI
 * y la tarea automática que marca las faltas.
 *
 * Reglas de tiempo (respecto a la hora de inicio del turno):
 *   - la entrada se acepta desde 15 min antes;
 *   - hasta 10 min tarde es PUNTUAL, hasta 30 min es ATRASO;
 *   - pasados 30 min sin entrada se registra FALTA (o PERMISO si tiene uno ese día).
 */
public class AsistenciaController {

    public static final int ANTICIPACION_MIN = 15;
    public static final int TOLERANCIA_PUNTUAL_MIN = 10;
    public static final int TOLERANCIA_FALTA_MIN = 30;
    private static final String[] COLUMNAS_TURNO = {"ID", "Auxiliar", "Día", "Inicio", "Fin", "Lab"};
    private static final String[] COLUMNAS_ASISTENCIA =
            {"Fecha", "Auxiliar", "Turno", "Lab", "Entrada", "Salida", "Estado"};

    private final TurnoDAO turnoDAO = new TurnoDAO();
    private final PermutaDAO permutaDAO = new PermutaDAO();
    private final AsistenciaDAO asistenciaDAO = new AsistenciaDAO();
    private final PermisoDAO permisoDAO = new PermisoDAO();
    private final SemestreDAO semestreDAO = new SemestreDAO();
    private final FechaBloqueadaDAO fechaBloqueadaDAO = new FechaBloqueadaDAO();
    private final Busquedas buscar = new Busquedas();
    private final Notificaciones notificaciones;

    public AsistenciaController(Notificaciones notificaciones) {
        this.notificaciones = notificaciones;
    }

    public void registrarEn(Map<String, Manejador> manejadores) {
        manejadores.put("INSTUR", this::asignarTurno);
        manejadores.put("LISTUR", this::listarTurnos);
        manejadores.put("MARENT", this::marcarEntrada);
        manejadores.put("MARSAL", this::marcarSalida);
        manejadores.put("INSPMS", this::registrarPermiso);
        manejadores.put("LISASI", this::consultarAsistencia);
    }

    // INSTUR["219010","LUNES","08:00","12:00"]  INSTUR["219010","SABADO","08:00","12:00","LAB-41"]
    public Resultado asignarTurno(Comando c, Usuario u) throws NegocioException, SQLException {
        c.exigirEntre(4, 5, "INSTUR[\"auxiliar\",\"dia\",\"inicio\",\"fin\"] o INSTUR[\"auxiliar\",\"dia\",\"inicio\",\"fin\",\"laboratorio\"]");
        Fila sem = buscar.semestreActivo();
        Usuario aux = buscar.usuarioActivoConRol(c.param(0), "AUX", "auxiliar");
        int dia = Validar.dia(c.param(1));
        LocalTime inicio = Validar.hora(c.param(2), "hora de inicio");
        LocalTime fin = Validar.hora(c.param(3), "hora de fin");
        Validar.rangoHoras(inicio, fin);
        String labCodigo = c.paramOpcional(4);
        Integer labId = labCodigo.isEmpty() || labCodigo.equals("*") ? null : buscar.laboratorio(labCodigo).entero("id");

        Fila choque = turnoDAO.choqueAuxiliar(sem.entero("id"), aux.getId(), dia, inicio, fin);
        if (choque != null) {
            throw new NegocioException(aux.getNombreCompleto() + " ya tiene el turno #" + choque.texto("id") + " el "
                    + Formato.dia(dia) + " de " + Formato.franja(choque.hora("hora_inicio"), choque.hora("hora_fin")) + ".");
        }
        int id = turnoDAO.insertar(sem.entero("id"), aux.getId(), labId, dia, inicio, fin);
        double horas = Formato.horas(inicio, fin);
        String nota = dia <= 5 && horas != 4 ? " Nota: los turnos de lunes a viernes suelen ser de 4 horas (este es de "
                + Formato.decimal(horas) + " h)." : "";
        return Resultado.tabla("Turno asignado", "Turno asignado a " + aux.getNombreCompleto() + "." + nota,
                COLUMNAS_TURNO, List.<String[]>of(filaTurno(turnoDAO.buscarPorId(id))));
    }

    // LISTUR["*"]  LISTUR["219010"]  (el auxiliar ve solo los suyos)
    public Resultado listarTurnos(Comando c, Usuario u) throws NegocioException, SQLException {
        c.exigirEntre(0, 1, "LISTUR[\"*\"] o LISTUR[\"auxiliar\"]");
        Fila sem = buscar.semestreActivo();
        Integer auxId;
        if (!"ENC".equals(u.getRolCodigo())) {
            auxId = u.getId();
        } else {
            auxId = c.cantidad() == 1 ? buscar.usuario(c.param(0)).getId() : null;
        }
        List<String[]> filas = new ArrayList<>();
        for (Fila t : turnoDAO.listar(sem.entero("id"), auxId)) {
            filas.add(filaTurno(t));
        }
        return Resultado.tabla("Turnos " + sem.texto("gestion"), filas.size() + " turno(s).", COLUMNAS_TURNO, filas);
    }

    // MARENT["*"]
    public Resultado marcarEntrada(Comando c, Usuario u) throws NegocioException, SQLException {
        LocalDate hoy = LocalDate.now();
        LocalTime ahora = LocalTime.now().truncatedTo(ChronoUnit.MINUTES);
        List<Fila> turnos = turnosDeHoy(u.getId(), hoy);
        Fila turno = null;
        for (Fila t : turnos) {
            if (!ahora.isBefore(t.hora("hora_inicio").minusMinutes(ANTICIPACION_MIN)) && ahora.isBefore(t.hora("hora_fin"))) {
                turno = t;
                break;
            }
        }
        if (turno == null) {
            throw new NegocioException("No tiene un turno vigente a las " + Formato.hora(ahora) + ". "
                    + (turnos.isEmpty() ? "Hoy no tiene turnos." : "Sus turnos de hoy: " + describir(turnos) + "."));
        }
        Fila previa = asistenciaDAO.buscar(turno.entero("id"), hoy);
        if (previa != null) {
            throw new NegocioException("Ya hay un registro para este turno hoy (" + previa.texto("estado")
                    + (previa.texto("hora_entrada").isEmpty() ? "" : ", entrada " + previa.texto("hora_entrada")) + ").");
        }
        long minutosTarde = Duration.between(turno.hora("hora_inicio"), ahora).toMinutes();
        String estado = minutosTarde <= TOLERANCIA_PUNTUAL_MIN ? "PUNTUAL"
                : minutosTarde <= TOLERANCIA_FALTA_MIN ? "ATRASO" : "FALTA";
        asistenciaDAO.insertar(turno.entero("id"), u.getId(), hoy, ahora, estado);

        String mensaje;
        if (estado.equals("FALTA")) {
            mensaje = "Llegó " + minutosTarde + " min tarde; supera la tolerancia de " + TOLERANCIA_FALTA_MIN
                    + " min, por lo que se registró FALTA.";
            notificaciones.aEncargados("Falta de " + u.getNombreCompleto(), u.getNombreCompleto() + " marcó entrada "
                    + minutosTarde + " min tarde al turno de " + Formato.franja(turno.hora("hora_inicio"), turno.hora("hora_fin"))
                    + "; se registró FALTA.", null, u.getId());
        } else {
            mensaje = estado.equals("PUNTUAL") ? "Entrada registrada a tiempo."
                    : "Entrada registrada con " + minutosTarde + " min de atraso.";
        }
        return Resultado.tabla("Entrada: " + estado, mensaje + " No olvide marcar su salida con MARSAL[\"*\"].",
                COLUMNAS_ASISTENCIA, List.<String[]>of(filaAsistencia(asistenciaDAO.buscar(turno.entero("id"), hoy))));
    }

    // MARSAL["*"]
    public Resultado marcarSalida(Comando c, Usuario u) throws NegocioException, SQLException {
        LocalDate hoy = LocalDate.now();
        LocalTime ahora = LocalTime.now().truncatedTo(ChronoUnit.MINUTES);
        Fila a = asistenciaDAO.abierta(u.getId(), hoy);
        if (a == null) {
            throw new NegocioException("No tiene una entrada registrada hoy sin salida. Marque primero su entrada con MARENT[\"*\"].");
        }
        if (ahora.isAfter(a.hora("hora_fin").plusHours(1))) {
            throw new NegocioException("La salida está fuera del turno (" + Formato.franja(a.hora("hora_inicio"), a.hora("hora_fin"))
                    + "). Consulte con el Encargado.");
        }
        asistenciaDAO.registrarSalida(a.entero("id"), ahora);
        String nota = ahora.isBefore(a.hora("hora_fin"))
                ? " Atención: salida anticipada (" + Duration.between(ahora, a.hora("hora_fin")).toMinutes() + " min antes del fin)."
                : "";
        return Resultado.tabla("Salida registrada", "Salida a las " + Formato.hora(ahora) + "." + nota,
                COLUMNAS_ASISTENCIA, List.<String[]>of(filaAsistencia(asistenciaDAO.buscar(a.entero("turno_id"), hoy))));
    }

    // INSPMS["219010","2026-11-05","Examen de Cálculo II"]
    public Resultado registrarPermiso(Comando c, Usuario u) throws NegocioException, SQLException {
        c.exigirParametros(3, "INSPMS[\"auxiliar\",\"fecha\",\"motivo\"]");
        Usuario aux = buscar.usuarioActivoConRol(c.param(0), "AUX", "auxiliar");
        LocalDate fecha = Validar.fecha(c.param(1), "fecha");
        String motivo = Validar.texto(c.param(2), "motivo", 200);
        if (permisoDAO.existe(aux.getId(), fecha)) {
            throw new NegocioException(aux.getNombreCompleto() + " ya tiene un permiso registrado el " + fecha + ".");
        }
        permisoDAO.insertar(aux.getId(), fecha, motivo, u.getId());
        int justificadas = asistenciaDAO.justificarFaltas(aux.getId(), fecha);
        notificaciones.aUsuario(aux.getId(), "Permiso registrado para el " + fecha,
                "El Encargado registró su permiso del " + fecha + ": " + motivo + ".", null);
        return Resultado.exito("Permiso registrado", "Permiso de " + aux.getNombreCompleto() + " para el " + fecha + " ("
                + motivo + ")." + (justificadas == 0 ? "" : " Se justificaron " + justificadas + " falta(s) ya registrada(s)."));
    }

    // LISASI["219010","2026-10-01","2026-10-31"]  LISASI["*","2026-10-01","2026-10-31"]  LISASI["219010"] (semestre activo)
    public Resultado consultarAsistencia(Comando c, Usuario u) throws NegocioException, SQLException {
        c.exigirEntre(1, 3, "LISASI[\"auxiliar\",\"desde\",\"hasta\"], LISASI[\"*\",\"desde\",\"hasta\"] o LISASI[\"auxiliar\"]");
        Integer auxId = c.param(0).equals("*") ? null : buscar.usuario(c.param(0)).getId();
        LocalDate desde;
        LocalDate hasta;
        if (c.cantidad() == 3) {
            desde = Validar.fecha(c.param(1), "fecha desde");
            hasta = Validar.fecha(c.param(2), "fecha hasta");
            Validar.rangoFechas(desde, hasta);
        } else if (c.cantidad() == 1) {
            Fila sem = buscar.semestreActivo();
            desde = sem.fecha("fecha_inicio");
            hasta = sem.fecha("fecha_fin");
        } else {
            throw new NegocioException("Indique el rango completo: LISASI[\"auxiliar\",\"desde\",\"hasta\"].");
        }
        List<String[]> filas = new ArrayList<>();
        int[] conteo = new int[4];
        for (Fila a : asistenciaDAO.listar(auxId, desde, hasta)) {
            filas.add(filaAsistencia(a));
            switch (a.texto("estado")) {
                case "PUNTUAL" -> conteo[0]++;
                case "ATRASO" -> conteo[1]++;
                case "FALTA" -> conteo[2]++;
                default -> conteo[3]++;
            }
        }
        return Resultado.tabla("Asistencia del " + desde + " al " + hasta, filas.size() + " registro(s): " + conteo[0]
                        + " puntual, " + conteo[1] + " atraso, " + conteo[2] + " falta, " + conteo[3] + " permiso.",
                COLUMNAS_ASISTENCIA, filas);
    }

    /**
     * Tarea automática (se ejecuta en cada revisión del buzón): registra FALTA, o PERMISO si lo tiene,
     * a quien no marcó entrada pasados TOLERANCIA_FALTA_MIN minutos del inicio de su turno de hoy.
     */
    public void marcarFaltas() throws SQLException {
        Fila sem = semestreDAO.obtenerActivo();
        LocalDate hoy = LocalDate.now();
        int dia = hoy.getDayOfWeek().getValue();
        if (sem == null || dia == 7 || hoy.isBefore(sem.fecha("fecha_inicio")) || hoy.isAfter(sem.fecha("fecha_fin"))
                || fechaBloqueadaDAO.bloqueoGeneral(hoy) != null) {
            return;
        }
        LocalTime ahora = LocalTime.now();
        if (ahora.isBefore(LocalTime.of(0, TOLERANCIA_FALTA_MIN))) {
            return;  // ahora - 30 min daría la noche anterior
        }
        LocalTime limite = ahora.minusMinutes(TOLERANCIA_FALTA_MIN);
        for (Fila t : turnoDAO.delDia(sem.entero("id"), dia)) {
            if (!t.hora("hora_inicio").isBefore(limite) || asistenciaDAO.buscar(t.entero("id"), hoy) != null) {
                continue;
            }
            int responsable = buscar.responsableDeTurno(t, hoy);
            boolean conPermiso = permisoDAO.existe(responsable, hoy);
            asistenciaDAO.insertar(t.entero("id"), responsable, hoy, null, conPermiso ? "PERMISO" : "FALTA");
            if (!conPermiso) {
                String texto = "No se registró entrada al turno #" + t.texto("id") + " de hoy "
                        + Formato.franja(t.hora("hora_inicio"), t.hora("hora_fin"))
                        + (t.texto("lab").isEmpty() ? "" : " (" + t.texto("lab") + ")") + "; se registró FALTA.";
                notificaciones.aUsuario(responsable, "Falta registrada", texto, null);
                notificaciones.aEncargados("Falta de auxiliar en turno #" + t.texto("id"), texto, null, 0);
            }
        }
    }

    /** Turnos que el auxiliar debe cubrir hoy: los propios (salvo los cedidos por permuta) y los recibidos. */
    private List<Fila> turnosDeHoy(int auxiliarId, LocalDate hoy) throws NegocioException, SQLException {
        Fila sem = buscar.semestreActivo();
        List<Fila> turnos = new ArrayList<>();
        int dia = hoy.getDayOfWeek().getValue();
        for (Fila t : turnoDAO.listar(sem.entero("id"), auxiliarId)) {
            if (t.entero("dia_semana") == dia && permutaDAO.aprobada(t.entero("id"), hoy) == null) {
                turnos.add(t);
            }
        }
        turnos.addAll(turnoDAO.recibidosPorPermuta(auxiliarId, hoy));
        turnos.sort((a, b) -> a.hora("hora_inicio").compareTo(b.hora("hora_inicio")));
        return turnos;
    }

    private static String describir(List<Fila> turnos) {
        List<String> partes = new ArrayList<>();
        for (Fila t : turnos) {
            partes.add(Formato.franja(t.hora("hora_inicio"), t.hora("hora_fin")));
        }
        return String.join(", ", partes);
    }

    private static String[] filaTurno(Fila t) {
        return new String[]{t.texto("id"), t.texto("auxiliar"), Formato.dia(t.entero("dia_semana")),
                t.texto("hora_inicio"), t.texto("hora_fin"), t.texto("lab").isEmpty() ? "General" : t.texto("lab")};
    }

    private static String[] filaAsistencia(Fila a) {
        return new String[]{a.texto("fecha"), a.texto("auxiliar"), Formato.franja(a.hora("hora_inicio"), a.hora("hora_fin")),
                a.texto("lab"), a.texto("hora_entrada"), a.texto("hora_salida"), a.texto("estado")};
    }
}
