package bo.uagrm.grupo08sa.negocio.controladores;

import bo.uagrm.grupo08sa.datos.Fila;
import bo.uagrm.grupo08sa.datos.dao.ReporteDAO;
import bo.uagrm.grupo08sa.datos.dao.SemestreDAO;
import bo.uagrm.grupo08sa.datos.modelo.Usuario;
import bo.uagrm.grupo08sa.negocio.Busquedas;
import bo.uagrm.grupo08sa.negocio.Comando;
import bo.uagrm.grupo08sa.negocio.Formato;
import bo.uagrm.grupo08sa.negocio.Manejador;
import bo.uagrm.grupo08sa.negocio.NegocioException;
import bo.uagrm.grupo08sa.negocio.Resultado;
import bo.uagrm.grupo08sa.negocio.Validar;

import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * CU8. Reportes y Estadísticas: REPOCU, REPRES, REPINC, REPASI, ESTSEM.
 * Todos reciben un rango de fechas (salvo ESTSEM, que usa las fechas del semestre) y calculan totales y porcentajes.
 */
public class ReporteController {

    /** Horas hábiles por día (07:00 a 22:00), de lunes a sábado, para calcular el % de ocupación. */
    public static final int HORAS_POR_DIA = 15;
    private static final int MAX_DIAS = 366;
    private static final String SINTAXIS_RANGO = "[\"desde\",\"hasta\"] con fechas AAAA-MM-DD";

    private final ReporteDAO reporteDAO = new ReporteDAO();
    private final SemestreDAO semestreDAO = new SemestreDAO();
    private final Busquedas buscar = new Busquedas();

    public void registrarEn(Map<String, Manejador> manejadores) {
        manejadores.put("REPOCU", this::ocupacion);
        manejadores.put("REPRES", this::reservas);
        manejadores.put("REPINC", this::incidencias);
        manejadores.put("REPASI", this::asistencia);
        manejadores.put("ESTSEM", this::estadisticasSemestre);
    }

    // REPOCU["2026-08-03","2026-10-31"]
    public Resultado ocupacion(Comando c, Usuario u) throws NegocioException, SQLException {
        LocalDate[] r = rango(c);
        Set<String> bloqueos = new HashSet<>();
        for (Fila b : reporteDAO.enRangoBloqueos(r[0], r[1])) {
            bloqueos.add(b.texto("fecha") + "|" + (b.enteroONulo("laboratorio_id") == null ? "*" : b.texto("laboratorio_id")));
        }
        Map<Integer, double[]> horas = new LinkedHashMap<>();     // [clase, reserva]
        List<Fila> labs = reporteDAO.laboratorios();
        for (Fila lab : labs) {
            horas.put(lab.entero("id"), new double[2]);
        }
        for (Fila clase : reporteDAO.clasesEnRango(r[0], r[1])) {
            LocalDate desde = max(r[0], clase.fecha("fecha_inicio"));
            LocalDate hasta = min(r[1], clase.fecha("fecha_fin"));
            double duracion = Formato.horas(clase.hora("hora_inicio"), clase.hora("hora_fin"));
            int labId = clase.entero("laboratorio_id");
            for (LocalDate d = desde; !d.isAfter(hasta); d = d.plusDays(1)) {
                if (d.getDayOfWeek().getValue() == clase.entero("dia_semana") && !bloqueado(bloqueos, d, labId)) {
                    horas.get(labId)[0] += duracion;
                }
            }
        }
        for (Fila res : reporteDAO.reservasEnRango(r[0], r[1])) {
            if ("APROBADA".equals(res.texto("estado"))) {
                horas.get(res.entero("laboratorio_id"))[1] += Formato.horas(res.hora("hora_inicio"), res.hora("hora_fin"));
            }
        }

        List<String[]> filas = new ArrayList<>();
        double totClase = 0, totReserva = 0, totDisponible = 0;
        for (Fila lab : labs) {
            int labId = lab.entero("id");
            double disponible = 0;
            for (LocalDate d = r[0]; !d.isAfter(r[1]); d = d.plusDays(1)) {
                if (d.getDayOfWeek().getValue() <= 6 && !bloqueado(bloqueos, d, labId)) {
                    disponible += HORAS_POR_DIA;
                }
            }
            double[] h = horas.get(labId);
            totClase += h[0];
            totReserva += h[1];
            totDisponible += disponible;
            filas.add(new String[]{lab.texto("codigo"), lab.texto("nombre"), Formato.decimal(h[0]), Formato.decimal(h[1]),
                    Formato.decimal(h[0] + h[1]), Formato.decimal(disponible), Formato.porcentaje(h[0] + h[1], disponible)});
        }
        filas.add(new String[]{"TOTAL", "", Formato.decimal(totClase), Formato.decimal(totReserva),
                Formato.decimal(totClase + totReserva), Formato.decimal(totDisponible),
                Formato.porcentaje(totClase + totReserva, totDisponible)});
        return Resultado.tabla("Reporte de ocupación", periodo(r) + " Horas disponibles: lunes a sábado, "
                        + HORAS_POR_DIA + " h por día, sin fechas bloqueadas.",
                new String[]{"Lab", "Nombre", "Horas clase", "Horas reserva", "Horas ocupadas", "Horas disponibles", "% Ocupación"},
                filas);
    }

    // REPRES["2026-08-03","2026-10-31"]
    public Resultado reservas(Comando c, Usuario u) throws NegocioException, SQLException {
        LocalDate[] r = rango(c);
        List<Fila> reservas = reporteDAO.reservasEnRango(r[0], r[1]);
        Map<String, Integer> porEstado = contar(reservas, "estado", "PENDIENTE", "APROBADA", "RECHAZADA", "CANCELADA");
        Map<String, Integer> porTipo = contar(reservas, "tipo", "EXAMEN", "RECUPERACION", "PRACTICA", "EVENTO");
        Map<String, double[]> porLab = new TreeMap<>();               // [solicitudes, aprobadas, horas aprobadas]
        for (Fila res : reservas) {
            double[] v = porLab.computeIfAbsent(res.texto("lab"), k -> new double[3]);
            v[0]++;
            if ("APROBADA".equals(res.texto("estado"))) {
                v[1]++;
                v[2] += Formato.horas(res.hora("hora_inicio"), res.hora("hora_fin"));
            }
        }
        List<String[]> filasLab = new ArrayList<>();
        for (Map.Entry<String, double[]> e : porLab.entrySet()) {
            double[] v = e.getValue();
            filasLab.add(new String[]{e.getKey(), String.valueOf((int) v[0]), String.valueOf((int) v[1]),
                    Formato.porcentaje(v[1], v[0]), Formato.decimal(v[2])});
        }
        return Resultado.exito("Reporte de reservas", periodo(r) + " Total de solicitudes: " + reservas.size() + ".")
                .agregarTabla("Por estado", new String[]{"Estado", "Cantidad", "%"}, filasConteo(porEstado, reservas.size()))
                .agregarTabla("Por tipo", new String[]{"Tipo", "Cantidad", "%"}, filasConteo(porTipo, reservas.size()))
                .agregarTabla("Por laboratorio", new String[]{"Lab", "Solicitudes", "Aprobadas", "% Aprobación", "Horas aprobadas"},
                        filasLab);
    }

    // REPINC["2026-08-03","2026-10-31"]
    public Resultado incidencias(Comando c, Usuario u) throws NegocioException, SQLException {
        LocalDate[] r = rango(c);
        List<Fila> incidencias = reporteDAO.incidenciasEnRango(r[0], r[1]);
        Map<String, Integer> porEstado = contar(incidencias, "estado", "PENDIENTE", "EN_ATENCION", "RESUELTA");
        Map<String, double[]> porLab = new TreeMap<>();               // [reportadas, resueltas, horas de resolución]
        for (Fila i : incidencias) {
            double[] v = porLab.computeIfAbsent(i.texto("lab"), k -> new double[3]);
            v[0]++;
            if (i.fechaHora("fecha_solucion") != null) {
                v[1]++;
                v[2] += Duration.between(i.fechaHora("fecha_reporte"), i.fechaHora("fecha_solucion")).toMinutes() / 60.0;
            }
        }
        List<String[]> filasLab = new ArrayList<>();
        for (Map.Entry<String, double[]> e : porLab.entrySet()) {
            double[] v = e.getValue();
            filasLab.add(new String[]{e.getKey(), String.valueOf((int) v[0]), String.valueOf((int) v[1]),
                    v[1] == 0 ? "-" : Formato.decimal(v[2] / v[1])});
        }
        List<Fila> objetos = reporteDAO.objetosEnRango(r[0], r[1]);
        Map<String, Integer> porEstadoObj = contar(objetos, "estado", "EN_CUSTODIA", "ENTREGADO");
        return Resultado.exito("Reporte de incidencias y objetos encontrados",
                        periodo(r) + " Incidencias: " + incidencias.size() + ". Objetos encontrados: " + objetos.size() + ".")
                .agregarTabla("Incidencias por estado", new String[]{"Estado", "Cantidad", "%"},
                        filasConteo(porEstado, incidencias.size()))
                .agregarTabla("Incidencias por laboratorio",
                        new String[]{"Lab", "Reportadas", "Resueltas", "Prom. horas de solución"}, filasLab)
                .agregarTabla("Objetos encontrados", new String[]{"Estado", "Cantidad", "%"},
                        filasConteo(porEstadoObj, objetos.size()));
    }

    // REPASI["2026-08-03","2026-10-31"]
    public Resultado asistencia(Comando c, Usuario u) throws NegocioException, SQLException {
        LocalDate[] r = rango(c);
        Map<String, double[]> porAux = new TreeMap<>();   // [puntual, atraso, falta, permiso, horas]
        Map<String, String> nombres = new LinkedHashMap<>();
        List<Fila> registros = reporteDAO.asistenciaEnRango(r[0], r[1]);
        for (Fila a : registros) {
            String clave = a.texto("auxiliar_codigo");
            nombres.put(clave, a.texto("auxiliar"));
            double[] v = porAux.computeIfAbsent(clave, k -> new double[5]);
            switch (a.texto("estado")) {
                case "PUNTUAL" -> v[0]++;
                case "ATRASO" -> v[1]++;
                case "FALTA" -> v[2]++;
                default -> v[3]++;
            }
            if (a.hora("hora_entrada") != null && a.hora("hora_salida") != null) {
                v[4] += Formato.horas(a.hora("hora_entrada"), a.hora("hora_salida"));
            }
        }
        List<String[]> filas = new ArrayList<>();
        for (Map.Entry<String, double[]> e : porAux.entrySet()) {
            double[] v = e.getValue();
            double total = v[0] + v[1] + v[2] + v[3];
            filas.add(new String[]{e.getKey(), nombres.get(e.getKey()), String.valueOf((int) total), String.valueOf((int) v[0]),
                    String.valueOf((int) v[1]), String.valueOf((int) v[2]), String.valueOf((int) v[3]),
                    Formato.porcentaje(v[0], total), Formato.decimal(v[4])});
        }
        return Resultado.tabla("Reporte de asistencia de auxiliares", periodo(r) + " Registros: " + registros.size() + ".",
                new String[]{"Código", "Auxiliar", "Turnos", "Puntual", "Atraso", "Falta", "Permiso", "% Puntualidad", "Horas trabajadas"},
                filas);
    }

    // ESTSEM["*"] (semestre activo)  ESTSEM["2-2026"]
    public Resultado estadisticasSemestre(Comando c, Usuario u) throws NegocioException, SQLException {
        c.exigirEntre(0, 1, "ESTSEM[\"*\"] o ESTSEM[\"gestion\"]");
        Fila sem = c.cantidad() == 1 ? semestreDAO.buscarPorGestion(c.param(0)) : buscar.semestreActivo();
        if (sem == null) {
            throw new NegocioException("No existe el semestre '" + c.param(0) + "'.");
        }
        int semId = sem.entero("id");
        LocalDate desde = sem.fecha("fecha_inicio");
        LocalDate hasta = sem.fecha("fecha_fin");

        List<Fila> labs = reporteDAO.laboratorios();
        Map<String, Integer> labsPorEstado = contar(labs, "estado", "DISPONIBLE", "MANTENIMIENTO", "INHABILITADO");
        List<String[]> general = new ArrayList<>();
        general.add(new String[]{"Gestión", sem.texto("gestion") + " (" + sem.texto("estado") + ")"});
        general.add(new String[]{"Período", desde + " a " + hasta});
        general.add(new String[]{"Laboratorios", labs.size() + " (" + labsPorEstado.get("DISPONIBLE") + " disponibles)"});
        general.add(new String[]{"Clases en el horario", String.valueOf(reporteDAO.clasesDelSemestre(semId))});
        general.add(new String[]{"Materias", String.valueOf(reporteDAO.materiasDelSemestre(semId))});
        general.add(new String[]{"Docentes con clase", String.valueOf(reporteDAO.docentesConClase(semId))});
        general.add(new String[]{"Fechas bloqueadas", String.valueOf(reporteDAO.fechasBloqueadas(semId))});
        general.add(new String[]{"Mensajes internos", String.valueOf(reporteDAO.mensajesEnRango(desde, hasta))});

        List<Fila> reservas = reporteDAO.reservasEnRango(desde, hasta);
        List<Fila> permutas = reporteDAO.permutasEnRango(desde, hasta);
        List<Fila> incidencias = reporteDAO.incidenciasEnRango(desde, hasta);
        List<Fila> equipos = reporteDAO.equipos();
        List<Fila> objetos = reporteDAO.objetosEnRango(desde, hasta);
        List<Fila> asistencia = reporteDAO.asistenciaEnRango(desde, hasta);
        return Resultado.exito("Estadísticas del semestre " + sem.texto("gestion"), "Resumen general del semestre.")
                .agregarTabla("General", new String[]{"Indicador", "Valor"}, general)
                .agregarTabla("Reservas (" + reservas.size() + ")", new String[]{"Estado", "Cantidad", "%"},
                        filasConteo(contar(reservas, "estado", "PENDIENTE", "APROBADA", "RECHAZADA", "CANCELADA"), reservas.size()))
                .agregarTabla("Permutas (" + permutas.size() + ")", new String[]{"Estado", "Cantidad", "%"},
                        filasConteo(contar(permutas, "estado", "PENDIENTE", "ACEPTADA", "APROBADA", "RECHAZADA"), permutas.size()))
                .agregarTabla("Incidencias (" + incidencias.size() + ")", new String[]{"Estado", "Cantidad", "%"},
                        filasConteo(contar(incidencias, "estado", "PENDIENTE", "EN_ATENCION", "RESUELTA"), incidencias.size()))
                .agregarTabla("Equipos (" + equipos.size() + ")", new String[]{"Estado", "Cantidad", "%"},
                        filasConteo(contar(equipos, "estado", "OPERATIVO", "EN_REPARACION", "BAJA"), equipos.size()))
                .agregarTabla("Objetos encontrados (" + objetos.size() + ")", new String[]{"Estado", "Cantidad", "%"},
                        filasConteo(contar(objetos, "estado", "EN_CUSTODIA", "ENTREGADO"), objetos.size()))
                .agregarTabla("Asistencia de auxiliares (" + asistencia.size() + ")", new String[]{"Estado", "Cantidad", "%"},
                        filasConteo(contar(asistencia, "estado", "PUNTUAL", "ATRASO", "FALTA", "PERMISO"), asistencia.size()));
    }

    private LocalDate[] rango(Comando c) throws NegocioException {
        c.exigirParametros(2, c.getNombre() + SINTAXIS_RANGO);
        LocalDate desde = Validar.fecha(c.param(0), "fecha desde");
        LocalDate hasta = Validar.fecha(c.param(1), "fecha hasta");
        Validar.rangoFechas(desde, hasta);
        if (ChronoUnit.DAYS.between(desde, hasta) > MAX_DIAS) {
            throw new NegocioException("El rango no puede superar un año.");
        }
        return new LocalDate[]{desde, hasta};
    }

    private static String periodo(LocalDate[] r) {
        return "Período del " + r[0] + " al " + r[1] + ".";
    }

    /** Cuenta las filas por el valor de una columna, mostrando también los valores con 0. */
    private static Map<String, Integer> contar(List<Fila> filas, String columna, String... valores) {
        Map<String, Integer> conteo = new LinkedHashMap<>();
        for (String v : valores) {
            conteo.put(v, 0);
        }
        for (Fila f : filas) {
            conteo.merge(f.texto(columna), 1, Integer::sum);
        }
        return conteo;
    }

    private static List<String[]> filasConteo(Map<String, Integer> conteo, int total) {
        List<String[]> filas = new ArrayList<>();
        for (Map.Entry<String, Integer> e : conteo.entrySet()) {
            filas.add(new String[]{e.getKey(), String.valueOf(e.getValue()), Formato.porcentaje(e.getValue(), total)});
        }
        return filas;
    }

    private static boolean bloqueado(Set<String> bloqueos, LocalDate fecha, int labId) {
        return bloqueos.contains(fecha + "|*") || bloqueos.contains(fecha + "|" + labId);
    }

    private static LocalDate max(LocalDate a, LocalDate b) {
        return a.isAfter(b) ? a : b;
    }

    private static LocalDate min(LocalDate a, LocalDate b) {
        return a.isBefore(b) ? a : b;
    }
}
