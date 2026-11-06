package bo.uagrm.grupo08sa.util;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.util.*;

public class BuscarAuditorio {
    public static void main(String[] args) throws Exception {
        File file = new File("c:\\Users\\JAIRO\\Desktop\\Activos FICCT.pdf");
        try (PDDocument doc = PDDocument.load(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            for (int p = 1; p <= doc.getNumberOfPages(); p++) {
                stripper.setStartPage(p);
                stripper.setEndPage(p);
                String text = stripper.getText(doc);
                if (text.toUpperCase().contains("AUDITORIO") || text.toUpperCase().contains("236-40") || text.toUpperCase().contains("236 40")) {
                    System.out.println("Página " + p + " contiene mención:");
                    for (String line : text.split("\\r?\\n")) {
                        if (line.toUpperCase().contains("AUDITORIO") || line.toUpperCase().contains("40")) {
                            System.out.println("  " + line);
                        }
                    }
                }
            }
        }
    }
}

