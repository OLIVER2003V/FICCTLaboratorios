package bo.uagrm.grupo08sa.datos.dao;

import bo.uagrm.grupo08sa.datos.Fila;

import java.sql.SQLException;
import java.util.List;

public class EquipoRedDAO extends BaseDAO {

    private static final String SELECT_BASE =
            "SELECT e.codigo, l.codigo AS lab, er.ip, er.mascara, er.gateway, er.dns1, er.dns2, er.proxy, er.grupo_red "
            + "FROM equipo_red er "
            + "JOIN equipo e ON e.id = er.equipo_id "
            + "JOIN laboratorio l ON l.id = e.laboratorio_id ";

    public List<Fila> listar(Integer laboratorioId) throws SQLException {
        return consultar(SELECT_BASE + "WHERE (CAST(? AS INTEGER) IS NULL OR e.laboratorio_id = ?) "
                + "ORDER BY "
                + "CASE WHEN TRIM(e.codigo) ~ '^[0-9]+$' THEN 0 ELSE 1 END, "
                + "CASE WHEN TRIM(e.codigo) ~ '^[0-9]+$' THEN TRIM(e.codigo)::numeric ELSE NULL END, "
                + "LENGTH(TRIM(e.codigo)) ASC, TRIM(e.codigo) ASC", laboratorioId, laboratorioId);
    }
}

