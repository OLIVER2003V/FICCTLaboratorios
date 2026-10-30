package bo.uagrm.grupo08sa.negocio;

import bo.uagrm.grupo08sa.datos.dao.ComandoDAO;
import bo.uagrm.grupo08sa.datos.dao.UsuarioDAO;
import bo.uagrm.grupo08sa.datos.modelo.Usuario;

import java.sql.SQLException;

/**
 * Verifica que el comando exista y que el rol del remitente pueda ejecutarlo (tabla rol_comando).
 */
public class Autorizador {

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private final ComandoDAO comandoDAO = new ComandoDAO();

    public Usuario autorizar(String remitente, String nombreComando) throws NegocioException, SQLException {
        if (!comandoDAO.existe(nombreComando)) {
            throw new NegocioException("El comando " + nombreComando
                    + " no existe. Envíe AYUDA[\"*\"] para ver los comandos que puede usar.");
        }
        Usuario usuario = usuarioDAO.buscarPorCorreo(remitente);
        if (usuario == null) {
            throw new AccesoDenegadoException("El correo " + remitente + " no está registrado en el sistema.");
        }
        if (!usuario.estaActivo()) {
            throw new AccesoDenegadoException("Su usuario está dado de baja. Consulte con el Encargado de Laboratorios.");
        }
        if (!comandoDAO.rolPuedeEjecutar(usuario.getRolId(), nombreComando)) {
            throw new AccesoDenegadoException("Su rol (" + usuario.getRolNombre()
                    + ") no tiene permiso para ejecutar " + nombreComando + ".");
        }
        return usuario;
    }
}
