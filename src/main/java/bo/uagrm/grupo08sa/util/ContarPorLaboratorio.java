package bo.uagrm.grupo08sa.util;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ContarPorLaboratorio {

    public static class Item {
        public String activo;
        public String grupo;
        public String estado;
        public String fecha;
        public String ubicacion;
        public String labAsignado; // e.g. "236-41", "236-42", etc.
    }

    public static void main(String[] args) throws Exception {
        File file = new File("c:\\Users\\JAIRO\\Desktop\\Activos FICCT.pdf");
        List<Item> items = new ArrayList<>();

        Pattern startAsset = Pattern.compile("^(U[0-9A-Z]{9,15})\\s+(.+)$");
        // State and date pattern e.g. "BUENO 22/10/2025" or "REGULAR 22/10/2025" or "MALO 22/10/2025"
        Pattern stateDate = Pattern.compile("\\b(BUENO|REGULAR|MALO|BUEN|NUEVO)\\s+(\\d{2}/\\d{2}/\\d{4})\\b");

        try (PDDocument doc = PDDocument.load(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            int totalPages = doc.getNumberOfPages();

            StringBuilder fullText = new StringBuilder();
            for (int p = 1; p <= totalPages; p++) {
                stripper.setStartPage(p);
                stripper.setEndPage(p);
                fullText.append(stripper.getText(doc)).append("\n");
            }

            String[] lines = fullText.toString().split("\\r?\\n");
            List<String> rawBlocks = new ArrayList<>();
            Pattern pAsset = Pattern.compile("^(U[0-9A-Za-z]{9,15})\\b");

            StringBuilder currentBlock = new StringBuilder();
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("Activo Grupo Estado Fecha Ubicaci")) {
                    continue;
                }
                Matcher m = pAsset.matcher(line);
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

            System.out.println("Bloques totales: " + rawBlocks.size());

            Map<String, List<Item>> porLab = new TreeMap<>();
            Map<String, Integer> gruposCount = new TreeMap<>();

            for (String block : rawBlocks) {
                String upper = block.toUpperCase();
                String lab = null;

                // Match lab
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
                } else if (upper.contains("AUDITORIO")) {
                    lab = "AUDITORIO";
                }

                if (lab != null) {
                    // Extract Activo code: first word
                    String[] parts = block.split("\\s+");
                    String activo = parts[0];

                    // Extract Grupo: between activo and the Estado (BUENO, REGULAR, MALO)
                    Matcher mState = stateDate.matcher(block);
                    String grupo = "DESCONOCIDO";
                    String estadoOriginal = "BUENO";
                    String fecha = "";
                    String ubic = "";

                    if (mState.find()) {
                        int stateStart = mState.start(1);
                        int stateEnd = mState.end(2);
                        grupo = block.substring(activo.length(), stateStart).trim();
                        estadoOriginal = mState.group(1);
                        fecha = mState.group(2);
                        ubic = block.substring(stateEnd).trim();
                    } else {
                        // Fallback
                        grupo = parts.length > 1 ? parts[1] : "EQUIPO";
                    }

                    Item item = new Item();
                    item.activo = activo;
                    item.grupo = grupo;
                    item.estado = "OPERATIVO"; // User requested: "pon todo que sean operativos"
                    item.fecha = fecha;
                    item.ubicacion = ubic;
                    item.labAsignado = lab;

                    porLab.computeIfAbsent(lab, k -> new ArrayList<>()).add(item);
                    gruposCount.put(grupo, gruposCount.getOrDefault(grupo, 0) + 1);
                }
            }

            System.out.println("--- RESUMEN POR LABORATORIO / UBICACIÓN ---");
            int total4to = 0;
            for (Map.Entry<String, List<Item>> entry : porLab.entrySet()) {
                System.out.println(entry.getKey() + ": " + entry.getValue().size() + " activos");
                total4to += entry.getValue().size();

                // Count CPUs
                int cpus = 0;
                for (Item it : entry.getValue()) {
                    if (it.grupo.toUpperCase().contains("CPU")) {
                        cpus++;
                    }
                }
                System.out.println("   -> CPUs: " + cpus);
            }
            System.out.println("Total activos 4to piso: " + total4to);

            System.out.println("\n--- GRUPOS (DESCRIPCIÓN) ENCONTRADOS ---");
            for (Map.Entry<String, Integer> entry : gruposCount.entrySet()) {
                System.out.println("  " + entry.getKey() + ": " + entry.getValue());
            }

            // Let's also print CPUs in Lab 41 as a sanity check
            System.out.println("\n--- CPUs en 236-41 (ordenados por activo asc) ---");
            List<Item> lab41 = porLab.get("236-41");
            if (lab41 != null) {
                List<Item> cpus41 = new ArrayList<>();
                for (Item it : lab41) {
                    if (it.grupo.toUpperCase().contains("CPU")) cpus41.add(it);
                }
                cpus41.sort(Comparator.comparing(i -> i.activo));
                for (int i = 0; i < cpus41.size(); i++) {
                    Item it = cpus41.get(i);
                    String rol = (i == 0) ? "PC-DOC (Docente)" : String.format("PC-%02d (Estudiante)", i);
                    System.out.println("  " + it.activo + " -> " + rol + " (" + it.grupo + ")");
                }
            }
        }
    }
}

