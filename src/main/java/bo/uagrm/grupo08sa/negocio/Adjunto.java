package bo.uagrm.grupo08sa.negocio;

/**
 * Archivo adjunto al correo (por ejemplo, el horario en Excel para CARHOR).
 */
public record Adjunto(String nombre, byte[] datos) {
}
