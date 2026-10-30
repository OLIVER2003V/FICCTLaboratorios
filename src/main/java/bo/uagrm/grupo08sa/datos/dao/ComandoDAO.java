package bo.uagrm.grupo08sa.datos.dao;

import bo.uagrm.grupo08sa.datos.Conexion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ComandoDAO {

    public boolean existe(String nombre) throws SQLException {
        String sql = "SELECT 1 FROM comando WHERE nombre = ?";
        try (Connection cn = Conexion.getConexion(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean rolPuedeEjecutar(int rolId, String nombreComando) throws SQLException {
        String sql = "SELECT 1 FROM rol_comando rc JOIN comando c ON c.id = rc.comando_id "
                + "WHERE rc.rol_id = ? AND c.nombre = ?";
        try (Connection cn = Conexion.getConexion(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, rolId);
            ps.setString(2, nombreComando);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Da permiso al rol. Devuelve false si ya lo tenía. */
    public boolean asignar(int rolId, String nombreComando) throws SQLException {
        String sql = "INSERT INTO rol_comando (rol_id, comando_id) SELECT ?, id FROM comando WHERE nombre = ? "
                + "ON CONFLICT DO NOTHING";
        try (Connection cn = Conexion.getConexion(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, rolId);
            ps.setString(2, nombreComando);
            return ps.executeUpdate() == 1;
        }
    }

    /** Quita el permiso al rol. Devuelve false si no lo tenía. */
    public boolean quitar(int rolId, String nombreComando) throws SQLException {
        String sql = "DELETE FROM rol_comando WHERE rol_id = ? AND comando_id = (SELECT id FROM comando WHERE nombre = ?)";
        try (Connection cn = Conexion.getConexion(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, rolId);
            ps.setString(2, nombreComando);
            return ps.executeUpdate() == 1;
        }
    }

    /** Devuelve filas {nombre, descripcion} de los comandos que el rol puede ejecutar. */
    public List<String[]> listarPorRol(int rolId) throws SQLException {
        String sql = "SELECT c.nombre, c.descripcion FROM rol_comando rc JOIN comando c ON c.id = rc.comando_id "
                + "WHERE rc.rol_id = ? ORDER BY c.id";
        List<String[]> filas = new ArrayList<>();
        try (Connection cn = Conexion.getConexion(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, rolId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    filas.add(new String[]{rs.getString("nombre"), rs.getString("descripcion")});
                }
            }
        }
        return filas;
    }
}
