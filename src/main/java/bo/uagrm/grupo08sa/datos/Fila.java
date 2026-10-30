package bo.uagrm.grupo08sa.datos;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;

/**
 * Una fila de resultado (columna → valor). La usan los DAO que no necesitan una clase de modelo propia.
 */
public class Fila extends LinkedHashMap<String, Object> {

    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    /** Valor como texto para mostrar: fechas yyyy-MM-dd, horas HH:mm, null como "". */
    public String texto(String columna) {
        Object v = get(columna);
        if (v == null) {
            return "";
        }
        if (v instanceof Time t) {
            return t.toLocalTime().format(HORA);
        }
        if (v instanceof Date d) {
            return d.toLocalDate().toString();
        }
        if (v instanceof Timestamp ts) {
            return ts.toLocalDateTime().format(FECHA_HORA);
        }
        return v.toString();
    }

    public int entero(String columna) {
        return ((Number) get(columna)).intValue();
    }

    public Integer enteroONulo(String columna) {
        Object v = get(columna);
        return v == null ? null : ((Number) v).intValue();
    }

    public LocalDate fecha(String columna) {
        Object v = get(columna);
        return v == null ? null : ((Date) v).toLocalDate();
    }

    public LocalTime hora(String columna) {
        Object v = get(columna);
        return v == null ? null : ((Time) v).toLocalTime();
    }

    public LocalDateTime fechaHora(String columna) {
        Object v = get(columna);
        return v == null ? null : ((Timestamp) v).toLocalDateTime();
    }
}
