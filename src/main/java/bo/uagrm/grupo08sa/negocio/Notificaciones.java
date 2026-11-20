package bo.uagrm.grupo08sa.negocio;

import bo.uagrm.grupo08sa.Log;
import bo.uagrm.grupo08sa.datos.Conexion;
import bo.uagrm.grupo08sa.datos.dao.NotificacionDAO;
import bo.uagrm.grupo08sa.datos.dao.UsuarioDAO;
import bo.uagrm.grupo08sa.datos.modelo.Usuario;

import java.util.ArrayList;
import java.util.List;

/**
 * Registra cada aviso en la tabla notificacion y lo envía por el Notificador.
 * Un fallo al avisar nunca anula la operación que ya se realizó: solo se registra en consola.
 *
 * Si el aviso se genera dentro de la transacción de un comando, el correo queda en espera:
 * se envía con enviarPendientes() después del COMMIT, o se descarta con descartarPendientes() si hubo ROLLBACK.
 */
public class Notificaciones {

    private record Aviso(String correo, String asunto, String mensaje) {
    }

    private final Notificador canal;
    private final NotificacionDAO notificacionDAO = new NotificacionDAO();
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private final List<Aviso> pendientes = new ArrayList<>();

    public Notificaciones(Notificador canal) {
        this.canal = canal;
    }

    public void aUsuario(int usuarioId, String asunto, String mensaje, Integer reservaId) {
        try {
            Usuario u = usuarioDAO.buscarPorId(usuarioId);
            if (u == null || !u.estaActivo()) {
                return;
            }
            // En un savepoint: si falla, no invalida la transacción del comando
            Conexion.aislado(() -> notificacionDAO.insertar(u.getId(), asunto, mensaje, reservaId));
            if (Conexion.hayTransaccion()) {
                pendientes.add(new Aviso(u.getCorreo(), asunto, mensaje));
            } else {
                canal.enviar(u.getCorreo(), asunto, mensaje);
            }
        } catch (Exception e) {
            Log.error("No se pudo avisar al usuario " + usuarioId, e);
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
            Log.error("No se pudo avisar a los Encargados", e);
        }
    }

    /** Después del COMMIT: envía los correos que quedaron en espera. */
    public void enviarPendientes() {
        List<Aviso> lote = new ArrayList<>(pendientes);
        pendientes.clear();
        for (Aviso a : lote) {
            try {
                canal.enviar(a.correo(), a.asunto(), a.mensaje());
            } catch (Exception e) {
                Log.error("No se pudo enviar el aviso a " + a.correo() + " " + Log.cita(a.asunto()), e);
            }
        }
    }

    /** Después del ROLLBACK: la operación no se guardó, así que no se avisa a nadie. */
    public void descartarPendientes() {
        pendientes.clear();
    }
}
