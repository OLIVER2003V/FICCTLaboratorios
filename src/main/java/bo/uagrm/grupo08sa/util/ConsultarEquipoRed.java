package bo.uagrm.grupo08sa.util;

import bo.uagrm.grupo08sa.datos.Conexion;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class ConsultarEquipoRed {
    public static void main(String[] args) throws Exception {
        try (Connection con = Conexion.getConexion();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT l.codigo as lab, e.codigo, r.ip, r.mascara, r.gateway, r.dns1, r.grupo_red " +
                     "FROM sislab.equipo_red r " +
                     "JOIN sislab.equipo e ON e.id = r.equipo_id " +
                     "JOIN sislab.laboratorio l ON l.id = e.laboratorio_id " +
                     "ORDER BY l.codigo, r.ip LIMIT 50")) {
            System.out.println("EQUIPOS DE RED EN BD:");
            while (rs.next()) {
                System.out.printf("%s | %s | %s | %s | %s | %s | %s%n",
                        rs.getString("lab"), rs.getString("codigo"), rs.getString("ip"),
                        rs.getString("mascara"), rs.getString("gateway"), rs.getString("dns1"), rs.getString("grupo_red"));
            }
        }
    }
}

