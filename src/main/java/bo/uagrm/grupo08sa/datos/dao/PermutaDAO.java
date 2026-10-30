package bo.uagrm.grupo08sa.datos.dao;

import bo.uagrm.grupo08sa.datos.Fila;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class PermutaDAO extends BaseDAO {

    private static final String SELECT_BASE =
            "SELECT p.*, t.dia_semana, t.hora_inicio, t.hora_fin, l.codigo AS lab, "
            + "s.codigo AS solicitante_codigo, s.nombre || ' ' || s.apellido AS solicitante, "
            + "r.codigo AS reemplazante_codigo, r.nombre || ' ' || r.apellido AS reemplazante "
            + "FROM permuta p JOIN turno t ON t.id = p.turno_id LEFT JOIN laboratorio l ON l.id = t.laboratorio_id "
            + "JOIN usuario s ON s.id = p.solicitante_id JOIN usuario r ON r.id = p.reemplazante_id ";

    public int insertar(int turnoId, int solicitanteId, int reemplazanteId, LocalDate fecha) throws SQLException {
        return insertar("INSERT INTO permuta (turno_id, solicitante_id, reemplazante_id, fecha) VALUES (?, ?, ?, ?)",
                turnoId, solicitanteId, reemplazanteId, fecha);
    }

    public Fila buscarPorId(int id) throws SQLException {
        return consultarUno(SELECT_BASE + "WHERE p.id = ?", id);
    }

    /** Permuta pendiente, aceptada o aprobada para ese turno y fecha. */
    public Fila activa(int turnoId, LocalDate fecha) throws SQLException {
        return consultarUno(SELECT_BASE + "WHERE p.turno_id = ? AND p.fecha = ? "
                + "AND p.estado IN ('PENDIENTE','ACEPTADA','APROBADA') LIMIT 1", turnoId, fecha);
    }

    public Fila aprobada(int turnoId, LocalDate fecha) throws SQLException {
        return consultarUno(SELECT_BASE + "WHERE p.turno_id = ? AND p.fecha = ? AND p.estado = 'APROBADA' LIMIT 1",
                turnoId, fecha);
    }

    public void cambiarEstado(int id, String estado, Integer aprobadoPor) throws SQLException {
        ejecutar("UPDATE permuta SET estado = ?, aprobado_por = COALESCE(CAST(? AS INTEGER), aprobado_por) WHERE id = ?",
                estado, aprobadoPor, id);
    }

    /** usuarioId (como solicitante o reemplazante) y estado opcionales. */
    public List<Fila> listar(Integer usuarioId, String estado) throws SQLException {
        return consultar(SELECT_BASE + "WHERE (CAST(? AS INTEGER) IS NULL OR p.solicitante_id = ? OR p.reemplazante_id = ?) "
                + "AND (CAST(? AS VARCHAR) IS NULL OR p.estado = ?) ORDER BY p.fecha DESC, p.id DESC",
                usuarioId, usuarioId, usuarioId, estado, estado);
    }
}
