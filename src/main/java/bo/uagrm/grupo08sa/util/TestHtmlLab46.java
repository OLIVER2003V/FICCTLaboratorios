package bo.uagrm.grupo08sa.util;

import bo.uagrm.grupo08sa.negocio.Comando;
import bo.uagrm.grupo08sa.negocio.GestorComandos;
import bo.uagrm.grupo08sa.negocio.Resultado;
import bo.uagrm.grupo08sa.presentacion.InterpreteComando;
import bo.uagrm.grupo08sa.presentacion.RespuestaHTML;

public class TestHtmlLab46 {
    public static void main(String[] args) throws Exception {
        GestorComandos gestor = new GestorComandos((dest, asunto, cuerpo) -> {});
        String remitente = "encargado.lab236@gmail.com";
        String asunto = "LISEQU[\"LAB-46\"]";

        Comando c = InterpreteComando.analizar(asunto, remitente, null);
        Resultado r = gestor.ejecutar(c);
        String html = RespuestaHTML.generar(r, asunto);

        System.out.println("--- HTML GENERADO ---");
        // Print the first 1000 characters of HTML
        System.out.println(html.substring(0, Math.min(1000, html.length())));
    }
}

