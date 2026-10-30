package bo.uagrm.grupo08sa.datos;

import bo.uagrm.grupo08sa.Config;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Conexión JDBC a db_grupo08sa.
 * Abrir una conexión al servidor remoto tarda 1-2 s, así que se mantiene una sola abierta y se reutiliza.
 * Los DAO siguen usando try-with-resources: close() sobre la conexión devuelta no la cierra de verdad.
 */
public final class Conexion {

    /** Solo se verifica la conexión si estuvo inactiva más de este tiempo (cada verificación es un viaje al servidor). */
    private static final long VALIDAR_TRAS_MS = 30_000;

    private static Connection real;
    private static long ultimoUso;

    private Conexion() {
    }

    public static synchronized Connection getConexion() throws SQLException {
        long ahora = System.currentTimeMillis();
        boolean inactiva = ahora - ultimoUso > VALIDAR_TRAS_MS;
        ultimoUso = ahora;
        if (real == null || real.isClosed() || (inactiva && !real.isValid(5))) {
            cerrarSilencioso();
            real = DriverManager.getConnection(
                    Config.get("db.url"), Config.get("db.usuario"), Config.get("db.contrasena"));
        }
        // Cada DAO recibe la conexión en modo autocommit, aunque otro la haya cambiado
        real.setAutoCommit(true);
        return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class}, (proxy, metodo, args) -> {
                    if (metodo.getName().equals("close")) {
                        return null;
                    }
                    try {
                        return metodo.invoke(real, args);
                    } catch (InvocationTargetException e) {
                        throw e.getCause();
                    }
                });
    }

    private static void cerrarSilencioso() {
        if (real != null) {
            try {
                real.close();
            } catch (SQLException ignorada) {
                // la conexión ya estaba rota
            }
            real = null;
        }
    }
}
