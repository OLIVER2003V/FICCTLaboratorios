package bo.uagrm.grupo08sa.util;

import bo.uagrm.grupo08sa.datos.Conexion;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class ConsultarTodosEquipoRed {
    public static void main(String[] args) throws Exception {
        try (Connection con = Conexion.getConexion();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT l.codigo as lab, COUNT(*) as cant, MIN(r.ip) as min_ip, MAX(r.ip) as max_ip " +
                     "FROM sislab.equipo_red r " +
                     "JOIN sislab.equipo e ON e.id = r.equipo_id " +
                     "JOIN sislab.laboratorio l ON l.id = e.laboratorio_id " +
                     "GROUP BY l.codigo ORDER BY l.codigo")) {
            System.out.println("RESUMEN RED POR LAB:");
            while (rs.next()) {
                System.out.printf("%s: %d equipos (IP %s a %s)%n",
                        rs.getString("lab"), rs.getInt("cant"), rs.getString("min_ip"), rs.getString("max_ip"));
            }
        }
    }
}

