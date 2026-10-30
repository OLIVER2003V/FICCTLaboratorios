package bo.uagrm.grupo08sa.datos.dao;

import bo.uagrm.grupo08sa.datos.Conexion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class BitacoraDAO {

    public void insertar(String remitente, String comando, String asunto, String resultado, String detalle)
            throws SQLException {
        String sql = "INSERT INTO bitacora (remitente, comando, asunto, resultado, detalle) VALUES (?, ?, ?, ?, ?)";
        try (Connection cn = Conexion.getConexion(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, recortar(remitente, 100));
            ps.setString(2, recortar(comando, 10));
            ps.setString(3, recortar(asunto, 300));
            ps.setString(4, resultado);
            ps.setString(5, recortar(detalle, 300));
            ps.executeUpdate();
        }
    }

    private static String recortar(String texto, int max) {
        if (texto == null) {
            return null;
        }
        return texto.length() <= max ? texto : texto.substring(0, max);
    }
}
