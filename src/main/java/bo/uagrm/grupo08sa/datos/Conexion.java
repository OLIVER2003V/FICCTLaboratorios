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
            real = conectarConFallback();
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

    private static Connection conectarConFallback() throws SQLException {
        String urlConfig = Config.get("db.url");
        String usuario = Config.get("db.usuario");
        String pass = Config.get("db.contrasena");

        java.util.List<String> urls = new java.util.ArrayList<>();
        urls.add(urlConfig);

        if (urlConfig != null && urlConfig.contains("tecnoweb.org.bo")) {
            String[] hosts = {"mail.tecnoweb.org.bo", "tecnoweb.org.bo", "www.tecnoweb.org.bo", "db.tecnoweb.org.bo"};
            for (String h : hosts) {
                String candidate = urlConfig.replaceFirst("://[^:/]+(:|/)", "://" + h + "$1");
                if (!urls.contains(candidate)) {
                    urls.add(candidate);
                }
            }
        }

        SQLException ultimoError = null;
        for (String url : urls) {
            try {
                String urlConTimeout = url.contains("?") ? url + "&connectTimeout=4" : url + "?connectTimeout=4";
                Connection c = DriverManager.getConnection(urlConTimeout, usuario, pass);
                if (c != null && !c.isClosed()) {
                    System.out.println("[BD] Conectado exitosamente con: " + url);
                    return c;
                }
            } catch (SQLException e) {
                ultimoError = e;
                String detalle = e.getCause() != null ? " (" + e.getCause().getClass().getSimpleName() + ": " + e.getCause().getMessage() + ")" : "";
                System.err.println("[BD-PROBANDO] Falló " + url + " -> " + e.getMessage() + detalle);
                String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
                if (msg.contains("password") || msg.contains("autenticación") || msg.contains("authentication")) {
                    throw e;
                }
            }
        }
        if (ultimoError != null) {
            String detalle = ultimoError.getCause() != null ? " (" + ultimoError.getCause().getMessage() + ")" : "";
            System.err.println("[BD-DETALLE] Intento fallido de conexión a PostgreSQL: " + ultimoError.getMessage() + detalle);
            throw ultimoError;
        }
        throw new SQLException("No se pudo establecer conexión a la base de datos.");
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
