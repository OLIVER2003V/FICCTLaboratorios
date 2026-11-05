package bo.uagrm.grupo08sa.datos.dao;

import bo.uagrm.grupo08sa.datos.Fila;

import java.sql.SQLException;
import java.util.List;

public class EquipoDAO extends BaseDAO {

    private static final String SELECT_BASE =
            "SELECT e.*, l.codigo AS lab FROM equipo e JOIN laboratorio l ON l.id = e.laboratorio_id ";

    public Fila buscarPorCodigo(String codigo) throws SQLException {
        return consultarUno(SELECT_BASE + "WHERE UPPER(e.codigo) = UPPER(?) OR UPPER(e.activo) = UPPER(?)", codigo, codigo);
    }

    public int insertar(String codigo, int laboratorioId, String tipo, String descripcion) throws SQLException {
        return insertar("INSERT INTO equipo (activo, codigo, laboratorio_id, tipo, descripcion) VALUES (?, ?, ?, ?, ?)",
                codigo, codigo, laboratorioId, tipo, descripcion);
    }

    public void actualizar(int id, int laboratorioId, String tipo, String descripcion, String estado)
            throws SQLException {
        ejecutar("UPDATE equipo SET laboratorio_id = ?, tipo = ?, descripcion = ?, estado = ? WHERE id = ?",
                laboratorioId, tipo, descripcion, estado, id);
    }

    public void cambiarEstado(int id, String estado) throws SQLException {
        ejecutar("UPDATE equipo SET estado = ? WHERE id = ?", estado, id);
    }

    public List<Fila> listar(Integer laboratorioId, String estado) throws SQLException {
        return consultar(SELECT_BASE + "WHERE (CAST(? AS INTEGER) IS NULL OR e.laboratorio_id = ?) "
                + "AND (CAST(? AS VARCHAR) IS NULL OR e.estado = ?) ORDER BY l.codigo, e.activo, e.codigo",
                laboratorioId, laboratorioId, estado, estado);
    }

    /** Solo accesible para Encargados (LISRED) */
    public List<Fila> listarRed(Integer laboratorioId) throws SQLException {
        String sql = "SELECT e.activo, e.codigo, l.codigo AS lab, r.ip, r.mascara, r.gateway, r.proxy, r.dns1, r.grupo_red "
                + "FROM equipo_red r JOIN equipo e ON e.id = r.equipo_id JOIN laboratorio l ON l.id = e.laboratorio_id "
                + "WHERE (CAST(? AS INTEGER) IS NULL OR e.laboratorio_id = ?) "
                + "ORDER BY l.codigo, CASE WHEN e.codigo LIKE '%PC-DOC%' THEN 0 ELSE 1 END, e.codigo";
        return consultar(sql, laboratorioId, laboratorioId);
    }
}
