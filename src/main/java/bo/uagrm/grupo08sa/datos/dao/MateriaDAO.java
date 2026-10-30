package bo.uagrm.grupo08sa.datos.dao;

import bo.uagrm.grupo08sa.datos.Fila;

import java.sql.SQLException;

public class MateriaDAO extends BaseDAO {

    public Fila buscarPorSigla(String sigla) throws SQLException {
        return consultarUno("SELECT * FROM materia WHERE UPPER(sigla) = UPPER(?)", sigla);
    }

    public int insertar(String sigla, String nombre) throws SQLException {
        return insertar("INSERT INTO materia (sigla, nombre) VALUES (?, ?)", sigla, nombre);
    }
}
