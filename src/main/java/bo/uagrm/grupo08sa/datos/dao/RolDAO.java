package bo.uagrm.grupo08sa.datos.dao;

import bo.uagrm.grupo08sa.datos.Conexion;
import bo.uagrm.grupo08sa.datos.modelo.Rol;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class RolDAO {

    public Rol buscarPorCodigo(String codigo) throws SQLException {
        String sql = "SELECT id, codigo, nombre, descripcion FROM rol WHERE codigo = ?";
        try (Connection cn = Conexion.getConexion(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, codigo);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new Rol(rs.getInt("id"), rs.getString("codigo"),
                        rs.getString("nombre"), rs.getString("descripcion"));
            }
        }
    }
}
