package bo.uagrm.grupo08sa.util;

import bo.uagrm.grupo08sa.negocio.Comando;
import bo.uagrm.grupo08sa.negocio.GestorComandos;
import bo.uagrm.grupo08sa.negocio.Resultado;
import bo.uagrm.grupo08sa.presentacion.InterpreteComando;

public class TestLisredTodosLabs {
    public static void main(String[] args) throws Exception {
        GestorComandos gestor = new GestorComandos((dest, asunto, cuerpo) -> {});
        String remitente = "encargado.lab236@gmail.com";

        String[] labs = {"LAB-41", "LAB-42", "LAB-43", "LAB-44", "LAB-45", "LAB-46", "LAB-47", "LAB-48", "236-41", "236-46", "*"};
        for (String lab : labs) {
            String asunto = "LISRED[\"" + lab + "\"]";
            Comando c = InterpreteComando.analizar(asunto, remitente, null);
            Resultado r = gestor.ejecutar(c);
            System.out.printf("--- %s -> %s (%s) ---%n", asunto, r.getTitulo(), r.isExito() ? "OK" : "ERROR");
            for (Resultado.Tabla t : r.getTablas()) {
                System.out.printf("    Total equipos: %d | Columnas: %s%n", t.filas().size(), String.join(" | ", t.columnas()));
                if (!t.filas().isEmpty()) {
                    System.out.println("    Fila 1 (Docente): " + String.join(" | ", t.filas().get(0)));
                }
                if (t.filas().size() > 1) {
                    System.out.println("    Fila 2 (PC-01):   " + String.join(" | ", t.filas().get(1)));
                }
            }
        }
    }
}

