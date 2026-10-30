package bo.uagrm.grupo08sa.datos.dao;

import bo.uagrm.grupo08sa.datos.Conexion;
import bo.uagrm.grupo08sa.datos.Fila;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Operaciones comunes de los DAO. Todos los valores se pasan como parámetros de PreparedStatement
 * (nunca concatenados al SQL), lo que evita la inyección SQL.
 *
 * Para filtros opcionales se usa el patrón (CAST(? AS tipo) IS NULL OR columna = ?), pasando el valor dos veces.
 */
public abstract class BaseDAO {

    protected List<Fila> consultar(String sql, Object... parametros) throws SQLException {
        try (Connection cn = Conexion.getConexion(); PreparedStatement ps = cn.prepareStatement(sql)) {
            asignar(ps, parametros);
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                List<Fila> filas = new ArrayList<>();
                while (rs.next()) {
                    Fila fila = new Fila();
                    for (int i = 1; i <= meta.getColumnCount(); i++) {
                        fila.put(meta.getColumnLabel(i), rs.getObject(i));
                    }
                    filas.add(fila);
                }
                return filas;
            }
        }
    }

    protected Fila consultarUno(String sql, Object... parametros) throws SQLException {
        List<Fila> filas = consultar(sql, parametros);
        return filas.isEmpty() ? null : filas.get(0);
    }

    protected boolean existe(String sql, Object... parametros) throws SQLException {
        return consultarUno(sql, parametros) != null;
    }

    protected int contar(String sql, Object... parametros) throws SQLException {
        Fila f = consultarUno(sql, parametros);
        return f == null ? 0 : ((Number) f.values().iterator().next()).intValue();
    }

    /** UPDATE / DELETE: devuelve las filas afectadas. */
    protected int ejecutar(String sql, Object... parametros) throws SQLException {
        try (Connection cn = Conexion.getConexion(); PreparedStatement ps = cn.prepareStatement(sql)) {
            asignar(ps, parametros);
            return ps.executeUpdate();
        }
    }

    /** INSERT: devuelve el id generado (serial). */
    protected int insertar(String sql, Object... parametros) throws SQLException {
        try (Connection cn = Conexion.getConexion(); PreparedStatement ps = cn.prepareStatement(sql + " RETURNING id")) {
            asignar(ps, parametros);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private static void asignar(PreparedStatement ps, Object[] parametros) throws SQLException {
        for (int i = 0; i < parametros.length; i++) {
            if (parametros[i] == null) {
                ps.setNull(i + 1, Types.NULL);
            } else {
                ps.setObject(i + 1, parametros[i]);
            }
        }
    }
}
