package bo.uagrm.grupo08sa.negocio;

import bo.uagrm.grupo08sa.datos.modelo.Usuario;

import java.sql.SQLException;

/**
 * Método de un controlador que atiende un comando. Ej.: UsuarioController::registrar para INSUSU.
 */
@FunctionalInterface
public interface Manejador {

    Resultado ejecutar(Comando comando, Usuario solicitante) throws NegocioException, SQLException;
}
