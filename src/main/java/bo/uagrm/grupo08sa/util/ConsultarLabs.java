package bo.uagrm.grupo08sa.util;

import bo.uagrm.grupo08sa.datos.Conexion;
import bo.uagrm.grupo08sa.datos.Fila;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class ConsultarLabs {
    public static void main(String[] args) throws Exception {
        try (Connection con = Conexion.getConexion();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT id, codigo, nombre, capacidad, ubicacion, estado FROM sislab.laboratorio ORDER BY codigo")) {
            System.out.println("LABORATORIOS EN BD:");
            while (rs.next()) {
                System.out.printf("ID=%d | Codigo=%s | Nombre=%s | Capacidad=%d | Ubicacion=%s | Estado=%s%n",
                        rs.getInt("id"), rs.getString("codigo"), rs.getString("nombre"),
                        rs.getInt("capacidad"), rs.getString("ubicacion"), rs.getString("estado"));
            }
        }
    }
}

