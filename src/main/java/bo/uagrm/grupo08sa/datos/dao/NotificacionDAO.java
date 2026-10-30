package bo.uagrm.grupo08sa.datos.dao;

import java.sql.SQLException;

public class NotificacionDAO extends BaseDAO {

    public int insertar(int usuarioId, String asunto, String cuerpo, Integer reservaId) throws SQLException {
        return insertar("INSERT INTO notificacion (usuario_id, asunto, cuerpo, reserva_id) VALUES (?, ?, ?, ?)",
                usuarioId, asunto.length() > 150 ? asunto.substring(0, 150) : asunto, cuerpo, reservaId);
    }
}
