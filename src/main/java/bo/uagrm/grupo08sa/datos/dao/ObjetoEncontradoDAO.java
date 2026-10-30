package bo.uagrm.grupo08sa.datos.dao;

import bo.uagrm.grupo08sa.datos.Fila;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class ObjetoEncontradoDAO extends BaseDAO {

    private static final String SELECT_BASE =
            "SELECT o.*, l.codigo AS lab, u.nombre || ' ' || u.apellido AS registrado FROM objeto_encontrado o "
            + "JOIN laboratorio l ON l.id = o.laboratorio_id JOIN usuario u ON u.id = o.registrado_por ";

    public int insertar(int laboratorioId, String descripcion, LocalDate fecha, int registradoPor) throws SQLException {
        return insertar("INSERT INTO objeto_encontrado (laboratorio_id, descripcion, fecha_hallazgo, registrado_por) "
                + "VALUES (?, ?, ?, ?)", laboratorioId, descripcion, fecha, registradoPor);
    }

    public Fila buscarPorId(int id) throws SQLException {
        return consultarUno(SELECT_BASE + "WHERE o.id = ?", id);
    }

    public void entregar(int id, String entregadoA, String ciReceptor) throws SQLException {
        ejecutar("UPDATE objeto_encontrado SET estado = 'ENTREGADO', entregado_a = ?, ci_receptor = ?, "
                + "fecha_entrega = CURRENT_TIMESTAMP WHERE id = ?", entregadoA, ciReceptor, id);
    }

    public List<Fila> listar(String estado) throws SQLException {
        return consultar(SELECT_BASE + "WHERE (CAST(? AS VARCHAR) IS NULL OR o.estado = ?) "
                + "ORDER BY o.fecha_hallazgo DESC, o.id DESC", estado, estado);
    }
}
