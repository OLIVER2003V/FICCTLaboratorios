package bo.uagrm.grupo08sa.datos.dao;

import bo.uagrm.grupo08sa.datos.Fila;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class SemestreDAO extends BaseDAO {

    public Fila buscarPorGestion(String gestion) throws SQLException {
        return consultarUno("SELECT * FROM semestre WHERE UPPER(gestion) = UPPER(?)", gestion);
    }

    public Fila obtenerActivo() throws SQLException {
        return consultarUno("SELECT * FROM semestre WHERE estado = 'ACTIVO'");
    }

    /** Otro semestre cuyas fechas se superponen con [inicio, fin]. */
    public Fila superpuesto(LocalDate inicio, LocalDate fin) throws SQLException {
        return consultarUno("SELECT * FROM semestre WHERE fecha_inicio <= ? AND fecha_fin >= ? LIMIT 1", fin, inicio);
    }

    /** Semestre no cerrado que contiene la fecha. */
    public Fila contenedor(LocalDate fecha) throws SQLException {
        return consultarUno("SELECT * FROM semestre WHERE ? BETWEEN fecha_inicio AND fecha_fin "
                + "AND estado <> 'CERRADO' ORDER BY id DESC LIMIT 1", fecha);
    }

    public int insertar(String gestion, LocalDate inicio, LocalDate fin) throws SQLException {
        return insertar("INSERT INTO semestre (gestion, fecha_inicio, fecha_fin) VALUES (?, ?, ?)", gestion, inicio, fin);
    }

    public void cambiarEstado(int id, String estado) throws SQLException {
        ejecutar("UPDATE semestre SET estado = ? WHERE id = ?", estado, id);
    }

    public List<Fila> listar() throws SQLException {
        return consultar("SELECT * FROM semestre ORDER BY fecha_inicio DESC");
    }
}
