package bo.uagrm.grupo08sa.datos.dao;

import bo.uagrm.grupo08sa.datos.Conexion;
import bo.uagrm.grupo08sa.datos.modelo.Usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class UsuarioDAO {

    private static final String SELECT_BASE =
            "SELECT u.id, u.codigo, u.nombre, u.apellido, u.correo, u.telefono, u.rol_id, "
            + "r.codigo AS rol_codigo, r.nombre AS rol_nombre, u.estado, u.creado_en "
            + "FROM usuario u JOIN rol r ON r.id = u.rol_id ";

    public Usuario buscarPorCorreo(String correo) throws SQLException {
        return buscarUno(SELECT_BASE + "WHERE LOWER(u.correo) = LOWER(?)", correo);
    }

    public Usuario buscarPorCodigo(String codigo) throws SQLException {
        return buscarUno(SELECT_BASE + "WHERE u.codigo = ?", codigo);
    }

    public Usuario buscarPorId(int id) throws SQLException {
        String sql = SELECT_BASE + "WHERE u.id = ?";
        try (Connection cn = Conexion.getConexion(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    /** Acepta el código del usuario o su correo. */
    public Usuario buscarPorCodigoOCorreo(String valor) throws SQLException {
        return valor.contains("@") ? buscarPorCorreo(valor) : buscarPorCodigo(valor);
    }

    public List<Usuario> listarActivosPorRol(String rolCodigo) throws SQLException {
        return listar(rolCodigo, "ACTIVO");
    }

    /** true si otro usuario (distinto de idExcluido) ya usa ese correo. Use idExcluido = 0 al registrar. */
    public boolean existeCorreo(String correo, int idExcluido) throws SQLException {
        String sql = "SELECT 1 FROM usuario WHERE LOWER(correo) = LOWER(?) AND id <> ?";
        try (Connection cn = Conexion.getConexion(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, correo);
            ps.setInt(2, idExcluido);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public int insertar(Usuario u) throws SQLException {
        String sql = "INSERT INTO usuario (codigo, nombre, apellido, correo, telefono, rol_id) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection cn = Conexion.getConexion();
             PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, u.getCodigo());
            ps.setString(2, u.getNombre());
            ps.setString(3, u.getApellido());
            ps.setString(4, u.getCorreo());
            ps.setString(5, u.getTelefono());
            ps.setInt(6, u.getRolId());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public boolean actualizar(Usuario u) throws SQLException {
        String sql = "UPDATE usuario SET nombre = ?, apellido = ?, correo = ?, telefono = ?, rol_id = ? WHERE id = ?";
        try (Connection cn = Conexion.getConexion(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, u.getNombre());
            ps.setString(2, u.getApellido());
            ps.setString(3, u.getCorreo());
            ps.setString(4, u.getTelefono());
            ps.setInt(5, u.getRolId());
            ps.setInt(6, u.getId());
            return ps.executeUpdate() == 1;
        }
    }

    public boolean cambiarEstado(int id, String estado) throws SQLException {
        String sql = "UPDATE usuario SET estado = ? WHERE id = ?";
        try (Connection cn = Conexion.getConexion(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, estado);
            ps.setInt(2, id);
            return ps.executeUpdate() == 1;
        }
    }

    /** rolCodigo y estado son opcionales (null = sin filtro). */
    public List<Usuario> listar(String rolCodigo, String estado) throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT_BASE).append("WHERE 1 = 1 ");
        List<String> parametros = new ArrayList<>();
        if (rolCodigo != null) {
            sql.append("AND r.codigo = ? ");
            parametros.add(rolCodigo);
        }
        if (estado != null) {
            sql.append("AND u.estado = ? ");
            parametros.add(estado);
        }
        sql.append("ORDER BY r.id, u.apellido, u.nombre");

        List<Usuario> lista = new ArrayList<>();
        try (Connection cn = Conexion.getConexion(); PreparedStatement ps = cn.prepareStatement(sql.toString())) {
            for (int i = 0; i < parametros.size(); i++) {
                ps.setString(i + 1, parametros.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        }
        return lista;
    }

    private Usuario buscarUno(String sql, String valor) throws SQLException {
        try (Connection cn = Conexion.getConexion(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, valor);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    private Usuario mapear(ResultSet rs) throws SQLException {
        Usuario u = new Usuario();
        u.setId(rs.getInt("id"));
        u.setCodigo(rs.getString("codigo"));
        u.setNombre(rs.getString("nombre"));
        u.setApellido(rs.getString("apellido"));
        u.setCorreo(rs.getString("correo"));
        u.setTelefono(rs.getString("telefono"));
        u.setRolId(rs.getInt("rol_id"));
        u.setRolCodigo(rs.getString("rol_codigo"));
        u.setRolNombre(rs.getString("rol_nombre"));
        u.setEstado(rs.getString("estado"));
        u.setCreadoEn(rs.getTimestamp("creado_en"));
        return u;
    }
}
