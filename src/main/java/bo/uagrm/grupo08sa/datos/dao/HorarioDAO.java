package bo.uagrm.grupo08sa.datos.dao;

import bo.uagrm.grupo08sa.datos.Fila;

import java.sql.SQLException;
import java.time.LocalTime;
import java.util.List;

public class HorarioDAO extends BaseDAO {

    private static final String SELECT_BASE =
            "SELECT h.id, h.semestre_id, h.laboratorio_id, l.codigo AS lab, h.materia_id, m.sigla, "
            + "m.nombre AS materia, h.grupo, h.dia_semana, h.hora_inicio, h.hora_fin, h.docente_id, "
            + "u.codigo AS docente_codigo, u.nombre || ' ' || u.apellido AS docente "
            + "FROM horario_clase h JOIN laboratorio l ON l.id = h.laboratorio_id "
            + "JOIN materia m ON m.id = h.materia_id JOIN usuario u ON u.id = h.docente_id ";

    public int insertar(int semestreId, int laboratorioId, int materiaId, int docenteId, String grupo,
                        int dia, LocalTime inicio, LocalTime fin) throws SQLException {
        return insertar("INSERT INTO horario_clase (semestre_id, laboratorio_id, materia_id, docente_id, grupo, "
                + "dia_semana, hora_inicio, hora_fin) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                semestreId, laboratorioId, materiaId, docenteId, grupo, dia, inicio, fin);
    }

    public void actualizar(int id, int laboratorioId, int materiaId, int docenteId, String grupo,
                           int dia, LocalTime inicio, LocalTime fin) throws SQLException {
        ejecutar("UPDATE horario_clase SET laboratorio_id = ?, materia_id = ?, docente_id = ?, grupo = ?, "
                + "dia_semana = ?, hora_inicio = ?, hora_fin = ? WHERE id = ?",
                laboratorioId, materiaId, docenteId, grupo, dia, inicio, fin, id);
    }

    public void eliminar(int id) throws SQLException {
        ejecutar("DELETE FROM horario_clase WHERE id = ?", id);
    }

    public Fila buscarPorId(int id) throws SQLException {
        return consultarUno(SELECT_BASE + "WHERE h.id = ?", id);
    }

    /** Clase del mismo semestre, laboratorio y día que se superpone con [inicio, fin). excluirId = 0 si no aplica. */
    public Fila choque(int semestreId, int laboratorioId, int dia, LocalTime inicio, LocalTime fin, int excluirId)
            throws SQLException {
        return consultarUno(SELECT_BASE + "WHERE h.semestre_id = ? AND h.laboratorio_id = ? AND h.dia_semana = ? "
                + "AND h.hora_inicio < ? AND h.hora_fin > ? AND h.id <> ? LIMIT 1",
                semestreId, laboratorioId, dia, fin, inicio, excluirId);
    }

    public List<Fila> listar(int semestreId, Integer laboratorioId, Integer dia) throws SQLException {
        return consultar(SELECT_BASE + "WHERE h.semestre_id = ? "
                + "AND (CAST(? AS INTEGER) IS NULL OR h.laboratorio_id = ?) "
                + "AND (CAST(? AS INTEGER) IS NULL OR h.dia_semana = ?) "
                + "ORDER BY h.dia_semana, h.hora_inicio, l.codigo",
                semestreId, laboratorioId, laboratorioId, dia, dia);
    }
}
