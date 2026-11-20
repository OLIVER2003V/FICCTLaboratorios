package bo.uagrm.grupo08sa;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Registro local para la auditoría del sistema, en la carpeta logs/ (configurable con log.directorio):
 * <ul>
 *   <li><b>access.log</b>: cada correo recibido, cada comando ejecutado (con su resultado) y cada respuesta enviada.</li>
 *   <li><b>error.log</b>: cada excepción capturada, con su traza completa.</li>
 * </ul>
 * Todo se muestra además en consola (journalctl en el servidor). Un fallo al escribir el log nunca detiene el sistema.
 */
public final class Log {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final Path DIRECTORIO = directorio();

    private Log() {
    }

    /** Solo consola: mensajes de estado que no hacen falta para la auditoría. */
    public static void info(String mensaje) {
        System.out.println("[" + LocalDateTime.now().format(HORA) + "] " + mensaje);
    }

    /** Consola + access.log. Evento: RECIBIDO, COMANDO, RESPUESTA, IGNORADO, NOTIFICACION, INICIO… */
    public static void acceso(String evento, String detalle) {
        info(evento + " " + detalle);
        escribir("access.log", LocalDateTime.now().format(FECHA) + " " + String.format("%-12s", evento) + " " + detalle);
    }

    /** Consola (stderr) + error.log, sin traza. */
    public static void error(String mensaje) {
        error(mensaje, null);
    }

    /** Consola (stderr) + error.log, con la traza completa de la excepción si se indica. */
    public static void error(String mensaje, Throwable causa) {
        String linea = mensaje + (causa != null ? " → " + causa : "");
        System.err.println("[" + LocalDateTime.now().format(HORA) + "] ERROR " + linea);
        StringBuilder sb = new StringBuilder(LocalDateTime.now().format(FECHA)).append(" ERROR ").append(linea);
        if (causa != null) {
            StringWriter traza = new StringWriter();
            causa.printStackTrace(new PrintWriter(traza));
            sb.append(System.lineSeparator()).append(traza.toString().stripTrailing());
        }
        escribir("error.log", sb.toString());
    }

    /** Texto entre comillas y en una sola línea, para que cada evento ocupe una línea del log. */
    public static String cita(String texto) {
        if (texto == null) {
            return "\"\"";
        }
        return "\"" + texto.replace("\r", " ").replace("\n", " ").replace("\"", "\\\"") + "\"";
    }

    private static synchronized void escribir(String archivo, String linea) {
        try {
            Files.createDirectories(DIRECTORIO);
            Files.writeString(DIRECTORIO.resolve(archivo), linea + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("[LOG] No se pudo escribir en " + DIRECTORIO.resolve(archivo) + ": " + e.getMessage());
        }
    }

    private static Path directorio() {
        try {
            return Path.of(Config.get("log.directorio", "logs"));
        } catch (RuntimeException | LinkageError e) {  // LinkageError: Config no pudo inicializarse
            return Path.of("logs");  // sin config.properties también se puede registrar
        }
    }
}
