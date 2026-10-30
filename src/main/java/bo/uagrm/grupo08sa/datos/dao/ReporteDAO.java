package bo.uagrm.grupo08sa.datos.dao;

import bo.uagrm.grupo08sa.datos.Fila;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * Consultas de solo lectura para los reportes (CU8).
 */
public class ReporteDAO extends BaseDAO {

    public List<Fila> laboratorios() throws SQLException {
        return consultar("SELECT id, codigo, nombre, estado FROM laboratorio ORDER BY codigo");
    }

    /** Clases de los semestres que se superponen con el rango, con las fechas de su semestre. */
    public List<Fila> clasesEnRango(LocalDate desde, LocalDate hasta) throws SQLException {
        return consultar("SELECT h.laboratorio_id, h.dia_semana, h.hora_inicio, h.hora_fin, "
                + "s.fecha_inicio, s.fecha_fin FROM horario_clase h JOIN semestre s ON s.id = h.semestre_id "
                + "WHERE s.fecha_inicio <= ? AND s.fecha_fin >= ?", hasta, desde);
    }

    public List<Fila> enRangoBloqueos(LocalDate desde, LocalDate hasta) throws SQLException {
        return consultar("SELECT fecha, laboratorio_id FROM fecha_bloqueada WHERE fecha BETWEEN ? AND ?", desde, hasta);
    }

    public List<Fila> reservasEnRango(LocalDate desde, LocalDate hasta) throws SQLException {
        return consultar("SELECT r.laboratorio_id, l.codigo AS lab, r.estado, r.tipo, r.fecha, r.hora_inicio, r.hora_fin "
                + "FROM reserva r JOIN laboratorio l ON l.id = r.laboratorio_id WHERE r.fecha BETWEEN ? AND ?",
                desde, hasta);
    }

    public List<Fila> incidenciasEnRango(LocalDate desde, LocalDate hasta) throws SQLException {
        return consultar("SELECT i.estado, l.codigo AS lab, e.tipo, i.fecha_reporte, i.fecha_solucion "
                + "FROM incidencia i JOIN equipo e ON e.id = i.equipo_id JOIN laboratorio l ON l.id = e.laboratorio_id "
                + "WHERE CAST(i.fecha_reporte AS DATE) BETWEEN ? AND ?", desde, hasta);
    }

    public List<Fila> objetosEnRango(LocalDate desde, LocalDate hasta) throws SQLException {
        return consultar("SELECT o.estado, l.codigo AS lab FROM objeto_encontrado o "
                + "JOIN laboratorio l ON l.id = o.laboratorio_id WHERE o.fecha_hallazgo BETWEEN ? AND ?", desde, hasta);
    }

    public List<Fila> asistenciaEnRango(LocalDate desde, LocalDate hasta) throws SQLException {
        return consultar("SELECT a.estado, a.hora_entrada, a.hora_salida, u.codigo AS auxiliar_codigo, "
                + "u.nombre || ' ' || u.apellido AS auxiliar FROM asistencia a JOIN usuario u ON u.id = a.auxiliar_id "
                + "WHERE a.fecha BETWEEN ? AND ?", desde, hasta);
    }

    public List<Fila> permutasEnRango(LocalDate desde, LocalDate hasta) throws SQLException {
        return consultar("SELECT estado FROM permuta WHERE fecha BETWEEN ? AND ?", desde, hasta);
    }

    public List<Fila> equipos() throws SQLException {
        return consultar("SELECT estado FROM equipo");
    }

    public int clasesDelSemestre(int semestreId) throws SQLException {
        return contar("SELECT COUNT(*) FROM horario_clase WHERE semestre_id = ?", semestreId);
    }

    public int docentesConClase(int semestreId) throws SQLException {
        return contar("SELECT COUNT(DISTINCT docente_id) FROM horario_clase WHERE semestre_id = ?", semestreId);
    }

    public int materiasDelSemestre(int semestreId) throws SQLException {
        return contar("SELECT COUNT(DISTINCT materia_id) FROM horario_clase WHERE semestre_id = ?", semestreId);
    }

    public int fechasBloqueadas(int semestreId) throws SQLException {
        return contar("SELECT COUNT(*) FROM fecha_bloqueada WHERE semestre_id = ?", semestreId);
    }

    public int mensajesEnRango(LocalDate desde, LocalDate hasta) throws SQLException {
        return contar("SELECT COUNT(*) FROM mensaje WHERE CAST(enviado_en AS DATE) BETWEEN ? AND ?", desde, hasta);
    }
}
