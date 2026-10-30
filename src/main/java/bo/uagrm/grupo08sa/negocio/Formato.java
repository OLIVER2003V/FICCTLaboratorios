package bo.uagrm.grupo08sa.negocio;

import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Formatos de texto comunes para las tablas de respuesta.
 */
public final class Formato {

    private static final String[] DIAS = {"", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"};
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private Formato() {
    }

    public static String dia(int dia) {
        return dia >= 1 && dia <= 7 ? DIAS[dia] : String.valueOf(dia);
    }

    public static String hora(LocalTime hora) {
        return hora == null ? "" : hora.format(HORA);
    }

    public static String franja(LocalTime inicio, LocalTime fin) {
        return hora(inicio) + "-" + hora(fin);
    }

    public static double horas(LocalTime inicio, LocalTime fin) {
        return Duration.between(inicio, fin).toMinutes() / 60.0;
    }

    public static String decimal(double valor) {
        return String.format(Locale.US, "%.1f", valor);
    }

    public static String porcentaje(double parte, double total) {
        return total == 0 ? "0.0%" : String.format(Locale.US, "%.1f%%", parte * 100.0 / total);
    }
}
