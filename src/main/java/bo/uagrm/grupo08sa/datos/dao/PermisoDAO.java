package bo.uagrm.grupo08sa.datos.dao;

import java.sql.SQLException;
import java.time.LocalDate;

public class PermisoDAO extends BaseDAO {

    public int insertar(int auxiliarId, LocalDate fecha, String motivo, int registradoPor) throws SQLException {
        return insertar("INSERT INTO permiso (auxiliar_id, fecha, motivo, registrado_por) VALUES (?, ?, ?, ?)",
                auxiliarId, fecha, motivo, registradoPor);
    }

    public boolean existe(int auxiliarId, LocalDate fecha) throws SQLException {
        return existe("SELECT 1 FROM permiso WHERE auxiliar_id = ? AND fecha = ?", auxiliarId, fecha);
    }
}
