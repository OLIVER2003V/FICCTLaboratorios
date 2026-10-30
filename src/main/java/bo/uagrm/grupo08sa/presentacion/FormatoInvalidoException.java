package bo.uagrm.grupo08sa.presentacion;

/**
 * El asunto no respeta el formato COMANDO["param1","param2",...].
 */
public class FormatoInvalidoException extends Exception {

    public FormatoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
