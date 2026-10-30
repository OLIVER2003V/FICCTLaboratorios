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
        List<String[]> filas = comandoDAO.listarPorRol(solicitante.getRolId());
        return Resultado.tabla("Comandos disponibles",
                "Hola " + solicitante.getNombreCompleto() + " (" + solicitante.getRolNombre() + "). "
                        + "Escriba el comando en el asunto con el formato COMANDO[\"param1\",\"param2\"]; "
                        + "si no lleva parámetros use COMANDO[\"*\"].",
                new String[]{"Comando", "Descripción"}, filas);
    }
}
