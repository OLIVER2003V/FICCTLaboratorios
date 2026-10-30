package bo.uagrm.grupo08sa.presentacion;

import bo.uagrm.grupo08sa.negocio.Notificador;
import bo.uagrm.grupo08sa.presentacion.correo.ClienteSMTP;

/**
 * Implementación del Notificador de la capa de negocio: envía el aviso como correo HTML por SMTP.
 */
public class NotificadorCorreo implements Notificador {

    private final ClienteSMTP smtp;

    public NotificadorCorreo(ClienteSMTP smtp) {
        this.smtp = smtp;
    }

    @Override
    public void enviar(String correo, String asunto, String mensaje) throws Exception {
        smtp.enviar(correo, "[Laboratorios FICCT] " + asunto, RespuestaHTML.notificacion(asunto, mensaje));
        ProcesadorCorreo.log("  → notificación a " + correo + ": " + asunto);
    }
}
