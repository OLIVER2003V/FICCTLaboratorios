package bo.uagrm.grupo08sa.util;

import bo.uagrm.grupo08sa.negocio.Comando;
import bo.uagrm.grupo08sa.negocio.GestorComandos;
import bo.uagrm.grupo08sa.negocio.Resultado;
import bo.uagrm.grupo08sa.presentacion.InterpreteComando;

public class ProbarComandosInventario {
    public static void main(String[] args) throws Exception {
        GestorComandos gestor = new GestorComandos((dest, asunto, cuerpo) -> {});
        String remitente = "encargado.lab236@gmail.com";

        probar(gestor, "LISEQU[\"236-41\"]", remitente);
        probar(gestor, "LISEQU[\"LAB-44\"]", remitente);
        probar(gestor, "LISEQU[\"*\"]", remitente);
        probar(gestor, "LISRED[\"LAB-41\"]", remitente);
        probar(gestor, "LISRED[\"236-45\"]", remitente);
        probar(gestor, "LISRED[\"*\"]", remitente);
    }

    private static void probar(GestorComandos gestor, String cmdStr, String remitente) throws Exception {
        System.out.println("\n========================================================");
        System.out.println("EJECUTANDO: " + cmdStr);
        Comando c = InterpreteComando.analizar(cmdStr, remitente, null);
        Resultado r = gestor.ejecutar(c);
        System.out.println("Resultado: " + (r.isExito() ? "EXITO" : "ERROR") + " | " + r.getTitulo() + " | " + r.getMensaje());
        for (Resultado.Tabla t : r.getTablas()) {
            System.out.println("Columnas: " + String.join(" | ", t.columnas()));
            System.out.println("Total registros: " + t.filas().size());
            int max = Math.min(6, t.filas().size());
            for (int i = 0; i < max; i++) {
                System.out.println("  [" + (i + 1) + "] " + String.join(" | ", t.filas().get(i)));
            }
        }
    }
}
