package bo.uagrm.grupo08sa.datos.dao;

import bo.uagrm.grupo08sa.datos.Fila;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class TurnoDAO extends BaseDAO {

    private static final String SELECT_BASE =
            "SELECT t.*, u.codigo AS auxiliar_codigo, u.nombre || ' ' || u.apellido AS auxiliar, l.codigo AS lab "
            + "FROM turno t JOIN usuario u ON u.id = t.auxiliar_id LEFT JOIN laboratorio l ON l.id = t.laboratorio_id ";

    public int insertar(int semestreId, int auxiliarId, Integer laboratorioId, int dia, LocalTime inicio, LocalTime fin)
            throws SQLException {
        return insertar("INSERT INTO turno (semestre_id, auxiliar_id, laboratorio_id, dia_semana, hora_inicio, hora_fin) "
                + "VALUES (?, ?, ?, ?, ?, ?)", semestreId, auxiliarId, laboratorioId, dia, inicio, fin);
    }

    public Fila buscarPorId(int id) throws SQLException {
        return consultarUno(SELECT_BASE + "WHERE t.id = ?", id);
    }

    /** Turno propio del auxiliar que se superpone en ese día. */
    public Fila choqueAuxiliar(int semestreId, int auxiliarId, int dia, LocalTime inicio, LocalTime fin)
            throws SQLException {
        return consultarUno(SELECT_BASE + "WHERE t.semestre_id = ? AND t.auxiliar_id = ? AND t.dia_semana = ? "
                + "AND t.hora_inicio < ? AND t.hora_fin > ? LIMIT 1", semestreId, auxiliarId, dia, fin, inicio);
    }

    public boolean tieneTurnos(int semestreId, int auxiliarId) throws SQLException {
        return existe("SELECT 1 FROM turno WHERE semestre_id = ? AND auxiliar_id = ?", semestreId, auxiliarId);
    }

    public List<Fila> listar(int semestreId, Integer auxiliarId) throws SQLException {
        return consultar(SELECT_BASE + "WHERE t.semestre_id = ? AND (CAST(? AS INTEGER) IS NULL OR t.auxiliar_id = ?) "
                + "ORDER BY t.dia_semana, t.hora_inicio, u.apellido", semestreId, auxiliarId, auxiliarId);
    }

    public List<Fila> delDia(int semestreId, int dia) throws SQLException {
        return consultar(SELECT_BASE + "WHERE t.semestre_id = ? AND t.dia_semana = ? ORDER BY t.hora_inicio",
                semestreId, dia);
    }

    /** Turnos de otros que el auxiliar cubre esa fecha por una permuta aprobada. */
    public List<Fila> recibidosPorPermuta(int auxiliarId, LocalDate fecha) throws SQLException {
        return consultar(SELECT_BASE + "JOIN permuta p ON p.turno_id = t.id "
                + "WHERE p.reemplazante_id = ? AND p.fecha = ? AND p.estado = 'APROBADA' ORDER BY t.hora_inicio",
                auxiliarId, fecha);
    }
}
