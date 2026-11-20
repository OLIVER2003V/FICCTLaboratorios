package bo.uagrm.grupo08sa.datos;

import bo.uagrm.grupo08sa.Config;
import bo.uagrm.grupo08sa.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Savepoint;

/**
 * Conexión JDBC a db_grupo08sa.
 * Abrir una conexión al servidor remoto tarda 1-2 s, así que se mantiene una sola abierta y se reutiliza.
 * Los DAO siguen usando try-with-resources: close() sobre la conexión devuelta no la cierra de verdad.
 *
 * Transacciones: GestorComandos envuelve cada comando entre iniciarTransaccion() y confirmar()/revertir(),
 * así un comando que escribe en varias tablas se guarda completo o no se guarda nada.
 */
public final class Conexion {

    /** Solo se verifica la conexión si estuvo inactiva más de este tiempo (cada verificación es un viaje al servidor). */
    private static final long VALIDAR_TRAS_MS = 30_000;

    private static Connection real;
    private static long ultimoUso;
    /** true entre iniciarTransaccion() y confirmar()/revertir(): todos los DAO comparten esa transacción. */
    private static boolean enTransaccion;
    /**
     * Modo --simular: toda la sesión corre en una sola transacción que se revierte al final.
     * Cada comando usa un SAVEPOINT en lugar de BEGIN/COMMIT, así se prueban todos los comandos
     * (incluidos los que dependen de otros) sin dejar cambios en la base.
     */
    private static boolean simulacion;
    private static Savepoint puntoComando;

    private Conexion() {
    }

    /** Activa el modo simulación: nada de lo que se haga desde aquí se guarda. */
    public static synchronized void activarSimulacion() throws SQLException {
        getConexion();
        real.setAutoCommit(false);
        simulacion = true;
    }

    /** Deshace todo lo hecho durante la simulación. */
    public static synchronized void terminarSimulacion() {
        try {
            if (real != null && !real.isClosed()) {
                real.rollback();
                real.setAutoCommit(true);
            }
        } catch (SQLException e) {
            Log.error("No se pudo revertir la simulación", e);
            cerrarSilencioso();
        } finally {
            simulacion = false;
        }
    }

    public static synchronized Connection getConexion() throws SQLException {
        if (enTransaccion) {
            // Dentro de una transacción no se puede reconectar: se perderían los cambios ya hechos
            if (real == null || real.isClosed()) {
                throw new SQLException("Se perdió la conexión con la base de datos durante la transacción.");
            }
            ultimoUso = System.currentTimeMillis();
            return envolver();
        }
        if (simulacion) {
            // Reconectar perdería todo lo simulado y escribiría en modo autocommit: mejor fallar
            if (real == null || real.isClosed()) {
                throw new SQLException("Se perdió la conexión durante la simulación.");
            }
            return envolver();
        }
        long ahora = System.currentTimeMillis();
        boolean inactiva = ahora - ultimoUso > VALIDAR_TRAS_MS;
        ultimoUso = ahora;
        if (real == null || real.isClosed() || (inactiva && !real.isValid(5))) {
            cerrarSilencioso();
            real = conectarConFallback();
        }
        // Fuera de una transacción, cada DAO recibe la conexión en modo autocommit
        real.setAutoCommit(true);
        return envolver();
    }

    // ---- Transacciones: un comando del correo = una transacción ----

    /** BEGIN: desde aquí, todo lo que hagan los DAO se confirma o se revierte junto. */
    public static synchronized void iniciarTransaccion() throws SQLException {
        if (enTransaccion) {
            throw new SQLException("Ya hay una transacción en curso.");
        }
        getConexion();  // asegura una conexión válida antes de empezar
        if (simulacion) {
            puntoComando = real.setSavepoint();
        } else {
            real.setAutoCommit(false);
        }
        enTransaccion = true;
    }

    /** COMMIT y vuelta al modo autocommit. */
    public static synchronized void confirmar() throws SQLException {
        try {
            if (simulacion) {
                real.releaseSavepoint(puntoComando);
            } else {
                real.commit();
            }
        } finally {
            terminarTransaccion();
        }
    }

    /** ROLLBACK y vuelta al modo autocommit. Nunca lanza: se usa al manejar otro error. */
    public static synchronized void revertir() {
        if (!enTransaccion) {
            return;  // ya terminó (por ejemplo, falló el COMMIT y la transacción quedó cerrada)
        }
        try {
            if (real != null && !real.isClosed()) {
                if (simulacion) {
                    real.rollback(puntoComando);
                } else {
                    real.rollback();
                }
            }
        } catch (SQLException e) {
            Log.error("No se pudo revertir la transacción", e);
            cerrarSilencioso();  // la próxima operación abrirá una conexión nueva
        } finally {
            terminarTransaccion();
        }
    }

    public static synchronized boolean hayTransaccion() {
        return enTransaccion;
    }

    /**
     * Ejecuta una operación secundaria (por ejemplo, guardar una notificación) dentro de un SAVEPOINT.
     * En PostgreSQL, un error dentro de una transacción la invalida entera; el savepoint permite
     * deshacer solo esa operación y seguir con la transacción principal.
     */
    public static void aislado(OperacionBD operacion) throws SQLException {
        if (!hayTransaccion() && !simulacion) {
            operacion.ejecutar();
            return;
        }
        Savepoint punto = real.setSavepoint();
        try {
            operacion.ejecutar();
            real.releaseSavepoint(punto);
        } catch (SQLException | RuntimeException e) {
            real.rollback(punto);
            throw e;
        }
    }

    @FunctionalInterface
    public interface OperacionBD {
        void ejecutar() throws SQLException;
    }

    private static void terminarTransaccion() {
        enTransaccion = false;
        puntoComando = null;
        if (simulacion) {
            return;  // la sesión simulada sigue en su transacción exterior
        }
        try {
            if (real != null && !real.isClosed()) {
                real.setAutoCommit(true);
            }
        } catch (SQLException e) {
            cerrarSilencioso();
        }
    }

    private static Connection envolver() {
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
                    Log.info("[BD] Conectado con: " + url);
                    return c;
                }
            } catch (SQLException e) {
                ultimoError = e;
                String detalle = e.getCause() != null ? " (" + e.getCause().getClass().getSimpleName() + ": " + e.getCause().getMessage() + ")" : "";
                Log.error("[BD] Falló la conexión con " + url + ": " + e.getMessage() + detalle);
                String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
                if (msg.contains("password") || msg.contains("autenticación") || msg.contains("authentication")) {
                    throw e;
                }
            }
        }
        if (ultimoError != null) {
            String detalle = ultimoError.getCause() != null ? " (" + ultimoError.getCause().getMessage() + ")" : "";
            Log.error("[BD] No se pudo conectar a PostgreSQL con ninguna dirección: " + ultimoError.getMessage() + detalle);
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
