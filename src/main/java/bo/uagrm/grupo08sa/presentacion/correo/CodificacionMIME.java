package bo.uagrm.grupo08sa.presentacion.correo;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Codificación de encabezados con tildes (RFC 2047): =?UTF-8?B?...?= y =?UTF-8?Q?...?=
 */
public final class CodificacionMIME {

    private static final Pattern PALABRA_CODIFICADA =
            Pattern.compile("=\\?([^?]+)\\?([BbQq])\\?([^?]*)\\?=");
    // Entre dos palabras codificadas seguidas, el espacio se descarta
    private static final Pattern ESPACIO_ENTRE_PALABRAS =
            Pattern.compile("(\\?=)\\s+(=\\?)");

    private CodificacionMIME() {
    }

    public static String decodificarEncabezado(String valor) {
        if (valor == null) {
            return "";
        }
        String texto = ESPACIO_ENTRE_PALABRAS.matcher(valor).replaceAll("$1$2");
        Matcher m = PALABRA_CODIFICADA.matcher(texto);
        StringBuilder sb = new StringBuilder();
        int ultimo = 0;
        while (m.find()) {
            sb.append(bytesCrudos(texto.substring(ultimo, m.start())));
            sb.append(decodificarPalabra(m.group(1), m.group(2), m.group(3)));
            ultimo = m.end();
        }
        sb.append(bytesCrudos(texto.substring(ultimo)));
        return sb.toString();
    }

    public static String codificarEncabezado(String valor) {
        boolean soloAscii = valor.chars().allMatch(ch -> ch >= 32 && ch < 127);
        if (soloAscii) {
            return valor;
        }
        // Trozos cortos para que cada línea del encabezado no supere los 76 caracteres
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < valor.length()) {
            int fin = Math.min(valor.length(), i + 30);
            if (fin < valor.length() && Character.isHighSurrogate(valor.charAt(fin - 1))) {
                fin--;
            }
            if (sb.length() > 0) {
                sb.append("\r\n ");
            }
            sb.append("=?UTF-8?B?")
              .append(Base64.getEncoder().encodeToString(valor.substring(i, fin).getBytes(StandardCharsets.UTF_8)))
              .append("?=");
            i = fin;
        }
        return sb.toString();
    }

    private static String decodificarPalabra(String juego, String tipo, String datos) {
        try {
            Charset charset = Charset.forName(juego);
            byte[] bytes = tipo.equalsIgnoreCase("B")
                    ? Base64.getMimeDecoder().decode(datos)
                    : decodificarQ(datos);
            return new String(bytes, charset);
        } catch (IllegalArgumentException e) {
            return datos;
        }
    }

    private static byte[] decodificarQ(String datos) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int i = 0; i < datos.length(); i++) {
            char ch = datos.charAt(i);
            if (ch == '_') {
                out.write(' ');
            } else if (ch == '=' && i + 2 < datos.length()) {
                out.write(Integer.parseInt(datos.substring(i + 1, i + 3), 16));
                i += 2;
            } else {
                out.write(ch);
            }
        }
        return out.toByteArray();
    }

    /**
     * El texto se leyó del socket como ISO-8859-1 (un char por byte). Si esos bytes forman UTF-8 válido
     * (algunos clientes envían el asunto sin codificar), se reinterpretan como UTF-8.
     */
    private static String bytesCrudos(String texto) {
        byte[] bytes = texto.getBytes(StandardCharsets.ISO_8859_1);
        CharsetDecoder utf8 = StandardCharsets.UTF_8.newDecoder();
        try {
            return utf8.decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException e) {
            return texto;
        }
    }
}
