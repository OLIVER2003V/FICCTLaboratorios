package bo.uagrm.grupo08sa.negocio;

import bo.uagrm.grupo08sa.datos.dao.NotificacionDAO;
import bo.uagrm.grupo08sa.datos.dao.UsuarioDAO;
import bo.uagrm.grupo08sa.datos.modelo.Usuario;

/**
 * Registra cada aviso en la tabla notificacion y lo envía por el Notificador.
 * Un fallo al avisar nunca anula la operación que ya se realizó: solo se registra en consola.
 */
public class Notificaciones {

    private final Notificador canal;
    private final NotificacionDAO notificacionDAO = new NotificacionDAO();
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    public Notificaciones(Notificador canal) {
        this.canal = canal;
    }

    public void aUsuario(int usuarioId, String asunto, String mensaje, Integer reservaId) {
        try {
            Usuario u = usuarioDAO.buscarPorId(usuarioId);
            if (u == null || !u.estaActivo()) {
                return;
            }
            notificacionDAO.insertar(u.getId(), asunto, mensaje, reservaId);
            canal.enviar(u.getCorreo(), asunto, mensaje);
        } catch (Exception e) {
            System.err.println("[NOTIFICACION] No se pudo avisar al usuario " + usuarioId + ": " + e.getMessage());
        }
    }

    /** Avisa a todos los Encargados activos, salvo a quien realizó la acción. */
    public void aEncargados(String asunto, String mensaje, Integer reservaId, int excluirId) {
        try {
            for (Usuario u : usuarioDAO.listarActivosPorRol("ENC")) {
                if (u.getId() != excluirId) {
                    aUsuario(u.getId(), asunto, mensaje, reservaId);
                }
            }
        } catch (Exception e) {
            System.err.println("[NOTIFICACION] No se pudo avisar a los Encargados: " + e.getMessage());
        }
    }
}
