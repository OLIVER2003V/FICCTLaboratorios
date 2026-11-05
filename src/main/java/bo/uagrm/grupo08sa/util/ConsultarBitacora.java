package bo.uagrm.grupo08sa.util;

import bo.uagrm.grupo08sa.datos.Conexion;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class ConsultarBitacora {
    public static void main(String[] args) throws Exception {
        try (Connection con = Conexion.getConexion();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT id, remitente, comando, asunto, resultado, detalle, fecha " +
                     "FROM sislab.bitacora ORDER BY id DESC LIMIT 10")) {
            System.out.println("ULTIMOS REGISTROS DE BITACORA:");
            while (rs.next()) {
                System.out.printf("[%d] %s | %s | %s | %s | %s | %s%n",
                        rs.getInt("id"), rs.getString("fecha"), rs.getString("remitente"),
                        rs.getString("comando"), rs.getString("asunto"),
                        rs.getString("resultado"), rs.getString("detalle"));
            }
        }
    }
}

