package bo.uagrm.grupo08sa.datos.dao;

import bo.uagrm.grupo08sa.datos.Fila;

import java.sql.SQLException;
import java.util.List;

public class MensajeDAO extends BaseDAO {

    public int insertar(int remitenteId, int destinatarioId, String contenido) throws SQLException {
        return insertar("INSERT INTO mensaje (remitente_id, destinatario_id, contenido) VALUES (?, ?, ?)",
                remitenteId, destinatarioId, contenido);
    }

    /** Últimos 50 mensajes recibidos, los más recientes primero. */
    public List<Fila> recibidos(int destinatarioId, boolean soloNoLeidos) throws SQLException {
        return consultar("SELECT m.*, u.codigo AS remitente_codigo, u.nombre || ' ' || u.apellido AS remitente "
                + "FROM mensaje m JOIN usuario u ON u.id = m.remitente_id "
                + "WHERE m.destinatario_id = ? AND (CAST(? AS BOOLEAN) = FALSE OR m.leido = FALSE) "
                + "ORDER BY m.enviado_en DESC LIMIT 50", destinatarioId, soloNoLeidos);
    }

    public void marcarLeidos(int destinatarioId) throws SQLException {
        ejecutar("UPDATE mensaje SET leido = TRUE WHERE destinatario_id = ? AND leido = FALSE", destinatarioId);
    }
}
