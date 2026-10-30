package bo.uagrm.grupo08sa.negocio;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Locale;

/**
 * Convierte y valida los parámetros que llegan como texto en el asunto del correo.
 * Cada error lleva un mensaje que se envía tal cual al usuario.
 */
public final class Validar {

    private static final String[] DIAS = {"LUNES", "MARTES", "MIERCOLES", "JUEVES", "VIERNES", "SABADO"};

    private Validar() {
    }

    /** Fecha en formato AAAA-MM-DD. */
    public static LocalDate fecha(String valor, String campo) throws NegocioException {
        try {
            return LocalDate.parse(valor.trim());
        } catch (DateTimeParseException e) {
            throw new NegocioException("La " + campo + " '" + valor + "' no es válida. Use el formato AAAA-MM-DD, "
                    + "por ejemplo 2026-11-03.");
        }
    }

    /** Hora en formato HH:MM (también acepta H:MM). */
    public static LocalTime hora(String valor, String campo) throws NegocioException {
        String v = valor.trim();
        if (v.matches("\\d:\\d{2}")) {
            v = "0" + v;
        }
        try {
            return LocalTime.parse(v);
        } catch (DateTimeParseException e) {
            throw new NegocioException("La " + campo + " '" + valor + "' no es válida. Use el formato HH:MM, por ejemplo 08:00.");
        }
    }

    public static int entero(String valor, String campo, int minimo, int maximo) throws NegocioException {
        try {
            int n = Integer.parseInt(valor.trim());
            if (n < minimo || n > maximo) {
                throw new NegocioException("El campo " + campo + " debe estar entre " + minimo + " y " + maximo + ".");
            }
            return n;
        } catch (NumberFormatException e) {
            throw new NegocioException("El campo " + campo + " debe ser un número entero ('" + valor + "' no lo es).");
        }
    }

    public static int id(String valor, String que) throws NegocioException {
        try {
            return Integer.parseInt(valor.trim());
        } catch (NumberFormatException e) {
            throw new NegocioException("El ID de " + que + " debe ser un número ('" + valor + "' no lo es).");
        }
    }

    /** Día de la semana: 1-6 o LUNES..SABADO (con o sin tilde). Devuelve 1 = lunes ... 6 = sábado. */
    public static int dia(String valor) throws NegocioException {
        String v = sinTildes(valor.trim()).toUpperCase(Locale.ROOT);
        if (v.matches("[1-6]")) {
            return Integer.parseInt(v);
        }
        for (int i = 0; i < DIAS.length; i++) {
            if (DIAS[i].equals(v)) {
                return i + 1;
            }
        }
        throw new NegocioException("El día '" + valor + "' no es válido. Use LUNES, MARTES, MIERCOLES, JUEVES, "
                + "VIERNES, SABADO o un número del 1 al 6.");
    }

    public static boolean esDia(String valor) {
        String v = sinTildes(valor.trim()).toUpperCase(Locale.ROOT);
        return v.matches("[1-6]") || Arrays.asList(DIAS).contains(v);
    }

    public static boolean esFecha(String valor) {
        try {
            LocalDate.parse(valor.trim());
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    /** Valor de una lista cerrada (sin distinguir mayúsculas ni tildes). Devuelve el valor normalizado. */
    public static String opcion(String valor, String campo, String... permitidas) throws NegocioException {
        String v = sinTildes(valor.trim()).toUpperCase(Locale.ROOT).replace(' ', '_');
        for (String p : permitidas) {
            if (p.equals(v)) {
                return p;
            }
        }
        throw new NegocioException("El " + campo + " '" + valor + "' no es válido. Valores permitidos: "
                + String.join(", ", permitidas) + ".");
    }

    public static boolean esOpcion(String valor, String... permitidas) {
        String v = sinTildes(valor.trim()).toUpperCase(Locale.ROOT).replace(' ', '_');
        return Arrays.asList(permitidas).contains(v);
    }

    /** Texto obligatorio con longitud máxima. */
    public static String texto(String valor, String campo, int maximo) throws NegocioException {
        String v = valor.trim();
        if (v.isEmpty()) {
            throw new NegocioException("El campo " + campo + " es obligatorio.");
        }
        if (v.length() > maximo) {
            throw new NegocioException("El campo " + campo + " admite como máximo " + maximo + " caracteres.");
        }
        return v;
    }

    /** Texto opcional: devuelve null si está vacío. */
    public static String textoOpcional(String valor, String campo, int maximo) throws NegocioException {
        return valor == null || valor.trim().isEmpty() ? null : texto(valor, campo, maximo);
    }

    public static void rangoHoras(LocalTime inicio, LocalTime fin) throws NegocioException {
        if (!fin.isAfter(inicio)) {
            throw new NegocioException("La hora de fin (" + Formato.hora(fin) + ") debe ser posterior a la de inicio ("
                    + Formato.hora(inicio) + ").");
        }
    }

    public static void rangoFechas(LocalDate desde, LocalDate hasta) throws NegocioException {
        if (hasta.isBefore(desde)) {
            throw new NegocioException("La fecha final (" + hasta + ") no puede ser anterior a la inicial (" + desde + ").");
        }
    }

    public static String sinTildes(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }
}
