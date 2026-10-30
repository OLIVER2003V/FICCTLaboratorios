package bo.uagrm.grupo08sa.presentacion.correo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;
import javax.net.ssl.SSLSocketFactory;

/**
 * Cliente SMTP (RFC 5321) implementado con sockets.
 * Diálogo: EHLO, [AUTH LOGIN], MAIL FROM, RCPT TO, DATA, QUIT. El cuerpo va en HTML UTF-8 en base64 (MIME).
 *
 * - tecnoweb (puerto 25): sin SSL ni autenticación; solo reenvía a otros dominios desde IPs autorizadas.
 * - Gmail u otro proveedor (puerto 465): SSL desde el inicio y AUTH LOGIN con usuario y contraseña.
 */
public class ClienteSMTP {

    private final String servidor;
    private final int puerto;
    private final boolean ssl;
    private final String usuario;
    private final String contrasena;
    private final String remitente;
    private final String responderA;

    /**
     * @param usuario    vacío = sin autenticación
     * @param remitente  dirección del sobre (MAIL FROM) y del encabezado From
     * @param responderA dirección del sistema (Reply-To), para que "Responder" vuelva al buzón del grupo
     */
    public ClienteSMTP(String servidor, int puerto, boolean ssl, String usuario, String contrasena,
                       String remitente, String responderA) {
        this.servidor = servidor;
        this.puerto = puerto;
        this.ssl = ssl;
        this.usuario = usuario;
        this.contrasena = contrasena;
        this.remitente = remitente;
        this.responderA = responderA;
    }

    public void enviar(String destinatario, String asunto, String html) throws IOException {
        try (Sesion s = abrirSesion()) {
            s.comando("MAIL FROM:<" + remitente + ">", 250);
            s.comando("RCPT TO:<" + destinatario + ">", 250, 251);
            s.comando("DATA", 354);
            s.salida.write(construirMensaje(destinatario, asunto, html));
            s.comando(".", 250);
            s.comando("QUIT", 221);
        }
    }

    /**
     * Verifica si el servidor acepta enviar al destinatario, sin enviar nada (RSET antes de DATA).
     */
    public String probarReenvio(String destinatario) throws IOException {
        try (Sesion s = abrirSesion()) {
            s.comando("MAIL FROM:<" + remitente + ">", 250);
            String resultado;
            try {
                s.comando("RCPT TO:<" + destinatario + ">", 250, 251);
                resultado = "permitido";
            } catch (IOException e) {
                resultado = "RECHAZADO (" + e.getMessage() + ")";
            }
            s.comando("RSET", 250);
            s.comando("QUIT", 221);
            return "servidor " + servidor + ":" + puerto + (ssl ? " (SSL)" : "")
                    + ", IP vista: " + s.ip + " — envío a " + destinatario + ": " + resultado;
        }
    }

    /** Conecta, saluda con EHLO y, si hay usuario configurado, se autentica. */
    private Sesion abrirSesion() throws IOException {
        Socket socket = ssl ? SSLSocketFactory.getDefault().createSocket() : new Socket();
        Sesion s = new Sesion(socket);
        try {
            socket.connect(new InetSocketAddress(servidor, puerto), 15_000);
            socket.setSoTimeout(30_000);
            s.entrada = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
            s.salida = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.US_ASCII));

            s.esperar("saludo", 220);
            String saludo = s.comando("EHLO " + dominio(responderA), 250);
            int inicio = saludo.indexOf('[');
            int fin = saludo.indexOf(']', inicio + 1);
            s.ip = inicio >= 0 && fin > inicio ? saludo.substring(inicio + 1, fin) : "desconocida";

            if (!usuario.isEmpty()) {
                // AUTH LOGIN: el servidor pide usuario y contraseña, cada uno en base64
                s.comando("AUTH LOGIN", 334);
                s.comando(base64(usuario), "AUTH (usuario)", 334);
                s.comando(base64(contrasena), "AUTH (revise usuario y contraseña)", 235);
            }
            return s;
        } catch (IOException e) {
            s.close();
            throw e;
        }
    }

    private String construirMensaje(String destinatario, String asunto, String html) {
        String cuerpo = Base64.getMimeEncoder().encodeToString(html.getBytes(StandardCharsets.UTF_8));
        String fecha = DateTimeFormatter.RFC_1123_DATE_TIME.withLocale(Locale.US).format(ZonedDateTime.now());
        // El cuerpo en base64 nunca empieza una línea con '.', así que no hace falta dot-stuffing
        return "From: Sistema de Laboratorios FICCT <" + remitente + ">\r\n"
                + "Reply-To: <" + responderA + ">\r\n"
                + "To: <" + destinatario + ">\r\n"
                + "Subject: " + CodificacionMIME.codificarEncabezado(asunto) + "\r\n"
                + "Date: " + fecha + "\r\n"
                + "Message-ID: <" + UUID.randomUUID() + "@" + dominio(responderA) + ">\r\n"
                + "MIME-Version: 1.0\r\n"
                + "Content-Type: text/html; charset=UTF-8\r\n"
                + "Content-Transfer-Encoding: base64\r\n"
                + "\r\n"
                + cuerpo + "\r\n";
    }

    private static String base64(String texto) {
        return Base64.getEncoder().encodeToString(texto.getBytes(StandardCharsets.UTF_8));
    }

    private static String dominio(String correo) {
        return correo.substring(correo.indexOf('@') + 1);
    }

    /** Una conexión SMTP abierta con sus flujos de lectura y escritura. */
    private static final class Sesion implements AutoCloseable {

        private final Socket socket;
        private BufferedReader entrada;
        private BufferedWriter salida;
        private String ip;

        Sesion(Socket socket) {
            this.socket = socket;
        }

        String comando(String linea, int... codigos) throws IOException {
            String descripcion = linea.startsWith("RCPT") || linea.startsWith("MAIL") ? linea : linea.split(" ")[0];
            return comando(linea, descripcion, codigos);
        }

        String comando(String linea, String descripcion, int... codigos) throws IOException {
            salida.write(linea + "\r\n");
            salida.flush();
            return esperar(descripcion, codigos);
        }

        /** Lee una respuesta (posiblemente de varias líneas "250-...") y verifica su código. */
        String esperar(String descripcion, int... codigos) throws IOException {
            String linea;
            StringBuilder respuesta = new StringBuilder();
            do {
                linea = entrada.readLine();
                if (linea == null) {
                    throw new IOException("SMTP: el servidor cerró la conexión (" + descripcion + ")");
                }
                respuesta.append(linea).append(' ');
            } while (linea.length() > 3 && linea.charAt(3) == '-');

            int codigo = Integer.parseInt(linea.substring(0, 3));
            for (int esperado : codigos) {
                if (codigo == esperado) {
                    return respuesta.toString();
                }
            }
            throw new IOException("SMTP: falló " + descripcion + " → " + respuesta.toString().trim());
        }

        @Override
        public void close() throws IOException {
            socket.close();
        }
    }
}
