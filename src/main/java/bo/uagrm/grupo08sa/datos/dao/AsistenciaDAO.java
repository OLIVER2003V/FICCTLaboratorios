package bo.uagrm.grupo08sa.datos.dao;

import bo.uagrm.grupo08sa.datos.Fila;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class AsistenciaDAO extends BaseDAO {

    private static final String SELECT_BASE =
            "SELECT a.*, t.dia_semana, t.hora_inicio, t.hora_fin, l.codigo AS lab, "
            + "u.codigo AS auxiliar_codigo, u.nombre || ' ' || u.apellido AS auxiliar "
            + "FROM asistencia a JOIN turno t ON t.id = a.turno_id LEFT JOIN laboratorio l ON l.id = t.laboratorio_id "
            + "JOIN usuario u ON u.id = a.auxiliar_id ";

    public Fila buscar(int turnoId, LocalDate fecha) throws SQLException {
        return consultarUno(SELECT_BASE + "WHERE a.turno_id = ? AND a.fecha = ?", turnoId, fecha);
    }

    public int insertar(int turnoId, int auxiliarId, LocalDate fecha, LocalTime entrada, String estado)
            throws SQLException {
        return insertar("INSERT INTO asistencia (turno_id, auxiliar_id, fecha, hora_entrada, estado) VALUES (?, ?, ?, ?, ?)",
                turnoId, auxiliarId, fecha, entrada, estado);
    }

    public void registrarSalida(int id, LocalTime salida) throws SQLException {
        ejecutar("UPDATE asistencia SET hora_salida = ? WHERE id = ?", salida, id);
    }

    /** Registro del día con entrada y sin salida. */
    public Fila abierta(int auxiliarId, LocalDate fecha) throws SQLException {
        return consultarUno(SELECT_BASE + "WHERE a.auxiliar_id = ? AND a.fecha = ? AND a.hora_entrada IS NOT NULL "
                + "AND a.hora_salida IS NULL ORDER BY t.hora_inicio LIMIT 1", auxiliarId, fecha);
    }

    /** Convierte en PERMISO las faltas ya registradas de ese auxiliar en esa fecha. */
    public int justificarFaltas(int auxiliarId, LocalDate fecha) throws SQLException {
        return ejecutar("UPDATE asistencia SET estado = 'PERMISO' WHERE auxiliar_id = ? AND fecha = ? AND estado = 'FALTA'",
                auxiliarId, fecha);
    }

    public List<Fila> listar(Integer auxiliarId, LocalDate desde, LocalDate hasta) throws SQLException {
        return consultar(SELECT_BASE + "WHERE (CAST(? AS INTEGER) IS NULL OR a.auxiliar_id = ?) "
                + "AND a.fecha BETWEEN ? AND ? ORDER BY a.fecha, t.hora_inicio, u.apellido",
                auxiliarId, auxiliarId, desde, hasta);
    }
}
