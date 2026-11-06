package bo.uagrm.grupo08sa.util;

import bo.uagrm.grupo08sa.negocio.GestorComandos;
import bo.uagrm.grupo08sa.negocio.Resultado;
import bo.uagrm.grupo08sa.presentacion.InterpreteComando;
import bo.uagrm.grupo08sa.presentacion.NotificadorCorreo;
import bo.uagrm.grupo08sa.presentacion.ProcesadorCorreo;
import bo.uagrm.grupo08sa.presentacion.RespuestaHTML;
import bo.uagrm.grupo08sa.presentacion.correo.ClienteSMTP;

public class EnviarLisredLab46 {
    public static void main(String[] args) throws Exception {
        String destinatario = "encargado.lab236@gmail.com";
        ClienteSMTP smtp = ProcesadorCorreo.crearClienteSMTP();
        GestorComandos gestor = new GestorComandos(new NotificadorCorreo(smtp));

        String asunto = "LISRED[\"LAB-46\"]";
        Resultado r = gestor.ejecutar(InterpreteComando.analizar(asunto, destinatario, null));
        String html = RespuestaHTML.generar(r, asunto);

        smtp.enviar(destinatario, "Re: " + asunto, html);
        System.out.println("Correo de LISRED[\"LAB-46\"] enviado con éxito a " + destinatario);
    }
}

