package bo.uagrm.grupo08sa;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Lee config.properties desde la carpeta donde se ejecuta el programa.
 */
public final class Config {

    private static final Properties props = new Properties();

    static {
        Path archivo = Path.of("config.properties");
        if (!Files.exists(archivo)) {
            throw new IllegalStateException(
                    "No se encontró config.properties. Copie config.example.properties y complételo.");
        }
        try (Reader r = new InputStreamReader(Files.newInputStream(archivo), StandardCharsets.UTF_8)) {
            props.load(r);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer config.properties", e);
        }
    }

    private Config() {
    }

    public static String get(String clave) {
        String valor = props.getProperty(clave);
        if (valor == null) {
            throw new IllegalStateException("Falta la clave '" + clave + "' en config.properties");
        }
        return valor.trim();
    }

    public static int getInt(String clave) {
        return Integer.parseInt(get(clave));
    }
}
