package bo.uagrm.grupo08sa.datos.dao;

import bo.uagrm.grupo08sa.datos.Fila;

import java.sql.SQLException;
import java.util.List;

public class LaboratorioDAO extends BaseDAO {

    public Fila buscarPorCodigo(String codigo) throws SQLException {
        return consultarUno("SELECT * FROM laboratorio WHERE UPPER(codigo) = UPPER(?)", codigo);
    }

    public Fila buscarPorId(int id) throws SQLException {
        return consultarUno("SELECT * FROM laboratorio WHERE id = ?", id);
    }

    public int insertar(String codigo, String nombre, int capacidad, String ubicacion) throws SQLException {
        return insertar("INSERT INTO laboratorio (codigo, nombre, capacidad, ubicacion) VALUES (?, ?, ?, ?)",
                codigo, nombre, capacidad, ubicacion);
    }

    public void actualizar(int id, String nombre, int capacidad, String ubicacion, String estado) throws SQLException {
        ejecutar("UPDATE laboratorio SET nombre = ?, capacidad = ?, ubicacion = ?, estado = ? WHERE id = ?",
                nombre, capacidad, ubicacion, estado, id);
    }

    public List<Fila> listar(String estado) throws SQLException {
        return consultar("SELECT * FROM laboratorio WHERE (CAST(? AS VARCHAR) IS NULL OR estado = ?) ORDER BY codigo",
                estado, estado);
    }
}
