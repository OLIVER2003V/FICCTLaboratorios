package bo.uagrm.grupo08sa.negocio;

/**
 * El remitente no está registrado, está inactivo o su rol no puede ejecutar el comando.
 */
public class AccesoDenegadoException extends NegocioException {

    public AccesoDenegadoException(String mensaje) {
        super(mensaje);
    }
}
