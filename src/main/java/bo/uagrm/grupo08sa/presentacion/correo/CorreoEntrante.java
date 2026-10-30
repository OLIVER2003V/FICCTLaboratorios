package bo.uagrm.grupo08sa.presentacion.correo;

import bo.uagrm.grupo08sa.negocio.Adjunto;

import java.io.ByteArrayOutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Correo leído por POP3: se extraen el remitente, el asunto y los archivos adjuntos (MIME multipart).
 */
public class CorreoEntrante {

    private static final Pattern CORREO_ENTRE_ANGULOS = Pattern.compile("<([^>]+)>");
    private static final Pattern CORREO_SUELTO = Pattern.compile("[\\w.+-]+@[\\w.-]+");
    private static final Pattern BOUNDARY = Pattern.compile("boundary=\"?([^\";]+)\"?", Pattern.CASE_INSENSITIVE);
    private static final Pattern NOMBRE_RFC2231 = Pattern.compile("filename\\*=([^;]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern NOMBRE = Pattern.compile("(?:file)?name=\"?([^\";]+)\"?", Pattern.CASE_INSENSITIVE);

    private final String remitente;
    private final String asunto;
    private final List<Adjunto> adjuntos;

    private CorreoEntrante(String remitente, String asunto, List<Adjunto> adjuntos) {
        this.remitente = remitente;
        this.asunto = asunto;
        this.adjuntos = adjuntos;
    }

    /** El texto llega como ISO-8859-1 (un carácter por byte), así el contenido binario se conserva. */
    public static CorreoEntrante desde(String crudo) {
        String[] partes = separar(crudo);
        Map<String, String> encabezados = encabezados(partes[0]);
        List<Adjunto> adjuntos = new ArrayList<>();
        buscarAdjuntos(encabezados, partes[1], adjuntos, 0);
        return new CorreoEntrante(extraerDireccion(encabezados.getOrDefault("from", "")),
                CodificacionMIME.decodificarEncabezado(encabezados.getOrDefault("subject", "")).trim(), adjuntos);
    }

    /** Recorre las partes multipart (pueden estar anidadas) y guarda las que tienen nombre de archivo. */
    private static void buscarAdjuntos(Map<String, String> encabezados, String cuerpo, List<Adjunto> adjuntos, int nivel) {
        String tipo = encabezados.getOrDefault("content-type", "text/plain");
        if (tipo.toLowerCase(Locale.ROOT).startsWith("multipart/") && nivel < 5) {
            Matcher b = BOUNDARY.matcher(tipo);
            if (!b.find()) {
                return;
            }
            String separador = "--" + b.group(1);
            for (String parte : cuerpo.split(Pattern.quote(separador))) {
                if (parte.startsWith("--") || parte.isBlank()) {
                    continue;   // cierre "--boundary--" o preámbulo vacío
                }
                String[] p = separar(parte.replaceFirst("^\r\n", ""));
                buscarAdjuntos(encabezados(p[0]), p[1], adjuntos, nivel + 1);
            }
            return;
        }
        String nombre = nombreArchivo(encabezados.getOrDefault("content-disposition", ""));
        if (nombre == null) {
            nombre = nombreArchivo(tipo);
        }
        if (nombre != null) {
            adjuntos.add(new Adjunto(nombre, decodificar(cuerpo, encabezados.getOrDefault("content-transfer-encoding", ""))));
        }
    }

    private static String nombreArchivo(String encabezado) {
        Matcher m = NOMBRE_RFC2231.matcher(encabezado);          // filename*=UTF-8''horario%20final.xlsx
        if (m.find()) {
            String valor = m.group(1).trim().replace("\"", "");
            int i = valor.indexOf("''");
            return URLDecoder.decode(i >= 0 ? valor.substring(i + 2) : valor, StandardCharsets.UTF_8);
        }
        m = NOMBRE.matcher(encabezado);
        return m.find() ? CodificacionMIME.decodificarEncabezado(m.group(1).trim()) : null;
    }

    private static byte[] decodificar(String cuerpo, String codificacion) {
        String c = codificacion.trim().toLowerCase(Locale.ROOT);
        if (c.equals("base64")) {
            return Base64.getMimeDecoder().decode(cuerpo.replaceAll("[^A-Za-z0-9+/=]", ""));
        }
        if (c.equals("quoted-printable")) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            String texto = cuerpo.replace("=\r\n", "");
            for (int i = 0; i < texto.length(); i++) {
                char ch = texto.charAt(i);
                if (ch == '=' && i + 2 < texto.length()) {
                    out.write(Integer.parseInt(texto.substring(i + 1, i + 3), 16));
                    i += 2;
                } else {
                    out.write(ch);
                }
            }
            return out.toByteArray();
        }
        return cuerpo.getBytes(StandardCharsets.ISO_8859_1);
    }

    /** [encabezados, cuerpo] separados por la primera línea vacía. */
    private static String[] separar(String texto) {
        int fin = texto.indexOf("\r\n\r\n");
        return fin >= 0 ? new String[]{texto.substring(0, fin), texto.substring(fin + 4)} : new String[]{texto, ""};
    }

    /** Encabezados en minúsculas; las líneas que empiezan con espacio continúan el anterior ("unfolding"). */
    private static Map<String, String> encabezados(String bloque) {
        Map<String, String> mapa = new HashMap<>();
        for (String linea : bloque.replaceAll("\r\n[ \t]+", " ").split("\r\n")) {
            int dosPuntos = linea.indexOf(':');
            if (dosPuntos > 0) {
                mapa.putIfAbsent(linea.substring(0, dosPuntos).trim().toLowerCase(Locale.ROOT),
                        linea.substring(dosPuntos + 1).trim());
            }
        }
        return mapa;
    }

    private static String extraerDireccion(String from) {
        Matcher m = CORREO_ENTRE_ANGULOS.matcher(from);
        if (m.find()) {
            return m.group(1).trim().toLowerCase(Locale.ROOT);
        }
        m = CORREO_SUELTO.matcher(from);
        return m.find() ? m.group().toLowerCase(Locale.ROOT) : "";
    }

    public String getRemitente() { return remitente; }
    public String getAsunto() { return asunto; }
    public List<Adjunto> getAdjuntos() { return adjuntos; }
}
