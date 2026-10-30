package bo.uagrm.grupo08sa.negocio;

/**
 * Canal por el que se avisa a los usuarios (CU4, CU5, CU6, CU7).
 * La capa de negocio solo conoce esta interfaz; la capa de presentación la implementa enviando un correo,
 * así el negocio no depende de SMTP.
 */
public interface Notificador {

    void enviar(String correo, String asunto, String mensaje) throws Exception;
}
