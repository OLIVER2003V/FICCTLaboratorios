package bo.uagrm.grupo08sa.datos.dao;

import bo.uagrm.grupo08sa.datos.Fila;

import java.sql.SQLException;
import java.util.List;

public class IncidenciaDAO extends BaseDAO {

    private static final String SELECT_BASE =
            "SELECT i.*, e.codigo AS equipo, e.estado AS equipo_estado, l.codigo AS lab, "
            + "u.nombre || ' ' || u.apellido AS reportado FROM incidencia i "
            + "JOIN equipo e ON e.id = i.equipo_id JOIN laboratorio l ON l.id = e.laboratorio_id "
            + "JOIN usuario u ON u.id = i.reportado_por ";

    public int insertar(int equipoId, int reportadoPor, String descripcion) throws SQLException {
        return insertar("INSERT INTO incidencia (equipo_id, reportado_por, descripcion) VALUES (?, ?, ?)",
                equipoId, reportadoPor, descripcion);
    }

    public Fila buscarPorId(int id) throws SQLException {
        return consultarUno(SELECT_BASE + "WHERE i.id = ?", id);
    }

    /** Al pasar a RESUELTA se guarda la solución y la fecha de solución. */
    public void actualizar(int id, String estado, String solucion) throws SQLException {
        ejecutar("UPDATE incidencia SET estado = ?, solucion = COALESCE(CAST(? AS VARCHAR), solucion), "
                + "fecha_solucion = CASE WHEN CAST(? AS VARCHAR) = 'RESUELTA' THEN CURRENT_TIMESTAMP ELSE fecha_solucion END "
                + "WHERE id = ?", estado, solucion, estado, id);
    }

    public int abiertas(int equipoId) throws SQLException {
        return contar("SELECT COUNT(*) FROM incidencia WHERE equipo_id = ? AND estado <> 'RESUELTA'", equipoId);
    }

    public List<Fila> listar(String estado, Integer laboratorioId) throws SQLException {
        return consultar(SELECT_BASE + "WHERE (CAST(? AS VARCHAR) IS NULL OR i.estado = ?) "
                + "AND (CAST(? AS INTEGER) IS NULL OR e.laboratorio_id = ?) ORDER BY i.fecha_reporte DESC",
                estado, estado, laboratorioId, laboratorioId);
    }
}
