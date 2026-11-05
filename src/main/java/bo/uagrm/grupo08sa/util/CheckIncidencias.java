package bo.uagrm.grupo08sa.util;

import bo.uagrm.grupo08sa.datos.Conexion;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class CheckIncidencias {
    public static void main(String[] args) throws Exception {
        try (Connection con = Conexion.getConexion();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT count(*) FROM sislab.incidencia")) {
            if (rs.next()) {
                System.out.println("Total incidencias: " + rs.getInt(1));
            }
        }
    }
}

