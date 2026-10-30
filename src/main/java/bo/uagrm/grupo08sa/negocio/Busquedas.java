package bo.uagrm.grupo08sa.negocio;

import bo.uagrm.grupo08sa.datos.Fila;
import bo.uagrm.grupo08sa.datos.dao.LaboratorioDAO;
import bo.uagrm.grupo08sa.datos.dao.PermutaDAO;
import bo.uagrm.grupo08sa.datos.dao.SemestreDAO;
import bo.uagrm.grupo08sa.datos.dao.TurnoDAO;
import bo.uagrm.grupo08sa.datos.dao.UsuarioDAO;
import bo.uagrm.grupo08sa.datos.modelo.Usuario;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Búsquedas que usan varios controladores y que fallan con un mensaje claro si no encuentran el dato.
 */
public class Busquedas {

    private final LaboratorioDAO laboratorioDAO = new LaboratorioDAO();
    private final SemestreDAO semestreDAO = new SemestreDAO();
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private final TurnoDAO turnoDAO = new TurnoDAO();
    private final PermutaDAO permutaDAO = new PermutaDAO();

    public Fila laboratorio(String codigo) throws NegocioException, SQLException {
        Fila lab = laboratorioDAO.buscarPorCodigo(codigo.trim());
        if (lab == null) {
            throw new NegocioException("No existe el laboratorio '" + codigo + "'. Envíe LISLAB[\"*\"] para ver los códigos.");
        }
        return lab;
    }

    public Fila semestreActivo() throws NegocioException, SQLException {
        Fila sem = semestreDAO.obtenerActivo();
        if (sem == null) {
            throw new NegocioException("No hay un semestre activo. El Encargado debe registrarlo (INSSEM) y activarlo (ACTSEM).");
        }
        return sem;
    }

    public void fechaEnSemestre(Fila semestre, LocalDate fecha) throws NegocioException {
        if (fecha.isBefore(semestre.fecha("fecha_inicio")) || fecha.isAfter(semestre.fecha("fecha_fin"))) {
            throw new NegocioException("La fecha " + fecha + " está fuera del semestre activo "
                    + semestre.texto("gestion") + " (" + semestre.texto("fecha_inicio") + " a "
                    + semestre.texto("fecha_fin") + ").");
        }
    }

    /** Usuario por código o correo. */
    public Usuario usuario(String codigoOCorreo) throws NegocioException, SQLException {
        Usuario u = usuarioDAO.buscarPorCodigoOCorreo(codigoOCorreo.trim());
        if (u == null) {
            throw new NegocioException("No existe ningún usuario con código o correo '" + codigoOCorreo + "'.");
        }
        return u;
    }

    /** Usuario activo con el rol indicado (ej. "AUX" para auxiliares, "DOC" para docentes). */
    public Usuario usuarioActivoConRol(String codigoOCorreo, String rol, String descripcion)
            throws NegocioException, SQLException {
        Usuario u = usuario(codigoOCorreo);
        if (!rol.equals(u.getRolCodigo())) {
            throw new NegocioException(u.getNombreCompleto() + " no es " + descripcion + " (su rol es "
                    + u.getRolNombre() + ").");
        }
        if (!u.estaActivo()) {
            throw new NegocioException(u.getNombreCompleto() + " está dado de baja.");
        }
        return u;
    }

    /** Quién cubre el turno en esa fecha: el reemplazante si hay una permuta aprobada, si no el titular. */
    public int responsableDeTurno(Fila turno, LocalDate fecha) throws SQLException {
        Fila permuta = permutaDAO.aprobada(turno.entero("id"), fecha);
        return permuta != null ? permuta.entero("reemplazante_id") : turno.entero("auxiliar_id");
    }

    /** Auxiliares que están de turno en ese laboratorio, fecha y franja (incluye permutas aprobadas). */
    public Set<Integer> auxiliaresDeTurno(int laboratorioId, LocalDate fecha, LocalTime inicio, LocalTime fin)
            throws SQLException {
        Set<Integer> ids = new LinkedHashSet<>();
        Fila sem = semestreDAO.obtenerActivo();
        int dia = fecha.getDayOfWeek().getValue();
        if (sem == null || dia == 7) {
            return ids;
        }
        for (Fila t : turnoDAO.delDia(sem.entero("id"), dia)) {
            Integer labTurno = t.enteroONulo("laboratorio_id");
            boolean mismoLab = labTurno == null || labTurno == laboratorioId;
            boolean seSuperpone = t.hora("hora_inicio").isBefore(fin) && t.hora("hora_fin").isAfter(inicio);
            if (mismoLab && seSuperpone) {
                ids.add(responsableDeTurno(t, fecha));
            }
        }
        return ids;
    }
}
