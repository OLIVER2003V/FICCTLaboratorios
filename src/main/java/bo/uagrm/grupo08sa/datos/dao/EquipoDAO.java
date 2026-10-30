package bo.uagrm.grupo08sa.datos.dao;

import bo.uagrm.grupo08sa.datos.Fila;

import java.sql.SQLException;
import java.util.List;

public class EquipoDAO extends BaseDAO {

    private static final String SELECT_BASE =
            "SELECT e.*, l.codigo AS lab FROM equipo e JOIN laboratorio l ON l.id = e.laboratorio_id ";

    public Fila buscarPorCodigo(String codigo) throws SQLException {
        return consultarUno(SELECT_BASE + "WHERE UPPER(e.codigo) = UPPER(?)", codigo);
    }

    public int insertar(String codigo, int laboratorioId, String tipo, String descripcion) throws SQLException {
        return insertar("INSERT INTO equipo (codigo, laboratorio_id, tipo, descripcion) VALUES (?, ?, ?, ?)",
                codigo, laboratorioId, tipo, descripcion);
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
                + "AND (CAST(? AS VARCHAR) IS NULL OR e.estado = ?) ORDER BY l.codigo, e.codigo",
                laboratorioId, laboratorioId, estado, estado);
    }
}
