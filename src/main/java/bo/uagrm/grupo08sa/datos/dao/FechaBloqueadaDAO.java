package bo.uagrm.grupo08sa.datos.dao;

import bo.uagrm.grupo08sa.datos.Fila;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class FechaBloqueadaDAO extends BaseDAO {

    private static final String SELECT_BASE =
            "SELECT fb.*, l.codigo AS lab FROM fecha_bloqueada fb LEFT JOIN laboratorio l ON l.id = fb.laboratorio_id ";

    public int insertar(int semestreId, LocalDate fecha, String tipo, String motivo, Integer laboratorioId)
            throws SQLException {
        return insertar("INSERT INTO fecha_bloqueada (semestre_id, fecha, tipo, motivo, laboratorio_id) "
                + "VALUES (?, ?, ?, ?, ?)", semestreId, fecha, tipo, motivo, laboratorioId);
    }

    /** Ya existe un bloqueo para esa fecha y ese laboratorio (o para todos, si laboratorioId es null). */
    public boolean duplicada(LocalDate fecha, Integer laboratorioId) throws SQLException {
        return existe("SELECT 1 FROM fecha_bloqueada WHERE fecha = ? "
                + "AND laboratorio_id IS NOT DISTINCT FROM CAST(? AS INTEGER)", fecha, laboratorioId);
    }

    /** Bloqueo que afecta al laboratorio en esa fecha (general o específico). */
    public Fila bloqueo(LocalDate fecha, int laboratorioId) throws SQLException {
        return consultarUno(SELECT_BASE + "WHERE fb.fecha = ? AND (fb.laboratorio_id IS NULL OR fb.laboratorio_id = ?) "
                + "ORDER BY fb.laboratorio_id NULLS FIRST LIMIT 1", fecha, laboratorioId);
    }

    /** Bloqueo que afecta a todos los laboratorios en esa fecha. */
    public Fila bloqueoGeneral(LocalDate fecha) throws SQLException {
        return consultarUno(SELECT_BASE + "WHERE fb.fecha = ? AND fb.laboratorio_id IS NULL LIMIT 1", fecha);
    }

    public List<Fila> listar(int semestreId) throws SQLException {
        return consultar(SELECT_BASE + "WHERE fb.semestre_id = ? ORDER BY fb.fecha, l.codigo NULLS FIRST", semestreId);
    }
}
