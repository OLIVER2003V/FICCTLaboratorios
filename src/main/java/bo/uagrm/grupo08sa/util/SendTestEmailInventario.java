package bo.uagrm.grupo08sa.util;

import bo.uagrm.grupo08sa.negocio.GestorComandos;
import bo.uagrm.grupo08sa.negocio.Resultado;
import bo.uagrm.grupo08sa.presentacion.InterpreteComando;
import bo.uagrm.grupo08sa.presentacion.NotificadorCorreo;
import bo.uagrm.grupo08sa.presentacion.ProcesadorCorreo;
import bo.uagrm.grupo08sa.presentacion.RespuestaHTML;
import bo.uagrm.grupo08sa.presentacion.correo.ClienteSMTP;

public class SendTestEmailInventario {
    public static void main(String[] args) throws Exception {
        String destinatario = "encargado.lab236@gmail.com";
        ClienteSMTP smtp = ProcesadorCorreo.crearClienteSMTP();
        GestorComandos gestor = new GestorComandos(new NotificadorCorreo(smtp));

        // 1. Enviar correo de prueba LISEQU["236-44"]
        String asunto1 = "LISEQU[\"236-44\"]";
        Resultado r1 = gestor.ejecutar(InterpreteComando.analizar(asunto1, destinatario, null));
        String html1 = RespuestaHTML.generar(r1, asunto1);
        smtp.enviar(destinatario, "Re: " + asunto1, html1);
        System.out.println("Correo enviado: " + asunto1);

        // 2. Enviar correo de prueba LISRED["LAB-41"]
        String asunto2 = "LISRED[\"LAB-41\"]";
        Resultado r2 = gestor.ejecutar(InterpreteComando.analizar(asunto2, destinatario, null));
        String html2 = RespuestaHTML.generar(r2, asunto2);
        smtp.enviar(destinatario, "Re: " + asunto2, html2);
        System.out.println("Correo enviado: " + asunto2);
    }
}

