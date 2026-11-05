package bo.uagrm.grupo08sa.util;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AnalizarPdf {

    public static void main(String[] args) throws Exception {
        File file = new File("c:\\Users\\JAIRO\\Desktop\\Activos FICCT.pdf");
        try (PDDocument doc = PDDocument.load(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            int totalPages = doc.getNumberOfPages();
            System.out.println("Procesando " + totalPages + " páginas...");

            StringBuilder docText = new StringBuilder();
            for (int p = 1; p <= totalPages; p++) {
                stripper.setStartPage(p);
                stripper.setEndPage(p);
                String text = stripper.getText(doc);
                docText.append("\n---PAGE_START_").append(p).append("---\n").append(text);
            }

            String[] lines = docText.toString().split("\\r?\\n");
            List<String> rawBlocks = new ArrayList<>();
            Pattern startAsset = Pattern.compile("^(U\\d{10,14})\\b");

            StringBuilder currentBlock = new StringBuilder();
            for (String line : lines) {
                line = line.trim();
                if (line.startsWith("---PAGE_START_")) {
                    continue;
                }
                if (line.isEmpty() || line.startsWith("Activo Grupo Estado Fecha Ubicaci")) {
                    continue;
                }

                Matcher m = startAsset.matcher(line);
                if (m.find()) {
                    if (currentBlock.length() > 0) {
                        rawBlocks.add(currentBlock.toString());
                        currentBlock.setLength(0);
                    }
                }
                if (currentBlock.length() > 0) {
                    currentBlock.append(" ");
                }
                currentBlock.append(line);
            }
            if (currentBlock.length() > 0) {
                rawBlocks.add(currentBlock.toString());
            }

            System.out.println("Total bloques de activos detectados: " + rawBlocks.size());

            Set<String> ubicaciones4to = new TreeSet<>();
            int count4to = 0;
            for (String block : rawBlocks) {
                String upper = block.toUpperCase();
                if (upper.contains("4TO") || upper.contains("236 4") || upper.contains("236-4") || upper.contains("AUDITORIO")) {
                    count4to++;
                    int idxMod = upper.indexOf("MODULO - 236");
                    if (idxMod != -1) {
                        ubicaciones4to.add(block.substring(idxMod));
                    } else {
                        ubicaciones4to.add(block);
                    }
                }
            }

            System.out.println("Activos con mención de 4to piso / 236-4x / Auditorio: " + count4to);
            System.out.println("--- MUESTRAS DE UBICACIONES / TEXTO DETECTADAS (" + ubicaciones4to.size() + ") ---");
            int printed = 0;
            for (String u : ubicaciones4to) {
                System.out.println("UBIC: " + u);
                printed++;
                if (printed >= 50) {
                    System.out.println("... y más ...");
                    break;
                }
            }
        }
    }
}
