package bo.uagrm.grupo08sa.negocio;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Lee el horario adjunto a CARHOR (.xlsx o .csv) sin librerías externas.
 * Un .xlsx es un ZIP con XML: xl/sharedStrings.xml (textos) y xl/worksheets/sheet1.xml (celdas).
 *
 * Columnas esperadas (la primera fila puede ser el encabezado):
 *   LAB | DIA | INICIO | FIN | SIGLA | MATERIA | GRUPO | DOCENTE (código o correo)
 */
public final class LectorHorario {

    public static final int COLUMNAS = 8;
    private static final int COL_INICIO = 2;
    private static final int COL_FIN = 3;

    /** Fila del archivo: número de fila (para los mensajes) y sus 8 valores. */
    public record FilaHorario(int numero, String[] valores) {
    }

    private LectorHorario() {
    }

    public static List<FilaHorario> leer(Adjunto adjunto) throws NegocioException {
        String nombre = adjunto.nombre().toLowerCase(Locale.ROOT);
        List<String[]> filas;
        try {
            if (nombre.endsWith(".xlsx")) {
                filas = leerXlsx(adjunto.datos());
            } else if (nombre.endsWith(".csv") || nombre.endsWith(".txt")) {
                filas = leerCsv(adjunto.datos());
            } else {
                throw new NegocioException("El archivo '" + adjunto.nombre() + "' no es válido. Adjunte un .xlsx o .csv.");
            }
        } catch (IOException | RuntimeException e) {
            throw new NegocioException("No se pudo leer el archivo '" + adjunto.nombre() + "': formato inválido.");
        }

        List<FilaHorario> resultado = new ArrayList<>();
        for (int i = 0; i < filas.size(); i++) {
            String[] f = filas.get(i);
            boolean vacia = true;
            for (String v : f) {
                vacia &= v.isBlank();
            }
            boolean encabezado = i == 0 && f[0].trim().toUpperCase(Locale.ROOT).startsWith("LAB");
            if (!vacia && !encabezado) {
                resultado.add(new FilaHorario(i + 1, f));
            }
        }
        if (resultado.isEmpty()) {
            throw new NegocioException("El archivo no tiene filas de horario. Columnas esperadas: "
                    + "LAB, DIA, INICIO, FIN, SIGLA, MATERIA, GRUPO, DOCENTE.");
        }
        return resultado;
    }

    private static List<String[]> leerCsv(byte[] datos) {
        String texto = new String(datos, StandardCharsets.UTF_8).replace("﻿", "");
        String[] lineas = texto.split("\r?\n");
        String separador = lineas.length > 0 && lineas[0].contains(";") ? ";" : ",";
        List<String[]> filas = new ArrayList<>();
        for (String linea : lineas) {
            String[] partes = linea.split(separador, -1);
            String[] fila = new String[COLUMNAS];
            for (int i = 0; i < COLUMNAS; i++) {
                fila[i] = i < partes.length ? partes[i].trim().replaceAll("^\"|\"$", "") : "";
            }
            filas.add(fila);
        }
        return filas;
    }

    private static List<String[]> leerXlsx(byte[] datos) throws IOException {
        Map<String, byte[]> entradas = new HashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(datos))) {
            ZipEntry e;
            while ((e = zip.getNextEntry()) != null) {
                if (e.getName().startsWith("xl/")) {
                    entradas.put(e.getName(), zip.readAllBytes());
                }
            }
        }
        List<String> textos = new ArrayList<>();
        if (entradas.containsKey("xl/sharedStrings.xml")) {
            NodeList items = xml(entradas.get("xl/sharedStrings.xml")).getElementsByTagName("si");
            for (int i = 0; i < items.getLength(); i++) {
                textos.add(items.item(i).getTextContent());
            }
        }
        byte[] hoja = entradas.get("xl/worksheets/sheet1.xml");
        if (hoja == null) {
            hoja = entradas.entrySet().stream().filter(x -> x.getKey().startsWith("xl/worksheets/sheet"))
                    .map(Map.Entry::getValue).findFirst().orElseThrow(() -> new IOException("sin hojas"));
        }

        TreeMap<Integer, String[]> filas = new TreeMap<>();
        NodeList celdas = xml(hoja).getElementsByTagName("c");
        for (int i = 0; i < celdas.getLength(); i++) {
            Element c = (Element) celdas.item(i);
            String ref = c.getAttribute("r");                 // ej. "C5"
            int columna = columna(ref);
            int fila = Integer.parseInt(ref.replaceAll("[A-Z]", ""));
            if (columna >= COLUMNAS) {
                continue;
            }
            String tipo = c.getAttribute("t");
            String valor;
            if (tipo.equals("inlineStr")) {
                valor = c.getTextContent();
            } else {
                NodeList v = c.getElementsByTagName("v");
                valor = v.getLength() == 0 ? "" : v.item(0).getTextContent();
                if (tipo.equals("s")) {
                    valor = textos.get(Integer.parseInt(valor));
                } else if (!tipo.equals("str") && !valor.isEmpty()) {
                    valor = numero(valor, columna);
                }
            }
            filas.computeIfAbsent(fila, k -> vacia())[columna] = valor.trim();
        }
        return new ArrayList<>(filas.values());
    }

    /** Excel guarda las horas como fracción del día (0.3333 = 08:00) y los códigos como números. */
    private static String numero(String valor, int columna) {
        BigDecimal n = new BigDecimal(valor);
        if ((columna == COL_INICIO || columna == COL_FIN) && n.compareTo(BigDecimal.ONE) < 0) {
            int minutos = (int) Math.round(n.doubleValue() * 24 * 60);
            return String.format("%02d:%02d", minutos / 60, minutos % 60);
        }
        return n.stripTrailingZeros().toPlainString();
    }

    private static int columna(String referencia) {
        int col = 0;
        for (char ch : referencia.toCharArray()) {
            if (!Character.isLetter(ch)) {
                break;
            }
            col = col * 26 + (ch - 'A' + 1);
        }
        return col - 1;
    }

    private static String[] vacia() {
        String[] f = new String[COLUMNAS];
        java.util.Arrays.fill(f, "");
        return f;
    }

    private static Document xml(byte[] datos) throws IOException {
        try {
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            // Protección contra XXE: el archivo viene de un correo externo
            f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            f.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            f.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            DocumentBuilder b = f.newDocumentBuilder();
            return b.parse(new ByteArrayInputStream(datos));
        } catch (Exception e) {
            throw new IOException("XML inválido", e);
        }
    }
}
