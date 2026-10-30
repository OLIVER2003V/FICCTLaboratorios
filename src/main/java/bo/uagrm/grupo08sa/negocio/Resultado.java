package bo.uagrm.grupo08sa.negocio;

import java.util.ArrayList;
import java.util.List;

/**
 * Respuesta de un controlador, independiente del formato. La capa de presentación la convierte a HTML.
 * Puede llevar varias tablas (los reportes muestran varias secciones).
 */
public class Resultado {

    /** subtitulo puede ser null cuando hay una sola tabla. */
    public record Tabla(String subtitulo, String[] columnas, List<String[]> filas) {
    }

    private final boolean exito;
    private final String titulo;
    private final String mensaje;
    private final List<Tabla> tablas = new ArrayList<>();

    private Resultado(boolean exito, String titulo, String mensaje) {
        this.exito = exito;
        this.titulo = titulo;
        this.mensaje = mensaje;
    }

    public static Resultado exito(String titulo, String mensaje) {
        return new Resultado(true, titulo, mensaje);
    }

    public static Resultado tabla(String titulo, String mensaje, String[] columnas, List<String[]> filas) {
        return exito(titulo, mensaje).agregarTabla(null, columnas, filas);
    }

    public static Resultado error(String mensaje) {
        return new Resultado(false, "Error", mensaje);
    }

    public Resultado agregarTabla(String subtitulo, String[] columnas, List<String[]> filas) {
        tablas.add(new Tabla(subtitulo, columnas, filas));
        return this;
    }

    public boolean isExito() { return exito; }
    public String getTitulo() { return titulo; }
    public String getMensaje() { return mensaje; }
    public List<Tabla> getTablas() { return tablas; }

    public boolean tieneTabla() {
        return !tablas.isEmpty();
    }
}
