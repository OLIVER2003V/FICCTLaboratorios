package bo.uagrm.grupo08sa.util;

import bo.uagrm.grupo08sa.datos.Conexion;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MigrarActivosEInventario {

    public static class ActivoRaw {
        public String activo;
        public String grupo;
        public String labNum; // "41", "42", ..., "48"
        public String labCodigo; // "LAB-41", ...
        public String labFacultad; // "236-41", ...
        public String codigoEquipo; // "LAB-41, PC-DOC", "LAB-41, PC-01", or activo
        public String tipo;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== INICIANDO MIGRACIÓN DE ACTIVOS FICCT 4TO PISO ===");

        // 1. Extraer activos del PDF
        File pdfFile = new File("c:\\Users\\JAIRO\\Desktop\\Activos FICCT.pdf");
        Pattern pAssetStart = Pattern.compile("^(U[0-9A-Za-z]{9,15})\\b");
        Pattern pHeader = Pattern.compile("^Activo\\s+Grupo\\s+Estado\\s+Fecha", Pattern.CASE_INSENSITIVE);
        Pattern stateDate = Pattern.compile("\\b(BUENO|REGULAR|MALO|BUEN|NUEVO)\\s+(\\d{2}/\\d{2}/\\d{4})\\b");

        Map<String, List<ActivoRaw>> activosPorLab = new TreeMap<>();
        int totalPdf = 0;

        try (PDDocument doc = PDDocument.load(pdfFile)) {
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
                            procesarFila(currentRow.toString(), pAssetStart, stateDate, activosPorLab);
                            currentRow.setLength(0);
                        }
                    }
                    if (currentRow.length() > 0) currentRow.append(" ");
                    currentRow.append(line);
                }
            }
            if (currentRow.length() > 0) {
                procesarFila(currentRow.toString(), pAssetStart, stateDate, activosPorLab);
            }
        }

        for (List<ActivoRaw> l : activosPorLab.values()) {
            totalPdf += l.size();
        }
        System.out.println("Activos 4to piso extraídos del PDF: " + totalPdf);

        // 2. Base de datos: Actualizar esquema
        try (Connection con = Conexion.getConexion(); Statement st = con.createStatement()) {
            System.out.println("Actualizando columnas en sislab.equipo...");
            st.executeUpdate("ALTER TABLE sislab.equipo ADD COLUMN IF NOT EXISTS activo VARCHAR(30)");
            st.executeUpdate("ALTER TABLE sislab.equipo ALTER COLUMN codigo TYPE VARCHAR(50)");
            st.executeUpdate("ALTER TABLE sislab.equipo ALTER COLUMN tipo TYPE VARCHAR(50)");
            st.executeUpdate("ALTER TABLE sislab.equipo DROP CONSTRAINT IF EXISTS equipo_tipo_check");

            // Mapeo de IDs de laboratorio
            Map<String, Integer> labIds = new HashMap<>();
            try (ResultSet rs = st.executeQuery("SELECT id, codigo FROM sislab.laboratorio")) {
                while (rs.next()) {
                    labIds.put(rs.getString("codigo"), rs.getInt("id"));
                }
            }

            // 3. Enumerar CPUs y asignar códigos de equipo
            for (Map.Entry<String, List<ActivoRaw>> entry : activosPorLab.entrySet()) {
                String labCode = entry.getKey(); // e.g. "LAB-41"
                List<ActivoRaw> list = entry.getValue();

                // Separar CPUs
                List<ActivoRaw> cpus = new ArrayList<>();
                for (ActivoRaw a : list) {
                    if (a.grupo.toUpperCase().contains("CPU")) {
                        cpus.add(a);
                    }
                }
                // Ordenar CPUs por activo asc
                cpus.sort(Comparator.comparing(a -> a.activo));

                for (int i = 0; i < cpus.size(); i++) {
                    ActivoRaw cpu = cpus.get(i);
                    if (i == 0) {
                        cpu.codigoEquipo = labCode + ", PC-DOC";
                    } else {
                        cpu.codigoEquipo = String.format("%s, PC-%02d", labCode, i);
                    }
                }

                // Para los no CPUs, codigoEquipo = activo
                for (ActivoRaw a : list) {
                    if (a.codigoEquipo == null) {
                        a.codigoEquipo = a.activo;
                    }
                }
            }

            // 4. Limpiar tablas equipo_red y equipo
            System.out.println("Limpiando equipos previos...");
            st.executeUpdate("DELETE FROM sislab.equipo_red");
            st.executeUpdate("DELETE FROM sislab.equipo");

            // 5. Insertar todos los activos en sislab.equipo
            System.out.println("Insertando activos en sislab.equipo...");
            String sqlInsertEq = "INSERT INTO sislab.equipo (activo, codigo, laboratorio_id, tipo, descripcion, estado) "
                    + "VALUES (?, ?, ?, ?, ?, 'OPERATIVO') RETURNING id";

            Map<String, Integer> idPorCodigo = new HashMap<>();

            try (PreparedStatement psEq = con.prepareStatement(sqlInsertEq)) {
                for (List<ActivoRaw> list : activosPorLab.values()) {
                    for (ActivoRaw a : list) {
                        Integer lId = labIds.get(a.labCodigo);
                        if (lId == null) {
                            System.err.println("No se encontró ID para " + a.labCodigo);
                            continue;
                        }
                        psEq.setString(1, a.activo);
                        psEq.setString(2, a.codigoEquipo);
                        psEq.setInt(3, lId);
                        psEq.setString(4, a.tipo);
                        psEq.setString(5, a.grupo);

                        try (ResultSet rs = psEq.executeQuery()) {
                            if (rs.next()) {
                                idPorCodigo.put(a.codigoEquipo, rs.getInt(1));
                            }
                        }
                    }
                }
            }
            System.out.println("Insertados en sislab.equipo: " + idPorCodigo.size() + " registros.");

            // 6. Insertar en sislab.equipo_red
            System.out.println("Configurando sislab.equipo_red...");
            String sqlInsertRed = "INSERT INTO sislab.equipo_red (equipo_id, ip, mascara, gateway, dns1, dns2, proxy, grupo_red) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

            try (PreparedStatement psRed = con.prepareStatement(sqlInsertRed)) {
                // LAB-41: Docente (.5) + 44 PCs (.184 a .227)
                insertarRed(psRed, idPorCodigo.get("LAB-41, PC-DOC"), "172.20.172.5", "255.255.255.0", "172.20.172.254", "10.10.0.251", "8.8.8.8", "172.20.172.254:3128", "LAB41");
                for (int i = 1; i <= 44; i++) {
                    String cod = String.format("LAB-41, PC-%02d", i);
                    Integer eqId = idPorCodigo.get(cod);
                    if (eqId != null) {
                        insertarRed(psRed, eqId, "172.20.172." + (183 + i), "255.255.255.0", "172.20.172.254", "10.10.0.251", "8.8.8.8", "172.20.172.254:3128", "LAB41");
                    }
                }

                // LAB-42: Solo Docente (.2)
                insertarRed(psRed, idPorCodigo.get("LAB-42, PC-DOC"), "172.20.173.2", "255.255.255.0", "172.20.172.254", "10.10.0.251", "8.8.8.8", "172.20.172.254:3128", "LAB236-42");

                // LAB-43: Docente (.10) + 25 PCs (.138 a .162)
                insertarRed(psRed, idPorCodigo.get("LAB-43, PC-DOC"), "172.20.172.10", "255.255.255.0", "172.20.172.254", "10.10.0.251", "8.8.8.8", "172.20.172.254:3128", "LAB43");
                for (int i = 1; i <= 25; i++) {
                    String cod = String.format("LAB-43, PC-%02d", i);
                    Integer eqId = idPorCodigo.get(cod);
                    if (eqId != null) {
                        insertarRed(psRed, eqId, "172.20.172." + (137 + i), "255.255.255.0", "172.20.172.254", "10.10.0.251", "8.8.8.8", "172.20.172.254:3128", "LAB43");
                    }
                }

                // LAB-44: Docente (.8) + 40 PCs (.55 a .94)
                insertarRed(psRed, idPorCodigo.get("LAB-44, PC-DOC"), "172.20.172.8", "255.255.255.0", "172.20.172.254", "10.10.0.251", "8.8.8.8", "172.20.172.254:3128", "LAB44");
                for (int i = 1; i <= 40; i++) {
                    String cod = String.format("LAB-44, PC-%02d", i);
                    Integer eqId = idPorCodigo.get(cod);
                    if (eqId != null) {
                        insertarRed(psRed, eqId, "172.20.172." + (54 + i), "255.255.255.0", "172.20.172.254", "10.10.0.251", "8.8.8.8", "172.20.172.254:3128", "LAB44");
                    }
                }

                // LAB-45: Docente (.57) + 44 PCs (.11 a .54)
                insertarRed(psRed, idPorCodigo.get("LAB-45, PC-DOC"), "172.20.172.57", "255.255.255.0", "172.20.172.254", "10.10.0.251", "8.8.8.8", "172.20.172.254:3128", "LAB45");
                for (int i = 1; i <= 44; i++) {
                    String cod = String.format("LAB-45, PC-%02d", i);
                    Integer eqId = idPorCodigo.get(cod);
                    if (eqId != null) {
                        insertarRed(psRed, eqId, "172.20.172." + (10 + i), "255.255.255.0", "172.20.172.254", "10.10.0.251", "8.8.8.8", "172.20.172.254:3128", "LAB45");
                    }
                }

                // LAB-46: Docente (.9) + 21 PCs (.95 a .115)
                insertarRed(psRed, idPorCodigo.get("LAB-46, PC-DOC"), "172.20.172.9", "255.255.255.0", "172.20.172.254", "10.10.0.251", "8.8.8.8", "172.20.172.254:3128", "LAB46");
                for (int i = 1; i <= 21; i++) {
                    String cod = String.format("LAB-46, PC-%02d", i);
                    Integer eqId = idPorCodigo.get(cod);
                    if (eqId != null) {
                        insertarRed(psRed, eqId, "172.20.172." + (94 + i), "255.255.255.0", "172.20.172.254", "10.10.0.251", "8.8.8.8", "172.20.172.254:3128", "LAB46");
                    }
                }
            }

            try (ResultSet rs = st.executeQuery("SELECT count(*) FROM sislab.equipo_red")) {
                if (rs.next()) {
                    System.out.println("Insertados en sislab.equipo_red: " + rs.getInt(1) + " registros.");
                }
            }
            System.out.println("=== MIGRACIÓN COMPLETADA EXITOSAMENTE ===");
        }
    }

    private static void insertarRed(PreparedStatement ps, Integer equipoId, String ip, String mask, String gw, String dns1, String dns2, String proxy, String grupoRed) throws SQLException {
        if (equipoId == null) return;
        ps.setInt(1, equipoId);
        ps.setString(2, ip);
        ps.setString(3, mask);
        ps.setString(4, gw);
        ps.setString(5, dns1);
        ps.setString(6, dns2);
        ps.setString(7, proxy);
        ps.setString(8, grupoRed);
        ps.executeUpdate();
    }

    private static void procesarFila(String row, Pattern pAssetStart, Pattern stateDate, Map<String, List<ActivoRaw>> map) {
        String upper = row.toUpperCase();
        String labNum = null;
        if (upper.contains("236 41") || upper.contains("236-41")) labNum = "41";
        else if (upper.contains("236 42") || upper.contains("236-42")) labNum = "42";
        else if (upper.contains("236 43") || upper.contains("236-43")) labNum = "43";
        else if (upper.contains("236 44") || upper.contains("236-44")) labNum = "44";
        else if (upper.contains("236 45") || upper.contains("236-45")) labNum = "45";
        else if (upper.contains("236 46") || upper.contains("236-46")) labNum = "46";
        else if (upper.contains("236 47") || upper.contains("236-47")) labNum = "47";
        else if (upper.contains("236 48") || upper.contains("236-48")) labNum = "48";

        if (labNum != null) {
            Matcher mAsset = pAssetStart.matcher(row);
            if (!mAsset.find()) return;
            String activo = mAsset.group(1);

            Matcher mSD = stateDate.matcher(row);
            String grupo = "";
            if (mSD.find()) {
                grupo = row.substring(activo.length(), mSD.start(1)).trim();
            } else {
                String[] p = row.split("\\s+");
                grupo = p.length > 1 ? p[1] : "EQUIPO";
            }

            // Normalización
            grupo = grupo.replaceAll("\\s+", " ").trim();
            if (grupo.equals("CPU (EQPO.")) grupo = "CPU (EQPO. COMPUTACION)";
            if (grupo.equals("MESA PARA")) grupo = "MESA PARA COMPUTADORA";
            if (grupo.equals("MONITOR PARA")) grupo = "MONITOR PARA COMPUTADORA";
            if (grupo.equals("DISCO DURO EXTERNO.")) grupo = "DISCO DURO EXTERNO";
            if (grupo.equals("LECTOR DE BARRA.")) grupo = "LECTOR DE CODIGO DE BARRA";

            ActivoRaw a = new ActivoRaw();
            a.activo = activo;
            a.grupo = grupo;
            a.labNum = labNum;
            a.labCodigo = "LAB-" + labNum;
            a.labFacultad = "236-" + labNum;

            if (grupo.toUpperCase().contains("CPU") || grupo.toUpperCase().contains("COMPUTADORA")) {
                a.tipo = "COMPUTADORA";
            } else if (grupo.toUpperCase().contains("PROYECTOR")) {
                a.tipo = "PROYECTOR";
            } else {
                a.tipo = "OTRO";
            }

            map.computeIfAbsent(a.labCodigo, k -> new ArrayList<>()).add(a);
        }
    }
}

