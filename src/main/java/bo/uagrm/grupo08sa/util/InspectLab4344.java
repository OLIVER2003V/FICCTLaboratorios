package bo.uagrm.grupo08sa.util;

import bo.uagrm.grupo08sa.datos.Conexion;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class InspectLab4344 {
    public static void main(String[] args) throws Exception {
        try (Connection con = Conexion.getConexion();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT l.codigo as lab, e.codigo, r.ip, r.mascara, r.gateway, r.dns1, r.grupo_red " +
                     "FROM sislab.equipo_red r " +
                     "JOIN sislab.equipo e ON e.id = r.equipo_id " +
                     "JOIN sislab.laboratorio l ON l.id = e.laboratorio_id " +
                     "WHERE l.codigo IN ('LAB-43', 'LAB-44') " +
                     "ORDER BY l.codigo, e.codigo")) {
            while (rs.next()) {
                System.out.printf("%s | %s | %s | %s%n",
                        rs.getString("lab"), rs.getString("codigo"), rs.getString("ip"), rs.getString("grupo_red"));
            }
        }
    }
}

