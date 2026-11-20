package bo.uagrm.grupo08sa.negocio.controladores;

import bo.uagrm.grupo08sa.datos.dao.ComandoDAO;
import bo.uagrm.grupo08sa.datos.modelo.Usuario;
import bo.uagrm.grupo08sa.negocio.Comando;
import bo.uagrm.grupo08sa.negocio.Manejador;
import bo.uagrm.grupo08sa.negocio.Resultado;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * AYUDA["*"]: lista los comandos que el rol del remitente puede ejecutar.
 */
public class AyudaController {

    private final ComandoDAO comandoDAO = new ComandoDAO();

    public void registrarEn(Map<String, Manejador> manejadores) {
        manejadores.put("AYUDA", this::ayuda);
    }

    public Resultado ayuda(Comando comando, Usuario solicitante) throws SQLException {
        List<String[]> filas = comandoDAO.listarTodosConRoles();
        return Resultado.tabla("Comandos del Sistema",
                "Lista completa de comandos clasificados por roles permitidos (ENC: Encargado, DOC: Docente, AUX: Auxiliar, DIR: Director). "
                        + "Formato: COMANDO[\"param1\",\"param2\"] o COMANDO[\"*\"].",
                new String[]{"Comando", "Roles", "Descripción"}, filas);
    }
}
