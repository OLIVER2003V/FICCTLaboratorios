package bo.uagrm.grupo08sa.negocio;

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

    public GestorComandos(Notificador notificador) {
        Notificaciones notificaciones = new Notificaciones(notificador);
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

    public Resultado ejecutar(Comando comando) {
        try {
            Usuario solicitante = autorizador.autorizar(comando.getRemitente(), comando.getNombre());
            Manejador manejador = manejadores.get(comando.getNombre());
            if (manejador == null) {
                throw new NegocioException("El comando " + comando.getNombre() + " todavía no está implementado.");
            }
            Resultado resultado = manejador.ejecutar(comando, solicitante);
            registrar(comando, "OK", resultado.getTitulo());
            return resultado;
        } catch (AccesoDenegadoException e) {
            registrar(comando, "DENEGADO", e.getMessage());
            return Resultado.error(e.getMessage());
        } catch (NegocioException e) {
            registrar(comando, "ERROR", e.getMessage());
            return Resultado.error(e.getMessage());
        } catch (SQLException e) {
            System.err.println("[BD] " + e.getMessage());
            registrar(comando, "ERROR", "BD: " + e.getMessage());
            return Resultado.error("Ocurrió un error en la base de datos al procesar " + comando.getNombre()
                    + ". Intente nuevamente o consulte con el Encargado.");
        }
    }

    /** Para asuntos que ni siquiera tienen el formato COMANDO["..."]. */
    public Resultado rechazarFormato(String remitente, String asunto, String detalle) {
        try {
            bitacoraDAO.insertar(remitente, null, asunto, "ERROR", detalle);
        } catch (SQLException e) {
            System.err.println("[BITACORA] " + e.getMessage());
        }
        return Resultado.error(detalle);
    }

    /** Tareas automáticas que se ejecutan en cada vuelta del bucle principal (CU7: faltas). */
    public void ejecutarTareasProgramadas() {
        try {
            asistencia.marcarFaltas();
        } catch (SQLException e) {
            System.err.println("[TAREAS] " + e.getMessage());
        }
    }

    private void registrar(Comando comando, String resultado, String detalle) {
        try {
            bitacoraDAO.insertar(comando.getRemitente(), comando.getNombre(), comando.getAsunto(), resultado, detalle);
        } catch (SQLException e) {
            System.err.println("[BITACORA] " + e.getMessage());
        }
    }
}
