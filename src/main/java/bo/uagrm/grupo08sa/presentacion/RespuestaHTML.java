package bo.uagrm.grupo08sa.presentacion;

import bo.uagrm.grupo08sa.negocio.Resultado;

/**
 * Convierte un Resultado en el HTML del correo de respuesta.
 * Los estilos van en línea porque los clientes de correo ignoran las hojas de estilo.
 */
public final class RespuestaHTML {

    private static final String VERDE = "#1e7e34";
    private static final String ROJO = "#b02a37";
    private static final String AZUL = "#1f3b6e";

    private RespuestaHTML() {
    }

    public static String generar(Resultado r, String asuntoOriginal) {
        StringBuilder cuerpo = new StringBuilder();
        String color = r.isExito() ? VERDE : ROJO;
        cuerpo.append("<p style=\"margin:0 0 12px;padding:10px 12px;border-left:4px solid ").append(color)
              .append(";background:").append(r.isExito() ? "#eaf6ec" : "#fbeaec").append(";\">")
              .append("<b style=\"color:").append(color).append(";\">").append(r.isExito() ? "Éxito" : "Error")
              .append(":</b> ").append(escapar(r.getMensaje())).append("</p>");
        for (Resultado.Tabla t : r.getTablas()) {
            if (t.subtitulo() != null) {
                cuerpo.append("<h3 style=\"margin:18px 0 8px;font-size:15px;color:").append(AZUL).append(";\">")
                      .append(escapar(t.subtitulo())).append("</h3>");
            }
            cuerpo.append(tabla(t));
        }
        return plantilla(r.getTitulo(), cuerpo.toString(), "Comando recibido: <code>" + escapar(asuntoOriginal) + "</code>");
    }

    /** Aviso automático (reserva aprobada, permuta, incidencia, falta...). */
    public static String notificacion(String titulo, String mensaje) {
        String cuerpo = "<p style=\"margin:0;padding:10px 12px;border-left:4px solid " + AZUL + ";background:#eef2f8;\">"
                + escapar(mensaje) + "</p>";
        return plantilla(titulo, cuerpo, "Notificación automática. Envíe AYUDA[\"*\"] para ver sus comandos.");
    }

    private static String plantilla(String titulo, String cuerpo, String pie) {
        return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"></head>"
                + "<body style=\"margin:0;padding:16px;background:#f4f6f9;font-family:Arial,Helvetica,sans-serif;color:#222;\">"
                + "<div style=\"max-width:960px;margin:0 auto;background:#fff;border:1px solid #dde2ea;border-radius:6px;\">"
                + "<div style=\"background:" + AZUL + ";color:#fff;padding:12px 16px;border-radius:6px 6px 0 0;\">"
                + "<div style=\"font-size:12px;opacity:.85;\">Sistema de Gestión de Laboratorios — FICCT · Grupo 08 SA</div>"
                + "<div style=\"font-size:18px;font-weight:bold;\">" + escapar(titulo) + "</div></div>"
                + "<div style=\"padding:16px;\">" + cuerpo
                + "<p style=\"margin:16px 0 0;font-size:12px;color:#666;\">" + pie + "</p>"
                + "</div></div></body></html>";
    }

    private static String tabla(Resultado.Tabla tabla) {
        if (tabla.filas().isEmpty()) {
            return "<p style=\"color:#666;\">No hay registros para mostrar.</p>";
        }
        StringBuilder t = new StringBuilder();
        t.append("<table cellpadding=\"6\" cellspacing=\"0\" style=\"border-collapse:collapse;width:100%;font-size:13px;\">")
         .append("<tr>");
        for (String col : tabla.columnas()) {
            t.append("<th style=\"background:").append(AZUL)
             .append(";color:#fff;text-align:left;border:1px solid #c8d0dc;\">").append(escapar(col)).append("</th>");
        }
        t.append("</tr>");
        boolean par = false;
        for (String[] fila : tabla.filas()) {
            t.append("<tr style=\"background:").append(par ? "#f3f6fa" : "#fff").append(";\">");
            for (String celda : fila) {
                t.append("<td style=\"border:1px solid #c8d0dc;\">").append(escapar(celda)).append("</td>");
            }
            t.append("</tr>");
            par = !par;
        }
        return t.append("</table>").toString();
    }

    public static String escapar(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
