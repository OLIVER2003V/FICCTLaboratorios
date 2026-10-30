package bo.uagrm.grupo08sa.datos.dao;

import bo.uagrm.grupo08sa.datos.Fila;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class ReservaDAO extends BaseDAO {

    private static final String SELECT_BASE =
            "SELECT r.*, l.codigo AS lab, u.codigo AS solicitante_codigo, "
            + "u.nombre || ' ' || u.apellido AS solicitante FROM reserva r "
            + "JOIN laboratorio l ON l.id = r.laboratorio_id JOIN usuario u ON u.id = r.solicitante_id ";

    public int insertar(int laboratorioId, int solicitanteId, LocalDate fecha, LocalTime inicio, LocalTime fin,
                        String tipo, String motivo) throws SQLException {
        return insertar("INSERT INTO reserva (laboratorio_id, solicitante_id, fecha, hora_inicio, hora_fin, tipo, motivo) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)", laboratorioId, solicitanteId, fecha, inicio, fin, tipo, motivo);
    }

    public Fila buscarPorId(int id) throws SQLException {
        return consultarUno(SELECT_BASE + "WHERE r.id = ?", id);
    }

    /** Reserva aprobada que se superpone. excluirId = 0 si no aplica. */
    public Fila choqueAprobada(int laboratorioId, LocalDate fecha, LocalTime inicio, LocalTime fin, int excluirId)
            throws SQLException {
        return consultarUno(SELECT_BASE + "WHERE r.laboratorio_id = ? AND r.fecha = ? AND r.estado = 'APROBADA' "
                + "AND r.hora_inicio < ? AND r.hora_fin > ? AND r.id <> ? LIMIT 1",
                laboratorioId, fecha, fin, inicio, excluirId);
    }

    public int pendientesSuperpuestas(int laboratorioId, LocalDate fecha, LocalTime inicio, LocalTime fin, int excluirId)
            throws SQLException {
        return contar("SELECT COUNT(*) FROM reserva WHERE laboratorio_id = ? AND fecha = ? AND estado = 'PENDIENTE' "
                + "AND hora_inicio < ? AND hora_fin > ? AND id <> ?", laboratorioId, fecha, fin, inicio, excluirId);
    }

    public int aprobadasEnFecha(LocalDate fecha, Integer laboratorioId) throws SQLException {
        return contar("SELECT COUNT(*) FROM reserva WHERE fecha = ? AND estado = 'APROBADA' "
                + "AND (CAST(? AS INTEGER) IS NULL OR laboratorio_id = ?)", fecha, laboratorioId, laboratorioId);
    }

    public void resolver(int id, String estado, int resueltoPor, String observacion) throws SQLException {
        ejecutar("UPDATE reserva SET estado = ?, resuelto_por = ?, observacion = ? WHERE id = ?",
                estado, resueltoPor, observacion, id);
    }

    public void cambiarEstado(int id, String estado) throws SQLException {
        ejecutar("UPDATE reserva SET estado = ? WHERE id = ?", estado, id);
    }

    /** solicitanteId y estado opcionales (null = todos). */
    public List<Fila> listar(Integer solicitanteId, String estado) throws SQLException {
        return consultar(SELECT_BASE + "WHERE (CAST(? AS INTEGER) IS NULL OR r.solicitante_id = ?) "
                + "AND (CAST(? AS VARCHAR) IS NULL OR r.estado = ?) ORDER BY r.fecha DESC, r.hora_inicio",
                solicitanteId, solicitanteId, estado, estado);
    }
}
