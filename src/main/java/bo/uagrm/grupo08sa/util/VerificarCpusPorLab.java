package bo.uagrm.grupo08sa.util;

import java.io.File;
import java.util.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VerificarCpusPorLab {
    public static void main(String[] args) throws Exception {
        File file = new File("c:\\Users\\JAIRO\\Desktop\\Activos FICCT.pdf");
        Pattern pAssetStart = Pattern.compile("^(U[0-9A-Za-z]{9,15})\\b");
        Pattern pHeader = Pattern.compile("^Activo\\s+Grupo\\s+Estado\\s+Fecha", Pattern.CASE_INSENSITIVE);
        Pattern stateDate = Pattern.compile("\\b(BUENO|REGULAR|MALO|BUEN|NUEVO)\\s+(\\d{2}/\\d{2}/\\d{4})\\b");

        Map<String, List<String>> cpusPorLab = new TreeMap<>();

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
                            procesarFila(currentRow.toString(), pAssetStart, stateDate, cpusPorLab);
                            currentRow.setLength(0);
                        }
                    }
                    if (currentRow.length() > 0) currentRow.append(" ");
                    currentRow.append(line);
                }
            }
            if (currentRow.length() > 0) {
                procesarFila(currentRow.toString(), pAssetStart, stateDate, cpusPorLab);
            }
        }

        System.out.println("CPUs por Laboratorio:");
        for (Map.Entry<String, List<String>> e : cpusPorLab.entrySet()) {
            List<String> list = e.getValue();
            Collections.sort(list);
            System.out.println("\nLAB " + e.getKey() + ": " + list.size() + " CPUs");
            System.out.println("  Docente (menor): " + list.get(0));
            if (list.size() > 1) {
                System.out.println("  PC-01: " + list.get(1));
                System.out.println("  PC-" + String.format("%02d", list.size() - 1) + " (último): " + list.get(list.size() - 1));
            }
        }
    }

    private static void procesarFila(String row, Pattern pAssetStart, Pattern stateDate, Map<String, List<String>> cpusPorLab) {
        String upper = row.toUpperCase();
        String lab = null;
        if (upper.contains("236 41") || upper.contains("236-41")) lab = "236-41";
        else if (upper.contains("236 42") || upper.contains("236-42")) lab = "236-42";
        else if (upper.contains("236 43") || upper.contains("236-43")) lab = "236-43";
        else if (upper.contains("236 44") || upper.contains("236-44")) lab = "236-44";
        else if (upper.contains("236 45") || upper.contains("236-45")) lab = "236-45";
        else if (upper.contains("236 46") || upper.contains("236-46")) lab = "236-46";
        else if (upper.contains("236 47") || upper.contains("236-47")) lab = "236-47";
        else if (upper.contains("236 48") || upper.contains("236-48")) lab = "236-48";

        if (lab != null && upper.contains("CPU")) {
            Matcher m = pAssetStart.matcher(row);
            if (m.find()) {
                String activo = m.group(1);
                cpusPorLab.computeIfAbsent(lab, k -> new ArrayList<>()).add(activo);
            }
        }
    }
}

