package bo.uagrm.grupo08sa.presentacion;

import bo.uagrm.grupo08sa.negocio.Notificador;

/**
 * Notificador del modo local: muestra el aviso en la consola en lugar de enviarlo.
 */
public class NotificadorConsola implements Notificador {

    @Override
    public void enviar(String correo, String asunto, String mensaje) {
        System.out.println("  [NOTIFICACIÓN → " + correo + "] " + asunto + ": " + mensaje);
    }
}
