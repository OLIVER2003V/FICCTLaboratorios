package bo.uagrm.grupo08sa.presentacion;

import bo.uagrm.grupo08sa.Config;
import bo.uagrm.grupo08sa.negocio.Adjunto;
import bo.uagrm.grupo08sa.negocio.Resultado;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

/**
 * Modo de prueba sin correo: se escribe el asunto en la consola y se ve la respuesta.
 * Pasa por las mismas capas que un correo real (interpretación, autorización, negocio, datos).
 * El HTML de la última respuesta se guarda en salida/respuesta.html para verlo en el navegador.
 */
public class ConsolaLocal {

    private final ProcesadorCorreo procesador = new ProcesadorCorreo(new NotificadorConsola());

    public void iniciar() {
        String remitente = Config.get("prueba.remitente");
        Adjunto adjunto = null;
        System.out.println("Modo local. Escriba el asunto, por ejemplo: LISUSU[\"*\"]");
        System.out.println("  :de correo@dominio   cambia el remitente simulado");
        System.out.println("  :adjunto ruta        adjunta un archivo al siguiente comando (CARHOR)");
        System.out.println("  :tareas              ejecuta las tareas automáticas (marcar faltas)");
        System.out.println("  :salir               termina");
        Scanner in = new Scanner(System.in);
        while (true) {
            System.out.print("\n[" + remitente + "] asunto> ");
            if (!in.hasNextLine()) {
                return;
            }
            String linea = in.nextLine().trim();
            if (linea.isEmpty() || linea.startsWith("#")) {
                continue;
            }
            if (linea.equalsIgnoreCase(":salir")) {
                return;
            }
            if (linea.toLowerCase().startsWith(":de ")) {
                remitente = linea.substring(4).trim().toLowerCase();
                continue;
            }
            if (linea.equalsIgnoreCase(":tareas")) {
                procesador.ejecutarTareasProgramadas();
                System.out.println("Tareas automáticas ejecutadas.");
                continue;
            }
            if (linea.toLowerCase().startsWith(":adjunto ")) {
                Path ruta = Path.of(linea.substring(9).trim());
                try {
                    adjunto = new Adjunto(ruta.getFileName().toString(), Files.readAllBytes(ruta));
                    System.out.println("Adjunto listo: " + ruta.getFileName() + " (" + adjunto.datos().length + " bytes)");
                } catch (IOException e) {
                    System.out.println("No se pudo leer " + ruta + ": " + e.getMessage());
                }
                continue;
            }
            Resultado r = procesador.atender(remitente, linea, adjunto);
            adjunto = null;
            imprimir(r);
            guardarHtml(RespuestaHTML.generar(r, linea));
        }
    }

    private static void imprimir(Resultado r) {
        System.out.println((r.isExito() ? "ÉXITO" : "ERROR") + " — " + r.getTitulo());
        System.out.println(r.getMensaje());
        for (Resultado.Tabla t : r.getTablas()) {
            if (t.subtitulo() != null) {
                System.out.println("\n  " + t.subtitulo());
            }
            imprimirTabla(t);
        }
    }

    private static void imprimirTabla(Resultado.Tabla t) {
        String[] cols = t.columnas();
        int[] ancho = new int[cols.length];
        for (int i = 0; i < cols.length; i++) {
            ancho[i] = cols[i].length();
        }
        for (String[] f : t.filas()) {
            for (int i = 0; i < f.length; i++) {
                ancho[i] = Math.min(60, Math.max(ancho[i], f[i] == null ? 0 : f[i].length()));
            }
        }
        System.out.println(fila(cols, ancho));
        StringBuilder sep = new StringBuilder();
        for (int a : ancho) {
            sep.append("-".repeat(a)).append("-+-");
        }
        System.out.println(sep);
        for (String[] f : t.filas()) {
            System.out.println(fila(f, ancho));
        }
    }

    private static String fila(String[] celdas, int[] ancho) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < celdas.length; i++) {
            String c = celdas[i] == null ? "" : celdas[i];
            if (c.length() > ancho[i]) {
                c = c.substring(0, ancho[i] - 1) + "…";
            }
            sb.append(c).append(" ".repeat(ancho[i] - c.length())).append(" | ");
        }
        return sb.toString();
    }

    private static void guardarHtml(String html) {
        try {
            Path archivo = Path.of("salida", "respuesta.html");
            Files.createDirectories(archivo.getParent());
            Files.writeString(archivo, html, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("No se pudo guardar salida/respuesta.html: " + e.getMessage());
        }
    }
}
