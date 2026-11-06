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
        return insertar(laboratorioId, descripcion, fecha, registradoPor, "2-2026", null);
    }

    public int insertar(int laboratorioId, String descripcion, LocalDate fecha, int registradoPor, String gestion) throws SQLException {
        return insertar(laboratorioId, descripcion, fecha, registradoPor, gestion, null);
    }

    public int insertar(int laboratorioId, String descripcion, LocalDate fecha, int registradoPor, String gestion, String auxEncontro) throws SQLException {
        return insertar("INSERT INTO objeto_encontrado (laboratorio_id, descripcion, fecha_hallazgo, registrado_por, gestion, aux_encontro) "
                + "VALUES (?, ?, ?, ?, ?, ?)", laboratorioId, descripcion, fecha, registradoPor, gestion, auxEncontro);
    }

    public Fila buscarPorId(int id) throws SQLException {
        return consultarUno(SELECT_BASE + "WHERE o.id = ?", id);
    }

    public void entregar(int id, String entregadoA, String ciReceptor) throws SQLException {
        entregar(id, entregadoA, ciReceptor, null);
    }

    public void entregar(int id, String entregadoA, String ciReceptor, String auxDevolvio) throws SQLException {
        ejecutar("UPDATE objeto_encontrado SET estado = 'ENTREGADO', entregado_a = ?, ci_receptor = ?, "
                + "fecha_entrega = CURRENT_TIMESTAMP, aux_devolvio = ? WHERE id = ?", entregadoA, ciReceptor, auxDevolvio, id);
    }

    public List<Fila> listar(String estado) throws SQLException {
        return consultar(SELECT_BASE + "WHERE (CAST(? AS VARCHAR) IS NULL OR o.estado = ?) "
                + "ORDER BY o.id ASC", estado, estado);
    }
}
