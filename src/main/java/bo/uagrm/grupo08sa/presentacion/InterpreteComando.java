package bo.uagrm.grupo08sa.presentacion;

import bo.uagrm.grupo08sa.negocio.Adjunto;
import bo.uagrm.grupo08sa.negocio.Comando;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Convierte el asunto del correo en un Comando.
 * Formato: COMANDO["p1","p2",...]; sin parámetros: COMANDO["*"].
 */
public final class InterpreteComando {

    private static final Pattern FORMATO = Pattern.compile("^([A-Za-z]{2,10})\\s*\\[(.*)\\]$", Pattern.DOTALL);
    private static final Pattern PREFIJO_RESPUESTA =
            Pattern.compile("^((re|rv|fw|fwd|reenviar)\\s*:\\s*)+", Pattern.CASE_INSENSITIVE);
    private static final String AYUDA_FORMATO =
            " El asunto debe tener el formato COMANDO[\"param1\",\"param2\"] o COMANDO[\"*\"]. "
            + "Envíe AYUDA[\"*\"] para ver sus comandos.";

    private InterpreteComando() {
    }

    public static Comando analizar(String asunto, String remitente, Adjunto adjunto) throws FormatoInvalidoException {
        String texto = normalizar(asunto);
        Matcher m = FORMATO.matcher(texto);
        if (!m.matches()) {
            throw new FormatoInvalidoException("No se reconoce el asunto '" + asunto + "'." + AYUDA_FORMATO);
        }
        String nombre = m.group(1).toUpperCase(Locale.ROOT);
        List<String> parametros = separarParametros(m.group(2), asunto);
        if (parametros.size() == 1 && parametros.get(0).trim().equals("*")) {
            parametros = List.of();
        }
        return new Comando(nombre, parametros, remitente, asunto, adjunto);
    }

    /** Quita "Re:"/"Fwd:" y reemplaza las comillas tipográficas que ponen algunos celulares. */
    private static String normalizar(String asunto) {
        String texto = asunto == null ? "" : asunto.replace("﻿", "").trim();
        texto = PREFIJO_RESPUESTA.matcher(texto).replaceFirst("");
        return texto.replaceAll("[“”„‟″«»]", "\"").trim();
    }

    private static List<String> separarParametros(String interior, String asunto) throws FormatoInvalidoException {
        List<String> parametros = new ArrayList<>();
        int i = 0;
        int n = interior.length();
        i = saltarEspacios(interior, i);
        if (i == n) {
            return parametros;  // COMANDO[]
        }
        while (true) {
            if (interior.charAt(i) != '"') {
                throw new FormatoInvalidoException("Cada parámetro debe ir entre comillas dobles: '" + asunto + "'."
                        + AYUDA_FORMATO);
            }
            int cierre = interior.indexOf('"', i + 1);
            if (cierre < 0) {
                throw new FormatoInvalidoException("Falta cerrar una comilla en '" + asunto + "'." + AYUDA_FORMATO);
            }
            parametros.add(interior.substring(i + 1, cierre));
            i = saltarEspacios(interior, cierre + 1);
            if (i == n) {
                return parametros;
            }
            if (interior.charAt(i) != ',') {
                throw new FormatoInvalidoException("Los parámetros deben separarse con comas: '" + asunto + "'."
                        + AYUDA_FORMATO);
            }
            i = saltarEspacios(interior, i + 1);
            if (i == n) {
                throw new FormatoInvalidoException("Sobra una coma al final de '" + asunto + "'." + AYUDA_FORMATO);
            }
        }
    }

    private static int saltarEspacios(String s, int i) {
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) {
            i++;
        }
        return i;
    }
}
