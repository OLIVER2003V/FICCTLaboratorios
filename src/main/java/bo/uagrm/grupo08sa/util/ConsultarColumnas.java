package bo.uagrm.grupo08sa.util;

import bo.uagrm.grupo08sa.datos.Conexion;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class ConsultarColumnas {
    public static void main(String[] args) throws Exception {
        try (Connection con = Conexion.getConexion();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT table_name, column_name, data_type " +
                     "FROM information_schema.columns " +
                     "WHERE table_schema = 'sislab' AND table_name IN ('equipo', 'equipo_red') " +
                     "ORDER BY table_name, ordinal_position")) {
            System.out.println("COLUMNAS:");
            while (rs.next()) {
                System.out.printf("%s.%s (%s)%n",
                        rs.getString("table_name"), rs.getString("column_name"), rs.getString("data_type"));
            }
        }
    }
}

