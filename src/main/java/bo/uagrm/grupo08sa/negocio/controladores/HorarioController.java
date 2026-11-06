package bo.uagrm.grupo08sa.negocio.controladores;

import bo.uagrm.grupo08sa.datos.Fila;
import bo.uagrm.grupo08sa.datos.dao.FechaBloqueadaDAO;
import bo.uagrm.grupo08sa.datos.dao.HorarioDAO;
import bo.uagrm.grupo08sa.datos.dao.LaboratorioDAO;
import bo.uagrm.grupo08sa.datos.dao.MateriaDAO;
import bo.uagrm.grupo08sa.datos.dao.ReservaDAO;
import bo.uagrm.grupo08sa.datos.modelo.Usuario;
import bo.uagrm.grupo08sa.negocio.Busquedas;
import bo.uagrm.grupo08sa.negocio.Comando;
import bo.uagrm.grupo08sa.negocio.Disponibilidad;
import bo.uagrm.grupo08sa.negocio.Formato;
import bo.uagrm.grupo08sa.negocio.LectorHorario;
import bo.uagrm.grupo08sa.negocio.Manejador;
import bo.uagrm.grupo08sa.negocio.NegocioException;
import bo.uagrm.grupo08sa.negocio.Resultado;
import bo.uagrm.grupo08sa.negocio.Validar;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * CU3. Gestión de Carga Horaria Base y Consultar Disponibilidad:
 * INSHOR, CARHOR, MODHOR, ELIHOR, LISHOR, CONDIS.
 */
public class HorarioController {

    private static final String DATOS_CLASE = "\"laboratorio\",\"dia\",\"inicio\",\"fin\",\"sigla\",\"materia\",\"grupo\",\"docente\"";
    private static final String[] COLUMNAS =
            {"Sigla", "Grupo", "Materia", "Docente", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado"};

    private final HorarioDAO horarioDAO = new HorarioDAO();
    private final MateriaDAO materiaDAO = new MateriaDAO();
    private final LaboratorioDAO laboratorioDAO = new LaboratorioDAO();
    private final ReservaDAO reservaDAO = new ReservaDAO();
    private final FechaBloqueadaDAO fechaBloqueadaDAO = new FechaBloqueadaDAO();
    private final Busquedas buscar = new Busquedas();
    private final Disponibilidad disponibilidad = new Disponibilidad();

    /** Representa una fila consolidada por materia y grupo en la matriz semanal. */
    private static class FilaMatriz {
        final String sigla;
        final String grupo;
        final String materia;
        final String docente;
        final String[] dias = new String[6];

        FilaMatriz(String sigla, String grupo, String materia, String docente) {
            this.sigla = sigla;
            this.grupo = grupo;
            this.materia = materia;
            this.docente = docente;
            for (int i = 0; i < 6; i++) {
                this.dias[i] = "";
            }
        }

        void agregarSesion(int diaSemana, String horario) {
            int idx = diaSemana - 1;
            if (idx >= 0 && idx < 6) {
                if (dias[idx].isEmpty()) {
                    dias[idx] = horario;
                } else {
                    dias[idx] = dias[idx] + " / " + horario;
                }
            }
        }

        String[] aFila() {
            return new String[]{
                    sigla, grupo, materia, docente,
                    dias[0], dias[1], dias[2], dias[3], dias[4], dias[5]
            };
        }
    }

    /** Datos de una clase ya validados. */
    private record DatosClase(Fila lab, int dia, LocalTime inicio, LocalTime fin, int materiaId,
                              String grupo, Usuario docente) {
    }

    public void registrarEn(Map<String, Manejador> manejadores) {
        manejadores.put("INSHOR", this::registrarClase);
        manejadores.put("CARHOR", this::cargarHorario);
        manejadores.put("MODHOR", this::modificarClase);
        manejadores.put("ELIHOR", this::eliminarClase);
        manejadores.put("LISHOR", this::listarHorario);
        manejadores.put("CONDIS", this::consultarDisponibilidad);
    }

    // INSHOR["LAB-41","LUNES","07:00","09:15","INF513","Tecnología Web","SA","219001"]
    public Resultado registrarClase(Comando c, Usuario u) throws NegocioException, SQLException {
        c.exigirParametros(8, "INSHOR[" + DATOS_CLASE + "]");
        Fila sem = buscar.semestreActivo();
        DatosClase d = validar(c.getParametros().toArray(new String[0]), 0, sem.entero("id"), 0);
        int id = horarioDAO.insertar(sem.entero("id"), d.lab().entero("id"), d.materiaId(), d.docente().getId(),
                d.grupo(), d.dia(), d.inicio(), d.fin());
        return Resultado.tabla("Clase registrada", "La clase se agregó al horario de " + d.lab().texto("codigo")
                        + " en el semestre " + sem.texto("gestion") + ".",
                COLUMNAS, List.<String[]>of(filaMatriz(horarioDAO.buscarPorId(id), true)));
    }

    // CARHOR["*"] con el archivo .xlsx o .csv adjunto
    public Resultado cargarHorario(Comando c, Usuario u) throws NegocioException, SQLException {
        if (c.getAdjunto() == null) {
            throw new NegocioException("Adjunte el horario (.xlsx o .csv) al correo. Columnas: "
                    + "LAB, DIA, INICIO, FIN, SIGLA, MATERIA, GRUPO, DOCENTE.");
        }
        Fila sem = buscar.semestreActivo();
        List<String[]> filas = new ArrayList<>();
        int cargadas = 0;
        for (LectorHorario.FilaHorario f : LectorHorario.leer(c.getAdjunto())) {
            try {
                DatosClase d = validar(f.valores(), 0, sem.entero("id"), 0);
                int id = horarioDAO.insertar(sem.entero("id"), d.lab().entero("id"), d.materiaId(), d.docente().getId(),
                        d.grupo(), d.dia(), d.inicio(), d.fin());
                filas.add(new String[]{String.valueOf(f.numero()), "CARGADA", "Clase #" + id + " "
                        + f.valores()[4] + "-" + f.valores()[6] + " " + f.valores()[0] + " " + f.valores()[1]});
                cargadas++;
            } catch (NegocioException e) {
                filas.add(new String[]{String.valueOf(f.numero()), "RECHAZADA", e.getMessage()});
            }
        }
        return Resultado.tabla("Carga de horario", cargadas + " clase(s) cargada(s) y " + (filas.size() - cargadas)
                        + " fila(s) rechazada(s) del archivo " + c.getAdjunto().nombre() + ".",
                new String[]{"Fila", "Resultado", "Detalle"}, filas);
    }

    // MODHOR["15","LAB-41","LUNES","07:00","09:15","INF513","Tecnología Web","SA","219001"]
    public Resultado modificarClase(Comando c, Usuario u) throws NegocioException, SQLException {
        c.exigirParametros(9, "MODHOR[\"id\"," + DATOS_CLASE + "]");
        Fila sem = buscar.semestreActivo();
        Fila clase = buscarClase(c.param(0), sem);
        DatosClase d = validar(c.getParametros().toArray(new String[0]), 1, sem.entero("id"), clase.entero("id"));
        horarioDAO.actualizar(clase.entero("id"), d.lab().entero("id"), d.materiaId(), d.docente().getId(),
                d.grupo(), d.dia(), d.inicio(), d.fin());
        return Resultado.tabla("Clase modificada", "Se actualizó la clase en " + d.lab().texto("codigo") + ".",
                COLUMNAS, List.<String[]>of(filaMatriz(horarioDAO.buscarPorId(clase.entero("id")), true)));
    }

    // ELIHOR["15"]
    public Resultado eliminarClase(Comando c, Usuario u) throws NegocioException, SQLException {
        c.exigirParametros(1, "ELIHOR[\"id\"]");
        Fila clase = buscarClase(c.param(0), buscar.semestreActivo());
        horarioDAO.eliminar(clase.entero("id"));
        return Resultado.tabla("Clase eliminada", "Se quitó la clase del horario; ese horario queda libre.",
                COLUMNAS, List.<String[]>of(filaMatriz(clase, true)));
    }

    // LISHOR["*"]  LISHOR["LAB-41"]  LISHOR["LUNES"]  LISHOR["LAB-41","LUNES"]
    public Resultado listarHorario(Comando c, Usuario u) throws NegocioException, SQLException {
        c.exigirEntre(0, 2, "LISHOR[\"*\"], LISHOR[\"laboratorio\"], LISHOR[\"dia\"] o LISHOR[\"laboratorio\",\"dia\"]");
        Fila sem = buscar.semestreActivo();
        Integer labId = null;
        Integer dia = null;
        Fila labFila = null;
        for (String p : c.getParametros()) {
            if (p.trim().equals("*")) {
                continue;
            }
            if (Validar.esDia(p)) {
                dia = Validar.dia(p);
            } else {
                labFila = buscar.laboratorio(p);
                labId = labFila.entero("id");
            }
        }

        List<Fila> registros = horarioDAO.listar(sem.entero("id"), labId, dia);
        Map<String, FilaMatriz> mapa = new LinkedHashMap<>();
        boolean incluirLab = (labId == null);

        for (Fila h : registros) {
            String clave = h.texto("sigla") + "|" + h.texto("grupo");
            FilaMatriz m = mapa.computeIfAbsent(clave, k ->
                    new FilaMatriz(h.texto("sigla"), h.texto("grupo"), h.texto("materia"), h.texto("docente")));

            String franja = Formato.hora(h.hora("hora_inicio")) + " - " + Formato.hora(h.hora("hora_fin"));
            if (incluirLab && h.texto("lab") != null) {
                franja += " (" + h.texto("lab") + ")";
            }
            m.agregarSesion(h.entero("dia_semana"), franja);
        }

        List<String[]> filas = new ArrayList<>();
        for (FilaMatriz m : mapa.values()) {
            filas.add(m.aFila());
        }

        String titulo;
        if (labFila != null) {
            titulo = "Horario " + labFila.texto("codigo") + " — Semestre " + sem.texto("gestion");
        } else {
            titulo = "Horario Semestre " + sem.texto("gestion");
        }
        if (dia != null) {
            titulo += " (" + Formato.dia(dia) + ")";
        }

        String detalle = filas.size() + " materia(s)/grupo(s) (" + registros.size() + " sesiones semanales).";
        return Resultado.tabla(titulo, detalle, COLUMNAS, filas);
    }

    // CONDIS["2026-11-03","08:00","10:00"]  (laboratorios libres en esa franja)
    // CONDIS["LAB-41","2026-11-03"]           (ocupación del laboratorio ese día)
    public Resultado consultarDisponibilidad(Comando c, Usuario u) throws NegocioException, SQLException {
        String sintaxis = "CONDIS[\"fecha\",\"inicio\",\"fin\"] o CONDIS[\"laboratorio\",\"fecha\"]";
        if (c.cantidad() == 3 && Validar.esFecha(c.param(0))) {
            LocalDate fecha = Validar.fecha(c.param(0), "fecha");
            LocalTime inicio = Validar.hora(c.param(1), "hora de inicio");
            LocalTime fin = Validar.hora(c.param(2), "hora de fin");
            Validar.rangoHoras(inicio, fin);
            List<String[]> filas = new ArrayList<>();
            int libres = 0;
            for (Fila lab : laboratorioDAO.listar(null)) {
                Disponibilidad.Ocupacion o = disponibilidad.ocupacion(lab, fecha, inicio, fin, 0);
                libres += o == null ? 1 : 0;
                filas.add(new String[]{lab.texto("codigo"), lab.texto("nombre"), lab.texto("capacidad"),
                        o == null ? "LIBRE" : "OCUPADO", o == null ? "" : o.detalle()});
            }
            return Resultado.tabla("Disponibilidad " + fecha + " " + Formato.franja(inicio, fin),
                    libres + " laboratorio(s) libre(s) de " + filas.size() + " (" + Formato.dia(fecha.getDayOfWeek().getValue()) + ").",
                    new String[]{"Lab", "Nombre", "Capacidad", "Estado", "Detalle"}, filas);
        }
        if (c.cantidad() == 2) {
            Fila lab = buscar.laboratorio(c.param(0));
            LocalDate fecha = Validar.fecha(c.param(1), "fecha");
            return agendaDelDia(lab, fecha);
        }
        throw new NegocioException("Sintaxis: " + sintaxis);
    }

    private Resultado agendaDelDia(Fila lab, LocalDate fecha) throws SQLException {
        List<String[]> filas = new ArrayList<>();
        String nota = "";
        Fila bloqueo = fechaBloqueadaDAO.bloqueo(fecha, lab.entero("id"));
        if (bloqueo != null) {
            nota = " FECHA BLOQUEADA: " + bloqueo.texto("tipo") + " " + bloqueo.texto("motivo") + ".";
        }
        if (!"DISPONIBLE".equals(lab.texto("estado"))) {
            nota += " El laboratorio está en " + lab.texto("estado") + ".";
        }
        int dia = fecha.getDayOfWeek().getValue();
        try {
            Fila sem = buscar.semestreActivo();
            if (dia <= 6 && !fecha.isBefore(sem.fecha("fecha_inicio")) && !fecha.isAfter(sem.fecha("fecha_fin"))) {
                for (Fila h : horarioDAO.listar(sem.entero("id"), lab.entero("id"), dia)) {
                    filas.add(new String[]{h.texto("hora_inicio"), h.texto("hora_fin"), "CLASE",
                            h.texto("sigla") + "-" + h.texto("grupo") + " " + h.texto("materia") + " (" + h.texto("docente") + ")"});
                }
            }
        } catch (NegocioException e) {
            nota += " " + e.getMessage();
        }
        for (Fila r : reservaDAO.listar(null, "APROBADA")) {
            if (r.entero("laboratorio_id") == lab.entero("id") && fecha.equals(r.fecha("fecha"))) {
                filas.add(new String[]{r.texto("hora_inicio"), r.texto("hora_fin"), "RESERVA",
                        "#" + r.texto("id") + " " + r.texto("tipo") + ": " + r.texto("motivo") + " (" + r.texto("solicitante") + ")"});
            }
        }
        filas.sort(Comparator.comparing(f -> f[0]));
        return Resultado.tabla("Ocupación de " + lab.texto("codigo") + " el " + Formato.dia(dia) + " " + fecha,
                (filas.isEmpty() ? "El laboratorio no tiene clases ni reservas ese día." : filas.size() + " ocupación(es).") + nota,
                new String[]{"Desde", "Hasta", "Tipo", "Detalle"}, filas);
    }

    /** Valida los 8 datos de una clase a partir de la posición desde y verifica que no choque. */
    private DatosClase validar(String[] p, int desde, int semestreId, int excluirId) throws NegocioException, SQLException {
        Fila lab = buscar.laboratorio(p[desde]);
        if (!"DISPONIBLE".equals(lab.texto("estado"))) {
            throw new NegocioException("El laboratorio " + lab.texto("codigo") + " está en estado "
                    + lab.texto("estado") + " y no está disponible para pasar clases.");
        }
        int dia = Validar.dia(p[desde + 1]);
        LocalTime inicio = Validar.hora(p[desde + 2], "hora de inicio");
        LocalTime fin = Validar.hora(p[desde + 3], "hora de fin");
        Validar.rangoHoras(inicio, fin);
        String sigla = Validar.texto(p[desde + 4], "sigla", 10).toUpperCase(Locale.ROOT);
        String nombreMateria = Validar.texto(p[desde + 5], "materia", 100);
        String grupo = Validar.texto(p[desde + 6], "grupo", 5).toUpperCase(Locale.ROOT);
        Usuario docente = buscar.usuarioActivoConRol(p[desde + 7], "DOC", "docente");

        Fila choque = horarioDAO.choque(semestreId, lab.entero("id"), dia, inicio, fin, excluirId);
        if (choque != null) {
            throw new NegocioException("Choca con la clase #" + choque.texto("id") + " " + choque.texto("sigla") + "-"
                    + choque.texto("grupo") + " (" + choque.texto("docente") + ") del " + Formato.dia(dia) + " "
                    + Formato.franja(choque.hora("hora_inicio"), choque.hora("hora_fin")) + " en " + lab.texto("codigo") + ".");
        }
        // La materia se crea automáticamente la primera vez que aparece su sigla
        Fila materia = materiaDAO.buscarPorSigla(sigla);
        int materiaId = materia != null ? materia.entero("id") : materiaDAO.insertar(sigla, nombreMateria);
        return new DatosClase(lab, dia, inicio, fin, materiaId, grupo, docente);
    }

    private Fila buscarClase(String id, Fila sem) throws NegocioException, SQLException {
        Fila clase = horarioDAO.buscarPorId(Validar.id(id, "la clase"));
        if (clase == null || clase.entero("semestre_id") != sem.entero("id")) {
            throw new NegocioException("No existe la clase #" + id + " en el semestre activo.");
        }
        return clase;
    }

    private static String[] filaMatriz(Fila h, boolean incluirLab) {
        String[] r = new String[10];
        r[0] = h.texto("sigla");
        r[1] = h.texto("grupo");
        r[2] = h.texto("materia");
        r[3] = h.texto("docente");
        for (int i = 4; i < 10; i++) {
            r[i] = "";
        }
        int dia = h.entero("dia_semana");
        if (dia >= 1 && dia <= 6) {
            String franja = Formato.hora(h.hora("hora_inicio")) + " - " + Formato.hora(h.hora("hora_fin"));
            if (incluirLab && h.texto("lab") != null) {
                franja += " (" + h.texto("lab") + ")";
            }
            r[3 + dia] = franja;
        }
        return r;
    }
}
