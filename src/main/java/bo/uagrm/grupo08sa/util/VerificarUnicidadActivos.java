package bo.uagrm.grupo08sa.util;

import java.io.File;
import java.util.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VerificarUnicidadActivos {
    public static void main(String[] args) throws Exception {
        File file = new File("c:\\Users\\JAIRO\\Desktop\\Activos FICCT.pdf");
        Pattern pAssetStart = Pattern.compile("^(U[0-9A-Za-z]{9,15})\\b");
        Pattern pHeader = Pattern.compile("^Activo\\s+Grupo\\s+Estado\\s+Fecha", Pattern.CASE_INSENSITIVE);
        Pattern stateDate = Pattern.compile("\\b(BUENO|REGULAR|MALO|BUEN|NUEVO)\\s+(\\d{2}/\\d{2}/\\d{4})\\b");

        Map<String, String> activosVistos = new HashMap<>();
        int duplicados = 0;
        int total = 0;

        try (PDDocument doc = PDDocument.load(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            StringBuilder currentRow = new StringBuilder();

            for (int p = 1; p <= doc.getNumberOfPages(); p++) {
                stripper.setStartPage(p);
                stripper.setEndPage(p);
                String[] lines = stripper.getText(doc).split("\\r?\\n");
                for (String line : lines) {
                    line = line.trim();
                    if (line.isEmpty() || pHeader.matcher(line).find()) continue;
                    Matcher m = pAssetStart.matcher(line);
                    if (m.find()) {
                        if (currentRow.length() > 0) {
                            String r = currentRow.toString();
                            String upper = r.toUpperCase();
                            String lab = obtenerLab(upper);
                            if (lab != null) {
                                total++;
                                Matcher ma = pAssetStart.matcher(r);
                                if (ma.find()) {
                                    String act = ma.group(1);
                                    if (activosVistos.containsKey(act)) {
                                        duplicados++;
                                        System.out.println("Duplicado: " + act + " en " + lab + " y " + activosVistos.get(act));
                                    } else {
                                        activosVistos.put(act, lab);
                                    }
                                }
                            }
                            currentRow.setLength(0);
                        }
                    }
                    if (currentRow.length() > 0) currentRow.append(" ");
                    currentRow.append(line);
                }
            }
            if (currentRow.length() > 0) {
                String r = currentRow.toString();
                String lab = obtenerLab(r.toUpperCase());
                if (lab != null) {
                    total++;
                    Matcher ma = pAssetStart.matcher(r);
                    if (ma.find()) {
                        String act = ma.group(1);
                        if (activosVistos.containsKey(act)) duplicados++;
                        else activosVistos.put(act, lab);
                    }
                }
            }
        }
        System.out.printf("Total procesados: %d, Unicos: %d, Duplicados: %d%n",
                total, activosVistos.size(), duplicados);
    }

    private static String obtenerLab(String upper) {
        if (upper.contains("236 41") || upper.contains("236-41")) return "236-41";
        if (upper.contains("236 42") || upper.contains("236-42")) return "236-42";
        if (upper.contains("236 43") || upper.contains("236-43")) return "236-43";
        if (upper.contains("236 44") || upper.contains("236-44")) return "236-44";
        if (upper.contains("236 45") || upper.contains("236-45")) return "236-45";
        if (upper.contains("236 46") || upper.contains("236-46")) return "236-46";
        if (upper.contains("236 47") || upper.contains("236-47")) return "236-47";
        if (upper.contains("236 48") || upper.contains("236-48")) return "236-48";
        return null;
    }
}

