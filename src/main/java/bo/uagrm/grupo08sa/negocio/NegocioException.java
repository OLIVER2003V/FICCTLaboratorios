package bo.uagrm.grupo08sa.negocio;

/**
 * Error de validación o de regla de negocio. Su mensaje se envía tal cual al usuario.
 */
public class NegocioException extends Exception {

    public NegocioException(String mensaje) {
        super(mensaje);
    }
}
