package bo.uagrm.grupo08sa.negocio;

import java.util.List;

/**
 * Solicitud ya interpretada: COMANDO["p1","p2",...] enviada por un remitente.
 * COMANDO["*"] llega con la lista de parámetros vacía.
 */
public class Comando {

    private final String nombre;
    private final List<String> parametros;
    private final String remitente;
    private final String asunto;
    private final Adjunto adjunto;

    public Comando(String nombre, List<String> parametros, String remitente, String asunto) {
        this(nombre, parametros, remitente, asunto, null);
    }

    public Comando(String nombre, List<String> parametros, String remitente, String asunto, Adjunto adjunto) {
        this.nombre = nombre;
        this.parametros = List.copyOf(parametros);
        this.remitente = remitente;
        this.asunto = asunto;
        this.adjunto = adjunto;
    }

    public String getNombre() { return nombre; }
    public List<String> getParametros() { return parametros; }
    public String getRemitente() { return remitente; }
    public String getAsunto() { return asunto; }
    public Adjunto getAdjunto() { return adjunto; }

    public int cantidad() {
        return parametros.size();
    }

    public String param(int indice) {
        return parametros.get(indice).trim();
    }

    /** Parámetro opcional: "" si no se envió. */
    public String paramOpcional(int indice) {
        return indice < parametros.size() ? param(indice) : "";
    }

    /** Lanza error si la cantidad de parámetros no coincide, mostrando la sintaxis correcta. */
    public void exigirParametros(int cantidad, String sintaxis) throws NegocioException {
        if (parametros.size() != cantidad) {
            throw new NegocioException("El comando " + nombre + " espera " + cantidad
                    + " parámetro(s) y recibió " + parametros.size() + ". Sintaxis: " + sintaxis);
        }
    }

    /** Igual que exigirParametros pero acepta un rango [minimo, maximo]. */
    public void exigirEntre(int minimo, int maximo, String sintaxis) throws NegocioException {
        if (parametros.size() < minimo || parametros.size() > maximo) {
            throw new NegocioException("Cantidad de parámetros incorrecta para " + nombre + " (recibió "
                    + parametros.size() + "). Sintaxis: " + sintaxis);
        }
    }
}
