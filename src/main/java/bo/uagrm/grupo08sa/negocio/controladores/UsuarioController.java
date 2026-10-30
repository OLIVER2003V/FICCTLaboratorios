package bo.uagrm.grupo08sa.negocio.controladores;

import bo.uagrm.grupo08sa.datos.dao.RolDAO;
import bo.uagrm.grupo08sa.datos.dao.UsuarioDAO;
import bo.uagrm.grupo08sa.datos.modelo.Rol;
import bo.uagrm.grupo08sa.datos.modelo.Usuario;
import bo.uagrm.grupo08sa.negocio.Comando;
import bo.uagrm.grupo08sa.negocio.Manejador;
import bo.uagrm.grupo08sa.negocio.NegocioException;
import bo.uagrm.grupo08sa.negocio.Resultado;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * CU1. Gestión de Usuarios: INSUSU, MODUSU, BAJUSU, ACTUSU, LISUSU.
 */
public class UsuarioController {

    private static final String SINTAXIS_INSUSU =
            "INSUSU[\"codigo\",\"nombre\",\"apellido\",\"correo\",\"telefono\",\"rol\"]";
    private static final String SINTAXIS_MODUSU =
            "MODUSU[\"codigo\",\"nombre\",\"apellido\",\"correo\",\"telefono\",\"rol\"]";
    private static final Pattern CORREO = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");
    private static final Pattern TELEFONO = Pattern.compile("^[0-9+\\- ]{6,20}$");
    private static final String[] COLUMNAS =
            {"Código", "Nombre", "Apellido", "Correo", "Teléfono", "Rol", "Estado"};

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private final RolDAO rolDAO = new RolDAO();

    public void registrarEn(Map<String, Manejador> manejadores) {
        manejadores.put("INSUSU", this::registrar);
        manejadores.put("MODUSU", this::modificar);
        manejadores.put("BAJUSU", this::darBaja);
        manejadores.put("ACTUSU", this::reactivar);
        manejadores.put("LISUSU", this::listar);
    }

    // INSUSU["219001","Ana","Rojas","ana@gmail.com","70011223","DOC"]
    public Resultado registrar(Comando c, Usuario solicitante) throws NegocioException, SQLException {
        c.exigirParametros(6, SINTAXIS_INSUSU);
        Usuario u = leerDatos(c);

        if (usuarioDAO.buscarPorCodigo(u.getCodigo()) != null) {
            throw new NegocioException("Ya existe un usuario con el código " + u.getCodigo() + ".");
        }
        if (usuarioDAO.existeCorreo(u.getCorreo(), 0)) {
            throw new NegocioException("El correo " + u.getCorreo() + " ya está registrado.");
        }
        usuarioDAO.insertar(u);
        return tablaDeUno("Usuario registrado",
                "El usuario fue registrado con estado ACTIVO.", usuarioDAO.buscarPorCodigo(u.getCodigo()));
    }

    // MODUSU["219001","Ana","Rojas Paz","ana@gmail.com","70011223","AUX"]
    public Resultado modificar(Comando c, Usuario solicitante) throws NegocioException, SQLException {
        c.exigirParametros(6, SINTAXIS_MODUSU);
        Usuario existente = buscarExistente(c.param(0));
        Usuario u = leerDatos(c);
        u.setId(existente.getId());

        if (usuarioDAO.existeCorreo(u.getCorreo(), u.getId())) {
            throw new NegocioException("El correo " + u.getCorreo() + " ya pertenece a otro usuario.");
        }
        if (existente.getId() == solicitante.getId() && !"ENC".equals(u.getRolCodigo())) {
            throw new NegocioException("No puede quitarse a sí mismo el rol de Encargado.");
        }
        usuarioDAO.actualizar(u);
        return tablaDeUno("Usuario modificado", "Los datos del usuario fueron actualizados.",
                usuarioDAO.buscarPorCodigo(u.getCodigo()));
    }

    // BAJUSU["219001"]
    public Resultado darBaja(Comando c, Usuario solicitante) throws NegocioException, SQLException {
        c.exigirParametros(1, "BAJUSU[\"codigo\"]");
        Usuario u = buscarExistente(c.param(0));
        if (u.getId() == solicitante.getId()) {
            throw new NegocioException("No puede darse de baja a sí mismo.");
        }
        if (!u.estaActivo()) {
            throw new NegocioException("El usuario " + u.getCodigo() + " ya está dado de baja.");
        }
        usuarioDAO.cambiarEstado(u.getId(), "INACTIVO");
        return tablaDeUno("Usuario dado de baja",
                "El usuario ya no podrá enviar comandos al sistema.", usuarioDAO.buscarPorCodigo(u.getCodigo()));
    }

    // ACTUSU["219001"]
    public Resultado reactivar(Comando c, Usuario solicitante) throws NegocioException, SQLException {
        c.exigirParametros(1, "ACTUSU[\"codigo\"]");
        Usuario u = buscarExistente(c.param(0));
        if (u.estaActivo()) {
            throw new NegocioException("El usuario " + u.getCodigo() + " ya está activo.");
        }
        usuarioDAO.cambiarEstado(u.getId(), "ACTIVO");
        return tablaDeUno("Usuario reactivado",
                "El usuario puede volver a enviar comandos.", usuarioDAO.buscarPorCodigo(u.getCodigo()));
    }

    // LISUSU["*"]  LISUSU["DOC"]  LISUSU["INACTIVO"]  LISUSU["DOC","ACTIVO"]  LISUSU["*","ACTIVO"]
    public Resultado listar(Comando c, Usuario solicitante) throws NegocioException, SQLException {
        if (c.cantidad() > 2) {
            throw new NegocioException("Sintaxis: LISUSU[\"*\"], LISUSU[\"rol\"], LISUSU[\"estado\"] "
                    + "o LISUSU[\"rol\",\"estado\"].");
        }
        String rol = null;
        String estado = null;
        for (int i = 0; i < c.cantidad(); i++) {
            String valor = c.param(i).toUpperCase();
            if (valor.equals("*")) {
                continue;
            }
            if (valor.equals("ACTIVO") || valor.equals("INACTIVO")) {
                estado = valor;
            } else if (rolDAO.buscarPorCodigo(valor) != null) {
                rol = valor;
            } else {
                throw new NegocioException("'" + c.param(i) + "' no es un rol (ENC, DOC, AUX, DIR) "
                        + "ni un estado (ACTIVO, INACTIVO).");
            }
        }

        List<String[]> filas = new ArrayList<>();
        for (Usuario u : usuarioDAO.listar(rol, estado)) {
            filas.add(fila(u));
        }
        String filtro = (rol == null ? "todos los roles" : "rol " + rol)
                + ", " + (estado == null ? "todos los estados" : "estado " + estado);
        return Resultado.tabla("Listado de usuarios", filas.size() + " usuario(s) — " + filtro + ".",
                COLUMNAS, filas);
    }

    private Usuario leerDatos(Comando c) throws NegocioException, SQLException {
        String codigo = requerido(c.param(0), "código", 20);
        String nombre = requerido(c.param(1), "nombre", 60);
        String apellido = requerido(c.param(2), "apellido", 60);
        String correo = requerido(c.param(3), "correo", 100).toLowerCase();
        String telefono = c.param(4);
        String rolCodigo = requerido(c.param(5), "rol", 3).toUpperCase();

        if (!CORREO.matcher(correo).matches()) {
            throw new NegocioException("El correo '" + correo + "' no tiene un formato válido.");
        }
        if (!telefono.isEmpty() && !TELEFONO.matcher(telefono).matches()) {
            throw new NegocioException("El teléfono '" + telefono + "' no es válido (solo números, de 6 a 20).");
        }
        Rol rol = rolDAO.buscarPorCodigo(rolCodigo);
        if (rol == null) {
            throw new NegocioException("El rol '" + rolCodigo + "' no existe. Use ENC, DOC, AUX o DIR.");
        }

        Usuario u = new Usuario();
        u.setCodigo(codigo);
        u.setNombre(nombre);
        u.setApellido(apellido);
        u.setCorreo(correo);
        u.setTelefono(telefono.isEmpty() ? null : telefono);
        u.setRolId(rol.getId());
        u.setRolCodigo(rol.getCodigo());
        return u;
    }

    private Usuario buscarExistente(String codigo) throws NegocioException, SQLException {
        Usuario u = usuarioDAO.buscarPorCodigo(codigo);
        if (u == null) {
            throw new NegocioException("No se encontró ningún usuario con el código " + codigo + ".");
        }
        return u;
    }

    private static String requerido(String valor, String campo, int max) throws NegocioException {
        if (valor.isEmpty()) {
            throw new NegocioException("El campo " + campo + " es obligatorio.");
        }
        if (valor.length() > max) {
            throw new NegocioException("El campo " + campo + " admite como máximo " + max + " caracteres.");
        }
        return valor;
    }

    private static Resultado tablaDeUno(String titulo, String mensaje, Usuario u) {
        return Resultado.tabla(titulo, mensaje, COLUMNAS, List.<String[]>of(fila(u)));
    }

    private static String[] fila(Usuario u) {
        return new String[]{u.getCodigo(), u.getNombre(), u.getApellido(), u.getCorreo(),
                u.getTelefono() == null ? "" : u.getTelefono(), u.getRolNombre(), u.getEstado()};
    }
}
