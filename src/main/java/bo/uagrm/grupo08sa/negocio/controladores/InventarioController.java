package bo.uagrm.grupo08sa.negocio.controladores;

import bo.uagrm.grupo08sa.datos.Fila;
import bo.uagrm.grupo08sa.datos.dao.EquipoDAO;
import bo.uagrm.grupo08sa.datos.dao.IncidenciaDAO;
import bo.uagrm.grupo08sa.datos.dao.ObjetoEncontradoDAO;
import bo.uagrm.grupo08sa.datos.modelo.Usuario;
import bo.uagrm.grupo08sa.negocio.AccesoDenegadoException;
import bo.uagrm.grupo08sa.negocio.Busquedas;
import bo.uagrm.grupo08sa.negocio.Comando;
import bo.uagrm.grupo08sa.negocio.Manejador;
import bo.uagrm.grupo08sa.negocio.NegocioException;
import bo.uagrm.grupo08sa.negocio.Notificaciones;
import bo.uagrm.grupo08sa.negocio.Resultado;
import bo.uagrm.grupo08sa.negocio.Validar;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * CU6. Gestión de Inventario, Incidencias, Red y Objetos Encontrados:
 * INSEQU, MODEQU, BAJEQU, LISEQU, LISRED, INSINC, MODINC, LISINC, INSOBJ, ENTOBJ, LISOBJ.
 */
public class InventarioController {

    private static final String[] TIPOS_EQUIPO = {"COMPUTADORA", "PROYECTOR", "COMPONENTE", "SOFTWARE", "OTRO"};
    private static final String[] ESTADOS_EQUIPO = {"OPERATIVO", "EN_REPARACION", "BAJA"};
    private static final String[] ESTADOS_INCIDENCIA = {"PENDIENTE", "EN_ATENCION", "RESUELTA"};
    private static final String[] ESTADOS_OBJETO = {"EN_CUSTODIA", "ENTREGADO"};
    private static final String[] COLUMNAS_EQUIPO = {"Activo", "Lab", "Descripción", "Estado"};
    private static final String[] COLUMNAS_RED = {"Activo", "Código", "IP Fija", "Máscara", "Gateway", "DNS", "Grupo Red"};
    private static final String[] COLUMNAS_INCIDENCIA =
            {"ID", "Equipo", "Lab", "Descripción", "Reportado por", "Fecha", "Estado", "Solución", "Resuelta"};
    private static final String[] COLUMNAS_OBJETO =
            {"ID", "Lab", "Descripción", "Hallazgo", "Registrado por", "Estado", "Entregado a", "CI", "Entrega"};

    private final EquipoDAO equipoDAO = new EquipoDAO();
    private final IncidenciaDAO incidenciaDAO = new IncidenciaDAO();
    private final ObjetoEncontradoDAO objetoDAO = new ObjetoEncontradoDAO();
    private final Busquedas buscar = new Busquedas();
    private final Notificaciones notificaciones;

    public InventarioController(Notificaciones notificaciones) {
        this.notificaciones = notificaciones;
    }

    public void registrarEn(Map<String, Manejador> manejadores) {
        manejadores.put("INSEQU", this::registrarEquipo);
        manejadores.put("MODEQU", this::modificarEquipo);
        manejadores.put("BAJEQU", this::darBajaEquipo);
        manejadores.put("LISEQU", this::listarEquipos);
        manejadores.put("LISRED", this::listarRed);
        manejadores.put("INSINC", this::reportarIncidencia);
        manejadores.put("MODINC", this::actualizarIncidencia);
        manejadores.put("LISINC", this::listarIncidencias);
        manejadores.put("INSOBJ", this::registrarObjeto);
        manejadores.put("ENTOBJ", this::entregarObjeto);
        manejadores.put("LISOBJ", this::listarObjetos);
    }

    // INSEQU["PC-L41-01","LAB-41","COMPUTADORA","Core i5, 16 GB RAM"]
    public Resultado registrarEquipo(Comando c, Usuario u) throws NegocioException, SQLException {
        c.exigirParametros(4, "INSEQU[\"codigo\",\"laboratorio\",\"tipo\",\"descripcion\"]");
        String codigo = Validar.texto(c.param(0), "código", 20).toUpperCase(Locale.ROOT);
        Fila lab = buscar.laboratorio(c.param(1));
        String tipo = Validar.opcion(c.param(2), "tipo", TIPOS_EQUIPO);
        String descripcion = Validar.texto(c.param(3), "descripción", 200);
        if (equipoDAO.buscarPorCodigo(codigo) != null) {
            throw new NegocioException("Ya existe un equipo con el código " + codigo + ".");
        }
        equipoDAO.insertar(codigo, lab.entero("id"), tipo, descripcion);
        return Resultado.tabla("Equipo registrado", "El equipo quedó OPERATIVO en " + lab.texto("codigo") + ".",
                COLUMNAS_EQUIPO, List.<String[]>of(filaEquipo(equipoDAO.buscarPorCodigo(codigo))));
    }

    // MODEQU["PC-L41-01","LAB-41","COMPUTADORA","Core i7, 32 GB RAM","OPERATIVO"]
    public Resultado modificarEquipo(Comando c, Usuario u) throws NegocioException, SQLException {
        c.exigirParametros(5, "MODEQU[\"codigo\",\"laboratorio\",\"tipo\",\"descripcion\",\"estado\"]");
        Fila equipo = buscarEquipo(c.param(0));
        Fila lab = buscar.laboratorio(c.param(1));
        String tipo = Validar.opcion(c.param(2), "tipo", TIPOS_EQUIPO);
        String descripcion = Validar.texto(c.param(3), "descripción", 200);
        String estado = Validar.opcion(c.param(4), "estado", ESTADOS_EQUIPO);
        equipoDAO.actualizar(equipo.entero("id"), lab.entero("id"), tipo, descripcion, estado);
        return Resultado.tabla("Equipo modificado", "Datos del equipo actualizados.",
                COLUMNAS_EQUIPO, List.<String[]>of(filaEquipo(equipoDAO.buscarPorCodigo(equipo.texto("codigo")))));
    }

    // BAJEQU["PC-L41-01"]
    public Resultado darBajaEquipo(Comando c, Usuario u) throws NegocioException, SQLException {
        c.exigirParametros(1, "BAJEQU[\"codigo\"]");
        Fila equipo = buscarEquipo(c.param(0));
        if ("BAJA".equals(equipo.texto("estado"))) {
            throw new NegocioException("El equipo " + equipo.texto("codigo") + " ya está dado de baja.");
        }
        equipoDAO.cambiarEstado(equipo.entero("id"), "BAJA");
        return Resultado.tabla("Equipo dado de baja", "El equipo sale del inventario operativo.",
                COLUMNAS_EQUIPO, List.<String[]>of(filaEquipo(equipoDAO.buscarPorCodigo(equipo.texto("codigo")))));
    }

    // LISEQU["*"]  LISEQU["LAB-41"]  LISEQU["EN_REPARACION"]  LISEQU["LAB-41","OPERATIVO"]
    public Resultado listarEquipos(Comando c, Usuario u) throws NegocioException, SQLException {
        c.exigirEntre(0, 2, "LISEQU[\"*\"], LISEQU[\"laboratorio\"], LISEQU[\"estado\"] o LISEQU[\"laboratorio\",\"estado\"]");
        Integer labId = null;
        String estado = null;
        for (String p : c.getParametros()) {
            if (p.trim().equals("*")) {
                continue;
            }
            if (Validar.esOpcion(p, ESTADOS_EQUIPO)) {
                estado = Validar.opcion(p, "estado", ESTADOS_EQUIPO);
            } else {
                labId = buscar.laboratorio(p).entero("id");
            }
        }
        List<String[]> filas = new ArrayList<>();
        for (Fila e : equipoDAO.listar(labId, estado)) {
            filas.add(filaEquipo(e));
        }
        return Resultado.tabla("Inventario", filas.size() + " equipo(s).", COLUMNAS_EQUIPO, filas);
    }

    // LISRED["*"]  LISRED["LAB-41"] (Solo Encargados)
    public Resultado listarRed(Comando c, Usuario u) throws NegocioException, SQLException {
        if (u == null || !"ENC".equalsIgnoreCase(u.getRolCodigo())) {
            throw new AccesoDenegadoException("Solo el Encargado de Laboratorios puede consultar la configuración de red e IPs.");
        }
        c.exigirEntre(0, 1, "LISRED[\"*\"], LISRED[\"laboratorio\"]");
        Integer labId = null;
        if (c.cantidad() == 1 && !c.param(0).trim().equals("*")) {
            labId = buscar.laboratorio(c.param(0)).entero("id");
        }
        List<String[]> filas = new ArrayList<>();
        for (Fila r : equipoDAO.listarRed(labId)) {
            filas.add(new String[]{
                r.texto("activo") != null ? r.texto("activo") : "",
                r.texto("codigo"),
                r.texto("ip"),
                r.texto("mascara"),
                r.texto("gateway"),
                r.texto("dns1"),
                r.texto("grupo_red")
            });
        }
        return Resultado.tabla("Configuración de Red e IPs (Privado Encargados)",
                filas.size() + " equipo(s) con red asignada.", COLUMNAS_RED, filas);
    }

    // INSINC["PC-L41-01","No enciende el monitor"]
    public Resultado reportarIncidencia(Comando c, Usuario u) throws NegocioException, SQLException {
        c.exigirParametros(2, "INSINC[\"codigo_equipo\",\"descripcion\"]");
        Fila equipo = buscarEquipo(c.param(0));
        if ("BAJA".equals(equipo.texto("estado"))) {
            throw new NegocioException("El equipo " + equipo.texto("codigo") + " está dado de baja.");
        }
        String descripcion = Validar.texto(c.param(1), "descripción de la falla", 300);
        int id = incidenciaDAO.insertar(equipo.entero("id"), u.getId(), descripcion);
        equipoDAO.cambiarEstado(equipo.entero("id"), "EN_REPARACION");
        notificaciones.aEncargados("Nueva incidencia #" + id + " en " + equipo.texto("lab"),
                u.getNombreCompleto() + " reportó en " + equipo.texto("codigo") + " (" + equipo.texto("lab") + "): "
                        + descripcion + ". Actualice con MODINC[\"" + id + "\",\"EN_ATENCION\"] o MODINC[\"" + id
                        + "\",\"RESUELTA\",\"solución\"].", null, u.getId());
        return Resultado.tabla("Incidencia reportada", "El equipo quedó EN_REPARACION y se notificó al Encargado.",
                COLUMNAS_INCIDENCIA, List.<String[]>of(filaIncidencia(incidenciaDAO.buscarPorId(id))));
    }

    // MODINC["5","EN_ATENCION"]  MODINC["5","RESUELTA","Se cambió el cable VGA"]
    public Resultado actualizarIncidencia(Comando c, Usuario u) throws NegocioException, SQLException {
        c.exigirEntre(2, 3, "MODINC[\"id\",\"EN_ATENCION\"] o MODINC[\"id\",\"RESUELTA\",\"solucion\"]");
        Fila inc = incidenciaDAO.buscarPorId(Validar.id(c.param(0), "la incidencia"));
        if (inc == null) {
            throw new NegocioException("No existe la incidencia #" + c.param(0) + ".");
        }
        if ("RESUELTA".equals(inc.texto("estado"))) {
            throw new NegocioException("La incidencia #" + inc.texto("id") + " ya está RESUELTA.");
        }
        String estado = Validar.opcion(c.param(1), "estado", "EN_ATENCION", "RESUELTA");
        String solucion = Validar.textoOpcional(c.paramOpcional(2), "solución", 300);
        if (estado.equals("RESUELTA") && solucion == null) {
            throw new NegocioException("Indique la solución: MODINC[\"" + inc.texto("id") + "\",\"RESUELTA\",\"solución\"].");
        }
        incidenciaDAO.actualizar(inc.entero("id"), estado, solucion);
        String nota = "";
        if (estado.equals("RESUELTA") && incidenciaDAO.abiertas(inc.entero("equipo_id")) == 0
                && "EN_REPARACION".equals(inc.texto("equipo_estado"))) {
            equipoDAO.cambiarEstado(inc.entero("equipo_id"), "OPERATIVO");
            nota = " El equipo " + inc.texto("equipo") + " volvió a OPERATIVO.";
        }
        notificaciones.aUsuario(inc.entero("reportado_por"), "Incidencia #" + inc.texto("id") + ": " + estado,
                "Su reporte sobre " + inc.texto("equipo") + " (" + inc.texto("descripcion") + ") está " + estado
                        + (solucion == null ? "." : ". Solución: " + solucion + "."), null);
        return Resultado.tabla("Incidencia actualizada", "La incidencia pasó a " + estado + "." + nota,
                COLUMNAS_INCIDENCIA, List.<String[]>of(filaIncidencia(incidenciaDAO.buscarPorId(inc.entero("id")))));
    }

    // LISINC["*"]  LISINC["PENDIENTE"]  LISINC["LAB-41"]  LISINC["LAB-41","RESUELTA"]
    public Resultado listarIncidencias(Comando c, Usuario u) throws NegocioException, SQLException {
        c.exigirEntre(0, 2, "LISINC[\"*\"], LISINC[\"estado\"], LISINC[\"laboratorio\"] o LISINC[\"laboratorio\",\"estado\"]");
        Integer labId = null;
        String estado = null;
        for (String p : c.getParametros()) {
            if (p.trim().equals("*")) {
                continue;
            }
            if (Validar.esOpcion(p, ESTADOS_INCIDENCIA)) {
                estado = Validar.opcion(p, "estado", ESTADOS_INCIDENCIA);
            } else {
                labId = buscar.laboratorio(p).entero("id");
            }
        }
        List<String[]> filas = new ArrayList<>();
        for (Fila i : incidenciaDAO.listar(estado, labId)) {
            filas.add(filaIncidencia(i));
        }
        return Resultado.tabla("Incidencias", filas.size() + " incidencia(s).", COLUMNAS_INCIDENCIA, filas);
    }

    // INSOBJ["LAB-41","Mochila negra con cuadernos"]  INSOBJ["LAB-41","Celular Samsung","2026-10-28"]
    public Resultado registrarObjeto(Comando c, Usuario u) throws NegocioException, SQLException {
        c.exigirEntre(2, 3, "INSOBJ[\"laboratorio\",\"descripcion\"] o INSOBJ[\"laboratorio\",\"descripcion\",\"fecha\"]");
        Fila lab = buscar.laboratorio(c.param(0));
        String descripcion = Validar.texto(c.param(1), "descripción", 200);
        LocalDate fecha = c.paramOpcional(2).isEmpty() ? LocalDate.now() : Validar.fecha(c.param(2), "fecha de hallazgo");
        if (fecha.isAfter(LocalDate.now())) {
            throw new NegocioException("La fecha de hallazgo no puede ser futura.");
        }
        int id = objetoDAO.insertar(lab.entero("id"), descripcion, fecha, u.getId());
        return Resultado.tabla("Objeto registrado", "El objeto quedó EN_CUSTODIA. Para entregarlo use ENTOBJ[\""
                + id + "\",\"nombre\",\"CI\"].", COLUMNAS_OBJETO, List.<String[]>of(filaObjeto(objetoDAO.buscarPorId(id))));
    }

    // ENTOBJ["3","Juan Pérez","8123456"]
    public Resultado entregarObjeto(Comando c, Usuario u) throws NegocioException, SQLException {
        c.exigirParametros(3, "ENTOBJ[\"id\",\"nombre_de_quien_recibe\",\"ci\"]");
        Fila obj = objetoDAO.buscarPorId(Validar.id(c.param(0), "el objeto"));
        if (obj == null) {
            throw new NegocioException("No existe el objeto #" + c.param(0) + ".");
        }
        if ("ENTREGADO".equals(obj.texto("estado"))) {
            throw new NegocioException("El objeto #" + obj.texto("id") + " ya fue entregado a " + obj.texto("entregado_a")
                    + " el " + obj.texto("fecha_entrega") + ".");
        }
        String receptor = Validar.texto(c.param(1), "nombre de quien recibe", 100);
        String ci = Validar.texto(c.param(2), "CI", 15);
        objetoDAO.entregar(obj.entero("id"), receptor, ci);
        return Resultado.tabla("Objeto entregado", "Se registró la entrega a " + receptor + ".",
                COLUMNAS_OBJETO, List.<String[]>of(filaObjeto(objetoDAO.buscarPorId(obj.entero("id")))));
    }

    // LISOBJ["*"]  LISOBJ["EN_CUSTODIA"]
    public Resultado listarObjetos(Comando c, Usuario u) throws NegocioException, SQLException {
        c.exigirEntre(0, 1, "LISOBJ[\"*\"] o LISOBJ[\"estado\"]");
        String estado = c.cantidad() == 1 ? Validar.opcion(c.param(0), "estado", ESTADOS_OBJETO) : null;
        List<String[]> filas = new ArrayList<>();
        for (Fila o : objetoDAO.listar(estado)) {
            filas.add(filaObjeto(o));
        }
        return Resultado.tabla("Objetos encontrados", filas.size() + " objeto(s).", COLUMNAS_OBJETO, filas);
    }

    private Fila buscarEquipo(String codigo) throws NegocioException, SQLException {
        Fila e = equipoDAO.buscarPorCodigo(codigo.trim());
        if (e == null) {
            throw new NegocioException("No existe el equipo '" + codigo + "'. Vea el inventario con LISEQU[\"*\"].");
        }
        return e;
    }

    private static String[] filaEquipo(Fila e) {
        String lab = e.texto("lab");
        if (lab.startsWith("LAB-")) {
            lab = "236-" + lab.substring(4);
        }
        String act = e.texto("activo");
        if (act == null || act.isBlank()) {
            act = e.texto("codigo");
        }
        return new String[]{act, lab, e.texto("descripcion"), e.texto("estado")};
    }

    private static String[] filaIncidencia(Fila i) {
        return new String[]{i.texto("id"), i.texto("equipo"), i.texto("lab"), i.texto("descripcion"), i.texto("reportado"),
                i.texto("fecha_reporte"), i.texto("estado"), i.texto("solucion"), i.texto("fecha_solucion")};
    }

    private static String[] filaObjeto(Fila o) {
        return new String[]{o.texto("id"), o.texto("lab"), o.texto("descripcion"), o.texto("fecha_hallazgo"),
                o.texto("registrado"), o.texto("estado"), o.texto("entregado_a"), o.texto("ci_receptor"), o.texto("fecha_entrega")};
    }
}
