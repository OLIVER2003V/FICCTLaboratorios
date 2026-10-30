package bo.uagrm.grupo08sa.datos;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Ejecuta los scripts de la carpeta sql/ (no hace falta tener psql instalado).
 */
public final class InicializadorBD {

    private InicializadorBD() {
    }

    public static boolean existenTablas() throws SQLException {
        try (Connection cn = Conexion.getConexion()) {
            DatabaseMetaData meta = cn.getMetaData();
            try (ResultSet rs = meta.getTables(null, "sislab", "usuario", new String[]{"TABLE"})) {
                return rs.next();
            }
        }
    }

    /** El driver de PostgreSQL acepta varias sentencias separadas por ';' en un solo execute(). */
    public static void ejecutarScript(Path archivo) throws IOException, SQLException {
        String sql = Files.readString(archivo, StandardCharsets.UTF_8);
        try (Connection cn = Conexion.getConexion(); Statement st = cn.createStatement()) {
            cn.setAutoCommit(false);
            try {
                st.execute(sql);
                cn.commit();
            } catch (SQLException e) {
                cn.rollback();
                throw e;
            }
        }
    }
}
