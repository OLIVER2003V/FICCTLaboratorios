package bo.uagrm.grupo08sa.presentacion.correo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Cliente POP3 (RFC 1939) implementado con sockets.
 * Diálogo: USER, PASS, STAT, RETR n, DELE n, QUIT. Los DELE se aplican recién al hacer QUIT.
 */
public class ClientePOP3 implements AutoCloseable {

    private final String servidor;
    private final int puerto;
    private final String usuario;
    private final String contrasena;

    private Socket socket;
    private BufferedReader entrada;
    private BufferedWriter salida;

    public ClientePOP3(String servidor, int puerto, String usuario, String contrasena) {
        this.servidor = servidor;
        this.puerto = puerto;
        this.usuario = usuario;
        this.contrasena = contrasena;
    }

    public void conectar() throws IOException {
        socket = new Socket();
        socket.connect(new InetSocketAddress(servidor, puerto), 15_000);
        socket.setSoTimeout(30_000);
        // ISO-8859-1 conserva cada byte tal cual; la decodificación real se hace al leer los encabezados
        entrada = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.ISO_8859_1));
        salida = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.ISO_8859_1));

        verificar(entrada.readLine(), "saludo del servidor");
        enviar("USER " + usuario, "USER");
        enviar("PASS " + contrasena, "PASS (revise usuario y contraseña)");
    }

    /** STAT → "+OK cantidad tamaño" */
    public int contarMensajes() throws IOException {
        String respuesta = enviar("STAT", "STAT");
        return Integer.parseInt(respuesta.split("\\s+")[1]);
    }

    /** RETR n → mensaje completo hasta la línea "." */
    public String leerMensaje(int numero) throws IOException {
        enviar("RETR " + numero, "RETR " + numero);
        StringBuilder mensaje = new StringBuilder();
        String linea;
        while ((linea = entrada.readLine()) != null) {
            if (linea.equals(".")) {
                return mensaje.toString();
            }
            // "dot-stuffing": las líneas que empiezan con '.' llegan con un punto extra
            if (linea.startsWith("..")) {
                linea = linea.substring(1);
            }
            mensaje.append(linea).append("\r\n");
        }
        throw new IOException("El servidor cerró la conexión durante RETR " + numero);
    }

    public void eliminar(int numero) throws IOException {
        enviar("DELE " + numero, "DELE " + numero);
    }

    @Override
    public void close() {
        if (socket == null) {
            return;
        }
        try {
            if (!socket.isClosed()) {
                enviar("QUIT", "QUIT");
            }
        } catch (IOException ignorada) {
            // Si falla el QUIT los DELE no se aplican y el mensaje se volverá a procesar
        } finally {
            try {
                socket.close();
            } catch (IOException ignorada) {
                // nada más que hacer
            }
        }
    }

    private String enviar(String comando, String descripcion) throws IOException {
        salida.write(comando + "\r\n");
        salida.flush();
        return verificar(entrada.readLine(), descripcion);
    }

    private static String verificar(String respuesta, String descripcion) throws IOException {
        if (respuesta == null) {
            throw new IOException("POP3: el servidor cerró la conexión (" + descripcion + ")");
        }
        if (!respuesta.startsWith("+OK")) {
            throw new IOException("POP3: falló " + descripcion + " → " + respuesta);
        }
        return respuesta;
    }
}
