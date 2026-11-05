package bo.uagrm.grupo08sa.util;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestExtraccionLimpia {

    public static class ActivoItem {
        public String activo;
        public String grupo;
        public String estado; // Original from PDF
        public String fecha;
        public String ubicacion;
        public String labNormalizado; // e.g. "236-41"
    }

    public static void main(String[] args) throws Exception {
        File file = new File("c:\\Users\\JAIRO\\Desktop\\Activos FICCT.pdf");
        List<String> rawRows = new ArrayList<>();

        Pattern pAssetStart = Pattern.compile("^(U[0-9A-Za-z]{9,15})\\b");
        Pattern pHeader = Pattern.compile("^Activo\\s+Grupo\\s+Estado\\s+Fecha", Pattern.CASE_INSENSITIVE);

        try (PDDocument doc = PDDocument.load(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            int totalPages = doc.getNumberOfPages();

            StringBuilder currentRow = new StringBuilder();

            for (int p = 1; p <= totalPages; p++) {
                stripper.setStartPage(p);
                stripper.setEndPage(p);
                String pageText = stripper.getText(doc);
                String[] lines = pageText.split("\\r?\\n");

                for (String line : lines) {
                    line = line.trim();
                    if (line.isEmpty() || pHeader.matcher(line).find()) {
                        continue;
                    }

                    Matcher m = pAssetStart.matcher(line);
                    if (m.find()) {
                        // Start of new asset row
                        if (currentRow.length() > 0) {
                            rawRows.add(currentRow.toString());
                            currentRow.setLength(0);
                        }
                    }
                    if (currentRow.length() > 0) {
                        currentRow.append(" ");
                    }
                    currentRow.append(line);
                }
            }
            if (currentRow.length() > 0) {
                rawRows.add(currentRow.toString());
            }

            System.out.println("Total activos extraídos del PDF completo: " + rawRows.size());

            // Pattern for Estado and Fecha: BUENO|REGULAR|MALO|BUEN|NUEVO followed by dd/mm/yyyy
            Pattern stateDate = Pattern.compile("\\b(BUENO|REGULAR|MALO|BUEN|NUEVO)\\s+(\\d{2}/\\d{2}/\\d{4})\\b");

            List<ActivoItem> items4to = new ArrayList<>();
            Map<String, Integer> porLab = new TreeMap<>();
            Map<String, Integer> gruposLab = new TreeMap<>();

            for (String row : rawRows) {
                String upper = row.toUpperCase();
                String lab = null;

                if (upper.contains("236 41") || upper.contains("236-41")) {
                    lab = "236-41";
                } else if (upper.contains("236 42") || upper.contains("236-42")) {
                    lab = "236-42";
                } else if (upper.contains("236 43") || upper.contains("236-43")) {
                    lab = "236-43";
                } else if (upper.contains("236 44") || upper.contains("236-44")) {
                    lab = "236-44";
                } else if (upper.contains("236 45") || upper.contains("236-45")) {
                    lab = "236-45";
                } else if (upper.contains("236 46") || upper.contains("236-46")) {
                    lab = "236-46";
                } else if (upper.contains("236 47") || upper.contains("236-47")) {
                    lab = "236-47";
                } else if (upper.contains("236 48") || upper.contains("236-48")) {
                    lab = "236-48";
                } else if (upper.contains("JEF. LAB. FICCT 4TO") || upper.contains("JEFATURA")) {
                    lab = "JEF-LAB";
                } else if (upper.contains("AUXILIARES DE LABORATORIO 4TO")) {
                    lab = "AUX-4TO";
                } else if (upper.contains("LABORATORIO 4TO. PISO") || upper.contains("LABORATORIO 4TO PISO")) {
                    lab = "LAB-4TO-GEN";
                }

                if (lab != null) {
                    Matcher mAsset = pAssetStart.matcher(row);
                    if (!mAsset.find()) continue;
                    String activo = mAsset.group(1);

                    Matcher mSD = stateDate.matcher(row);
                    String grupo = "";
                    String estado = "BUENO";
                    String fecha = "";
                    String ubic = "";

                    if (mSD.find()) {
                        grupo = row.substring(activo.length(), mSD.start(1)).trim();
                        estado = mSD.group(1);
                        fecha = mSD.group(2);
                        ubic = row.substring(mSD.end(2)).trim();
                    } else {
                        String[] p = row.split("\\s+");
                        grupo = p.length > 1 ? p[1] : "EQUIPO";
                    }

                    // Clean Grupo: normalize internal spaces
                    grupo = grupo.replaceAll("\\s+", " ").trim();

                    ActivoItem item = new ActivoItem();
                    item.activo = activo;
                    item.grupo = grupo;
                    item.estado = estado;
                    item.fecha = fecha;
                    item.ubicacion = ubic;
                    item.labNormalizado = lab;

                    items4to.add(item);
                    porLab.put(lab, porLab.getOrDefault(lab, 0) + 1);
                    gruposLab.put(grupo, gruposLab.getOrDefault(grupo, 0) + 1);
                }
            }

            System.out.println("Activos 4to piso extraídos: " + items4to.size());
            System.out.println("\nConteo por laboratorio:");
            for (Map.Entry<String, Integer> e : porLab.entrySet()) {
                System.out.println("  " + e.getKey() + ": " + e.getValue());
            }

            System.out.println("\nGrupos encontrados (limpios):");
            for (Map.Entry<String, Integer> e : gruposLab.entrySet()) {
                System.out.println("  '" + e.getKey() + "': " + e.getValue());
            }

            // Check U147124117689 specifically
            for (ActivoItem it : items4to) {
                if (it.activo.equals("U147124117689")) {
                    System.out.println("\nVerificación U147124117689:");
                    System.out.println("  Activo: " + it.activo);
                    System.out.println("  Grupo: " + it.grupo);
                    System.out.println("  Lab: " + it.labNormalizado);
                }
            }
        }
    }
}

