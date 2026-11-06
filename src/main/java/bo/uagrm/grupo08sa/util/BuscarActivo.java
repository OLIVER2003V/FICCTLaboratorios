package bo.uagrm.grupo08sa.util;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;

public class BuscarActivo {
    public static void main(String[] args) throws Exception {
        File file = new File("c:\\Users\\JAIRO\\Desktop\\Activos FICCT.pdf");
        try (PDDocument doc = PDDocument.load(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(149);
            stripper.setEndPage(149);
            System.out.println("Page 149:");
            System.out.println(stripper.getText(doc));
        }
    }
}
