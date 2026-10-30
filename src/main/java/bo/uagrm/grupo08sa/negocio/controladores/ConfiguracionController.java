package bo.uagrm.grupo08sa.negocio.controladores;

import bo.uagrm.grupo08sa.datos.Fila;
import bo.uagrm.grupo08sa.datos.dao.ComandoDAO;
import bo.uagrm.grupo08sa.datos.dao.FechaBloqueadaDAO;
import bo.uagrm.grupo08sa.datos.dao.LaboratorioDAO;
import bo.uagrm.grupo08sa.datos.dao.ReservaDAO;
import bo.uagrm.grupo08sa.datos.dao.RolDAO;
import bo.uagrm.grupo08sa.datos.dao.SemestreDAO;
import bo.uagrm.grupo08sa.datos.modelo.Rol;
import bo.uagrm.grupo08sa.datos.modelo.Usuario;
import bo.uagrm.grupo08sa.negocio.Busquedas;
import bo.uagrm.grupo08sa.negocio.Comando;
import bo.uagrm.grupo08sa.negocio.Manejador;
import bo.uagrm.grupo08sa.negocio.NegocioException;
import bo.uagrm.grupo08sa.negocio.Resultado;
import bo.uagrm.grupo08sa.negocio.Validar;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * CU2. Gestión de Perfiles y Configuración Semestral:
 * ASICOM, INSLAB, MODLAB, LISLAB, INSSEM, ACTSEM, CERSEM, INSFEB, LISFEB.
 */
public class ConfiguracionController {

    private static final String[] ESTADOS_LAB = {"DISPONIBLE", "MANTENIMIENTO", "INHABILITADO"};
    private static final String[] TIPOS_BLOQUEO = {"FERIADO", "PSA", "MANTENIMIENTO", "OTRO"};
    private static final String[] COLUMNAS_LAB = {"Código", "Nombre", "Capacidad", "Ubicación", "Estado"};
    private static final String[] COLUMNAS_SEM = {"Gestión", "Inicio", "Fin", "Estado"};

    private final ComandoDAO comandoDAO = new ComandoDAO();
    private final RolDAO rolDAO = new RolDAO();
    private final LaboratorioDAO laboratorioDAO = new LaboratorioDAO();
    private final SemestreDAO semestreDAO = new SemestreDAO();
    private final FechaBloqueadaDAO fechaBloqueadaDAO = new FechaBloqueadaDAO();
    private final ReservaDAO reservaDAO = new ReservaDAO();
    private final Busquedas buscar = new Busquedas();

    public void registrarEn(Map<String, Manejador> manejadores) {
        manejadores.put("ASICOM", this::asignarComandos);
        manejadores.put("INSLAB", this::registrarLaboratorio);
        manejadores.put("MODLAB", this::modificarLaboratorio);
        manejadores.put("LISLAB", this::listarLaboratorios);
        manejadores.put("INSSEM", this::registrarSemestre);
        manejadores.put("ACTSEM", this::activarSemestre);
        manejadores.put("CERSEM", this::cerrarSemestre);
        manejadores.put("INSFEB", this::registrarFechaBloqueada);
        manejadores.put("LISFEB", this::listarFechasBloqueadas);
    }

    // ASICOM["DOC"]  ASICOM["DOC","AGREGAR","LISUSU"]  ASICOM["DOC","QUITAR","LISUSU,INSUSU"]
    public Resultado asignarComandos(Comando c, Usuario u) throws NegocioException, SQLException {
        String sintaxis = "ASICOM[\"rol\"] para ver sus permisos, o ASICOM[\"rol\",\"AGREGAR|QUITAR\",\"COMANDO1,COMANDO2\"]";
        if (c.cantidad() != 1 && c.cantidad() != 3) {
            throw new NegocioException("Sintaxis: " + sintaxis);
        }
        Rol rol = rolDAO.buscarPorCodigo(c.param(0).toUpperCase(Locale.ROOT));
        if (rol == null) {
            throw new NegocioException("El rol '" + c.param(0) + "' no existe. Use ENC, DOC, AUX o DIR.");
        }
        String mensaje = "Permisos actuales del rol " + rol.getNombre() + ".";
        if (c.cantidad() == 3) {
            String accion = Validar.opcion(c.param(1), "acción", "AGREGAR", "QUITAR");
            List<String> cambiados = new ArrayList<>();
            List<String> sinCambio = new ArrayList<>();
            for (String nombre : c.param(2).toUpperCase(Locale.ROOT).split("[,; ]+")) {
                if (nombre.isEmpty()) {
                    continue;
                }
                if (!comandoDAO.existe(nombre)) {
                    throw new NegocioException("El comando " + nombre + " no existe.");
                }
                if (accion.equals("QUITAR") && (rol.getCodigo().equals("ENC") || nombre.equals("AYUDA"))) {
                    throw new NegocioException("No se puede quitar " + nombre + " al rol " + rol.getNombre()
                            + " (el Encargado conserva todos los comandos y AYUDA es para todos).");
                }
                boolean cambio = accion.equals("AGREGAR")
                        ? comandoDAO.asignar(rol.getId(), nombre) : comandoDAO.quitar(rol.getId(), nombre);
                (cambio ? cambiados : sinCambio).add(nombre);
            }
            mensaje = (accion.equals("AGREGAR") ? "Agregados" : "Quitados") + " al rol " + rol.getNombre() + ": "
                    + (cambiados.isEmpty() ? "ninguno" : String.join(", ", cambiados))
                    + (sinCambio.isEmpty() ? "" : ". Sin cambio (ya estaba así): " + String.join(", ", sinCambio)) + ".";
        }
        return Resultado.tabla("Permisos del rol " + rol.getCodigo(), mensaje,
                new String[]{"Comando", "Descripción"}, comandoDAO.listarPorRol(rol.getId()));
    }

    // INSLAB["LAB-41","Laboratorio 41","40","Módulo 236, piso 4"]
    public Resultado registrarLaboratorio(Comando c, Usuario u) throws NegocioException, SQLException {
        c.exigirParametros(4, "INSLAB[\"codigo\",\"nombre\",\"capacidad\",\"ubicacion\"]");
        String codigo = Validar.texto(c.param(0), "código", 10).toUpperCase(Locale.ROOT);
        String nombre = Validar.texto(c.param(1), "nombre", 60);
        int capacidad = Validar.entero(c.param(2), "capacidad", 1, 500);
        String ubicacion = Validar.textoOpcional(c.param(3), "ubicación", 100);
        if (laboratorioDAO.buscarPorCodigo(codigo) != null) {
            throw new NegocioException("Ya existe un laboratorio con el código " + codigo + ".");
        }
        laboratorioDAO.insertar(codigo, nombre, capacidad, ubicacion);
        return Resultado.tabla("Laboratorio registrado", "El laboratorio quedó DISPONIBLE para reservas.",
                COLUMNAS_LAB, List.<String[]>of(filaLab(laboratorioDAO.buscarPorCodigo(codigo))));
    }

    // MODLAB["LAB-41","Laboratorio 41","45","Módulo 236","MANTENIMIENTO"]
    public Resultado modificarLaboratorio(Comando c, Usuario u) throws NegocioException, SQLException {
        c.exigirParametros(5, "MODLAB[\"codigo\",\"nombre\",\"capacidad\",\"ubicacion\",\"estado\"]");
        Fila lab = buscar.laboratorio(c.param(0));
        String nombre = Validar.texto(c.param(1), "nombre", 60);
        int capacidad = Validar.entero(c.param(2), "capacidad", 1, 500);
        String ubicacion = Validar.textoOpcional(c.param(3), "ubicación", 100);
        String estado = Validar.opcion(c.param(4), "estado", ESTADOS_LAB);
        laboratorioDAO.actualizar(lab.entero("id"), nombre, capacidad, ubicacion, estado);
        String nota = estado.equals("DISPONIBLE") ? "" : " Mientras esté en " + estado + " no se aceptan reservas.";
        return Resultado.tabla("Laboratorio modificado", "Datos actualizados." + nota,
                COLUMNAS_LAB, List.<String[]>of(filaLab(laboratorioDAO.buscarPorId(lab.entero("id")))));
    }

    // LISLAB["*"]  LISLAB["DISPONIBLE"]
    public Resultado listarLaboratorios(Comando c, Usuario u) throws NegocioException, SQLException {
        c.exigirEntre(0, 1, "LISLAB[\"*\"] o LISLAB[\"estado\"]");
        String estado = c.cantidad() == 1 ? Validar.opcion(c.param(0), "estado", ESTADOS_LAB) : null;
        List<String[]> filas = new ArrayList<>();
        for (Fila lab : laboratorioDAO.listar(estado)) {
            filas.add(filaLab(lab));
        }
        return Resultado.tabla("Laboratorios", filas.size() + " laboratorio(s)"
                + (estado == null ? "." : " en estado " + estado + "."), COLUMNAS_LAB, filas);
    }

    // INSSEM["2-2026","2026-08-03","2026-12-12"]
    public Resultado registrarSemestre(Comando c, Usuario u) throws NegocioException, SQLException {
        c.exigirParametros(3, "INSSEM[\"gestion\",\"fecha_inicio\",\"fecha_fin\"]");
        String gestion = Validar.texto(c.param(0), "gestión", 10).toUpperCase(Locale.ROOT);
        LocalDate inicio = Validar.fecha(c.param(1), "fecha de inicio");
        LocalDate fin = Validar.fecha(c.param(2), "fecha de fin");
        if (!fin.isAfter(inicio)) {
            throw new NegocioException("La fecha de fin debe ser posterior a la de inicio.");
        }
        if (semestreDAO.buscarPorGestion(gestion) != null) {
            throw new NegocioException("Ya existe el semestre " + gestion + ".");
        }
        Fila otro = semestreDAO.superpuesto(inicio, fin);
        if (otro != null) {
            throw new NegocioException("Las fechas se superponen con el semestre " + otro.texto("gestion") + " ("
                    + otro.texto("fecha_inicio") + " a " + otro.texto("fecha_fin") + ").");
        }
        semestreDAO.insertar(gestion, inicio, fin);
        return Resultado.tabla("Semestre registrado", "El semestre quedó PLANIFICADO. Actívelo con ACTSEM[\""
                + gestion + "\"].", COLUMNAS_SEM, List.<String[]>of(filaSem(semestreDAO.buscarPorGestion(gestion))));
    }

    // ACTSEM["2-2026"]
    public Resultado activarSemestre(Comando c, Usuario u) throws NegocioException, SQLException {
        c.exigirParametros(1, "ACTSEM[\"gestion\"]");
        Fila sem = buscarSemestre(c.param(0));
        if (!"PLANIFICADO".equals(sem.texto("estado"))) {
            throw new NegocioException("Solo se puede activar un semestre PLANIFICADO; " + sem.texto("gestion")
                    + " está " + sem.texto("estado") + ".");
        }
        Fila activo = semestreDAO.obtenerActivo();
        if (activo != null) {
            throw new NegocioException("Ya existe un semestre activo (" + activo.texto("gestion")
                    + "). Ciérrelo primero con CERSEM[\"" + activo.texto("gestion") + "\"].");
        }
        semestreDAO.cambiarEstado(sem.entero("id"), "ACTIVO");
        return Resultado.tabla("Semestre activado", "Desde ahora el horario, las reservas y los turnos usan este semestre.",
                COLUMNAS_SEM, List.<String[]>of(filaSem(semestreDAO.buscarPorGestion(sem.texto("gestion")))));
    }

    // CERSEM["2-2026"]
    public Resultado cerrarSemestre(Comando c, Usuario u) throws NegocioException, SQLException {
        c.exigirParametros(1, "CERSEM[\"gestion\"]");
        Fila sem = buscarSemestre(c.param(0));
        if (!"ACTIVO".equals(sem.texto("estado"))) {
            throw new NegocioException("Solo se puede cerrar el semestre activo; " + sem.texto("gestion")
                    + " está " + sem.texto("estado") + ".");
        }
        semestreDAO.cambiarEstado(sem.entero("id"), "CERRADO");
        return Resultado.tabla("Semestre cerrado", "El semestre quedó CERRADO; ya puede activar el siguiente.",
                COLUMNAS_SEM, List.<String[]>of(filaSem(semestreDAO.buscarPorGestion(sem.texto("gestion")))));
    }

    // INSFEB["2026-11-02","FERIADO","Todos Santos"]  INSFEB["2026-11-20","MANTENIMIENTO","Cableado","LAB-41"]
    public Resultado registrarFechaBloqueada(Comando c, Usuario u) throws NegocioException, SQLException {
        c.exigirEntre(3, 4, "INSFEB[\"fecha\",\"tipo\",\"motivo\"] o INSFEB[\"fecha\",\"tipo\",\"motivo\",\"laboratorio\"]");
        LocalDate fecha = Validar.fecha(c.param(0), "fecha");
        String tipo = Validar.opcion(c.param(1), "tipo", TIPOS_BLOQUEO);
        String motivo = Validar.textoOpcional(c.param(2), "motivo", 200);
        Fila lab = c.cantidad() == 4 && !c.param(3).isEmpty() && !c.param(3).equals("*")
                ? buscar.laboratorio(c.param(3)) : null;
        Integer labId = lab == null ? null : lab.entero("id");

        Fila sem = semestreDAO.contenedor(fecha);
        if (sem == null) {
            throw new NegocioException("La fecha " + fecha + " no pertenece a ningún semestre planificado o activo.");
        }
        if (fechaBloqueadaDAO.duplicada(fecha, labId)) {
            throw new NegocioException("La fecha " + fecha + " ya está bloqueada"
                    + (lab == null ? " para todos los laboratorios." : " para " + lab.texto("codigo") + "."));
        }
        fechaBloqueadaDAO.insertar(sem.entero("id"), fecha, tipo, motivo, labId);
        int afectadas = reservaDAO.aprobadasEnFecha(fecha, labId);
        String aviso = afectadas == 0 ? "" : " Atención: hay " + afectadas
                + " reserva(s) aprobada(s) en esa fecha; revíselas con LISRES[\"APROBADA\"].";
        return Resultado.tabla("Fecha bloqueada", "Se bloqueó el " + fecha
                + (lab == null ? " para todos los laboratorios." : " para " + lab.texto("codigo") + ".") + aviso,
                new String[]{"Semestre", "Fecha", "Tipo", "Motivo", "Laboratorio"},
                List.<String[]>of(new String[]{sem.texto("gestion"), fecha.toString(), tipo, motivo == null ? "" : motivo,
                        lab == null ? "Todos" : lab.texto("codigo")}));
    }

    // LISFEB["*"]  LISFEB["2-2026"]
    public Resultado listarFechasBloqueadas(Comando c, Usuario u) throws NegocioException, SQLException {
        c.exigirEntre(0, 1, "LISFEB[\"*\"] (semestre activo) o LISFEB[\"gestion\"]");
        Fila sem = c.cantidad() == 1 ? buscarSemestre(c.param(0)) : buscar.semestreActivo();
        List<String[]> filas = new ArrayList<>();
        for (Fila f : fechaBloqueadaDAO.listar(sem.entero("id"))) {
            filas.add(new String[]{f.texto("fecha"), f.texto("tipo"), f.texto("motivo"),
                    f.texto("lab").isEmpty() ? "Todos" : f.texto("lab")});
        }
        return Resultado.tabla("Fechas bloqueadas " + sem.texto("gestion"), filas.size() + " fecha(s) bloqueada(s).",
                new String[]{"Fecha", "Tipo", "Motivo", "Laboratorio"}, filas);
    }

    private Fila buscarSemestre(String gestion) throws NegocioException, SQLException {
        Fila sem = semestreDAO.buscarPorGestion(gestion.trim());
        if (sem == null) {
            throw new NegocioException("No existe el semestre '" + gestion + "'.");
        }
        return sem;
    }

    private static String[] filaLab(Fila l) {
        return new String[]{l.texto("codigo"), l.texto("nombre"), l.texto("capacidad"), l.texto("ubicacion"), l.texto("estado")};
    }

    private static String[] filaSem(Fila s) {
        return new String[]{s.texto("gestion"), s.texto("fecha_inicio"), s.texto("fecha_fin"), s.texto("estado")};
    }
}
