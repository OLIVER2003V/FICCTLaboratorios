package bo.uagrm.grupo08sa.negocio;

import bo.uagrm.grupo08sa.datos.Fila;
import bo.uagrm.grupo08sa.datos.dao.FechaBloqueadaDAO;
import bo.uagrm.grupo08sa.datos.dao.HorarioDAO;
import bo.uagrm.grupo08sa.datos.dao.LaboratorioDAO;
import bo.uagrm.grupo08sa.datos.dao.ReservaDAO;
import bo.uagrm.grupo08sa.datos.dao.SemestreDAO;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Cruza el horario base, las reservas aprobadas, las fechas bloqueadas y el estado del laboratorio (CU3 y CU4).
 * Dos asignaciones chocan si ocupan el mismo laboratorio y cada una empieza antes de que termine la otra.
 */
public class Disponibilidad {

    /** tipo: ESTADO, BLOQUEO, CLASE o RESERVA. */
    public record Ocupacion(String tipo, String detalle) {
    }

    private final FechaBloqueadaDAO fechaBloqueadaDAO = new FechaBloqueadaDAO();
    private final HorarioDAO horarioDAO = new HorarioDAO();
    private final ReservaDAO reservaDAO = new ReservaDAO();
    private final SemestreDAO semestreDAO = new SemestreDAO();
    private final LaboratorioDAO laboratorioDAO = new LaboratorioDAO();

    /** null si el laboratorio está libre en esa fecha y franja. excluirReservaId = 0 si no aplica. */
    public Ocupacion ocupacion(Fila lab, LocalDate fecha, LocalTime inicio, LocalTime fin, int excluirReservaId)
            throws SQLException {
        int labId = lab.entero("id");
        if (!"DISPONIBLE".equals(lab.texto("estado"))) {
            return new Ocupacion("ESTADO", "Laboratorio en estado " + lab.texto("estado"));
        }
        Fila bloqueo = fechaBloqueadaDAO.bloqueo(fecha, labId);
        if (bloqueo != null) {
            return new Ocupacion("BLOQUEO", "Fecha bloqueada: " + bloqueo.texto("tipo")
                    + (bloqueo.texto("motivo").isEmpty() ? "" : " — " + bloqueo.texto("motivo")));
        }
        Fila sem = semestreDAO.obtenerActivo();
        int dia = fecha.getDayOfWeek().getValue();
        if (sem != null && dia <= 6 && !fecha.isBefore(sem.fecha("fecha_inicio")) && !fecha.isAfter(sem.fecha("fecha_fin"))) {
            Fila clase = horarioDAO.choque(sem.entero("id"), labId, dia, inicio, fin, 0);
            if (clase != null) {
                return new Ocupacion("CLASE", "Clase " + clase.texto("sigla") + "-" + clase.texto("grupo") + " ("
                        + clase.texto("docente") + ") " + Formato.franja(clase.hora("hora_inicio"), clase.hora("hora_fin")));
            }
        }
        Fila reserva = reservaDAO.choqueAprobada(labId, fecha, inicio, fin, excluirReservaId);
        if (reserva != null) {
            return new Ocupacion("RESERVA", "Reserva #" + reserva.texto("id") + " " + reserva.texto("tipo") + " de "
                    + reserva.texto("solicitante") + " " + Formato.franja(reserva.hora("hora_inicio"), reserva.hora("hora_fin")));
        }
        return null;
    }

    /** Laboratorios disponibles en esa fecha y franja (para proponer alternativas ante un conflicto). */
    public List<Fila> libres(LocalDate fecha, LocalTime inicio, LocalTime fin) throws SQLException {
        List<Fila> libres = new ArrayList<>();
        for (Fila lab : laboratorioDAO.listar("DISPONIBLE")) {
            if (ocupacion(lab, fecha, inicio, fin, 0) == null) {
                libres.add(lab);
            }
        }
        return libres;
    }

    public static String codigos(List<Fila> labs) {
        List<String> codigos = new ArrayList<>();
        for (Fila l : labs) {
            codigos.add(l.texto("codigo") + " (" + l.texto("capacidad") + " pc)");
        }
        return codigos.isEmpty() ? "ninguno" : String.join(", ", codigos);
    }
}
