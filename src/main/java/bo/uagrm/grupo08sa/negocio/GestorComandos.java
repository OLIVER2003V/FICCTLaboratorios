package bo.uagrm.grupo08sa.negocio;

import bo.uagrm.grupo08sa.Log;
import bo.uagrm.grupo08sa.datos.Conexion;
import bo.uagrm.grupo08sa.datos.dao.BitacoraDAO;
import bo.uagrm.grupo08sa.datos.modelo.Usuario;
import bo.uagrm.grupo08sa.negocio.controladores.AsistenciaController;
import bo.uagrm.grupo08sa.negocio.controladores.AyudaController;
import bo.uagrm.grupo08sa.negocio.controladores.ConfiguracionController;
import bo.uagrm.grupo08sa.negocio.controladores.HorarioController;
import bo.uagrm.grupo08sa.negocio.controladores.InventarioController;
import bo.uagrm.grupo08sa.negocio.controladores.PermutaController;
import bo.uagrm.grupo08sa.negocio.controladores.ReporteController;
import bo.uagrm.grupo08sa.negocio.controladores.ReservaController;
import bo.uagrm.grupo08sa.negocio.controladores.UsuarioController;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Punto de entrada a la capa de negocio: autoriza, despacha el comando a su controlador
 * y registra cada solicitud en la bitácora.
 */
public class GestorComandos {

    private final Autorizador autorizador = new Autorizador();
    private final BitacoraDAO bitacoraDAO = new BitacoraDAO();
    private final Map<String, Manejador> manejadores = new HashMap<>();
    private final AsistenciaController asistencia;
    private final Notificaciones notificaciones;

    public GestorComandos(Notificador notificador) {
        notificaciones = new Notificaciones(notificador);
        asistencia = new AsistenciaController(notificaciones);

        new AyudaController().registrarEn(manejadores);
        new UsuarioController().registrarEn(manejadores);                       // CU1
        new ConfiguracionController().registrarEn(manejadores);                 // CU2
        new HorarioController().registrarEn(manejadores);                       // CU3
        new ReservaController(notificaciones).registrarEn(manejadores);         // CU4
        new PermutaController(notificaciones).registrarEn(manejadores);         // CU5
        new InventarioController(notificaciones).registrarEn(manejadores);      // CU6
        asistencia.registrarEn(manejadores);                                    // CU7
        new ReporteController().registrarEn(manejadores);                       // CU8
    }

    /**
     * Cada comando se ejecuta en una sola transacción: si termina bien se hace COMMIT; ante cualquier error
     * (de negocio, de base de datos o inesperado) se hace ROLLBACK y no queda ningún cambio a medias.
     * La bitácora se escribe después, fuera de la transacción, para que también queden los errores.
     */
    public Resultado ejecutar(Comando comando) {
        long inicio = System.currentTimeMillis();
        try {
            Conexion.iniciarTransaccion();
        } catch (SQLException e) {
            return errorBD(comando, e, inicio);
        }
        try {
            Usuario solicitante = autorizador.autorizar(comando.getRemitente(), comando.getNombre());
            Manejador manejador = manejadores.get(comando.getNombre());
            if (manejador == null) {
                throw new NegocioException("El comando " + comando.getNombre() + " todavía no está implementado.");
            }
            Resultado resultado = manejador.ejecutar(comando, solicitante);
            Conexion.confirmar();
            notificaciones.enviarPendientes();
            registrar(comando, "OK", resultado.getTitulo(), inicio);
            return resultado;
        } catch (AccesoDenegadoException e) {
            deshacer();
            registrar(comando, "DENEGADO", e.getMessage(), inicio);
            return Resultado.error(e.getMessage());
        } catch (NegocioException e) {
            deshacer();
            registrar(comando, "ERROR", e.getMessage(), inicio);
            return Resultado.error(e.getMessage());
        } catch (SQLException e) {
            deshacer();
            return errorBD(comando, e, inicio);
        } catch (RuntimeException e) {
            deshacer();
            Log.error("Error interno en " + comando.getNombre() + " de " + comando.getRemitente()
                    + " " + Log.cita(comando.getAsunto()), e);
            registrar(comando, "ERROR", "Interno: " + e, inicio);
            return Resultado.error("Ocurrió un error interno al procesar " + comando.getNombre()
                    + ". No se guardó ningún cambio.");
        } finally {
            // Red de seguridad (p. ej. ante un Error de la JVM): nunca dejar la transacción abierta
            if (Conexion.hayTransaccion()) {
                deshacer();
            }
        }
    }

    private void deshacer() {
        Conexion.revertir();
        notificaciones.descartarPendientes();
    }

    private Resultado errorBD(Comando comando, SQLException e, long inicio) {
        String detalle = e.getCause() != null ? " (" + e.getCause().getMessage() + ")" : "";
        Log.error("Base de datos en " + comando.getNombre() + " de " + comando.getRemitente()
                + " " + Log.cita(comando.getAsunto()), e);
        registrar(comando, "ERROR", "BD: " + e.getMessage() + detalle, inicio);
        return Resultado.error("Ocurrió un error en la base de datos al procesar " + comando.getNombre()
                + ". No se guardó ningún cambio; intente nuevamente o consulte con el Encargado.");
    }

    /** Para asuntos que ni siquiera tienen el formato COMANDO["..."]. */
    public Resultado rechazarFormato(String remitente, String asunto, String detalle) {
        Log.acceso("COMANDO", "de=" + remitente + " cmd=- resultado=FORMATO detalle=" + Log.cita(detalle));
        try {
            bitacoraDAO.insertar(remitente, null, asunto, "ERROR", detalle);
        } catch (SQLException e) {
            Log.error("No se pudo registrar en la bitácora el asunto inválido de " + remitente, e);
        }
        return Resultado.error(detalle);
    }

    /** Tareas automáticas que se ejecutan en cada vuelta del bucle principal (CU7: faltas). */
    public void ejecutarTareasProgramadas() {
        try {
            asistencia.marcarFaltas();
        } catch (SQLException e) {
            Log.error("Tarea automática: marcar faltas", e);
        }
    }

    /** access.log + tabla bitacora. */
    private void registrar(Comando comando, String resultado, String detalle, long inicio) {
        Log.acceso("COMANDO", "de=" + comando.getRemitente() + " cmd=" + comando.getNombre()
                + " params=" + comando.cantidad() + " resultado=" + resultado + " detalle=" + Log.cita(detalle)
                + " (" + (System.currentTimeMillis() - inicio) + " ms)");
        try {
            bitacoraDAO.insertar(comando.getRemitente(), comando.getNombre(), comando.getAsunto(), resultado, detalle);
        } catch (SQLException e) {
            Log.error("No se pudo registrar en la bitácora el comando " + comando.getNombre(), e);
        }
    }
}
