package bo.uagrm.grupo08sa.presentacion;

import bo.uagrm.grupo08sa.Config;
import bo.uagrm.grupo08sa.negocio.Adjunto;
import bo.uagrm.grupo08sa.negocio.Comando;
import bo.uagrm.grupo08sa.negocio.GestorComandos;
import bo.uagrm.grupo08sa.negocio.Notificador;
import bo.uagrm.grupo08sa.negocio.Resultado;
import bo.uagrm.grupo08sa.presentacion.correo.ClientePOP3;
import bo.uagrm.grupo08sa.presentacion.correo.ClienteSMTP;
import bo.uagrm.grupo08sa.presentacion.correo.CorreoEntrante;

import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Orquesta el ciclo: leer buzón (POP3) → interpretar → negocio → responder (SMTP) → eliminar mensaje.
 */
public class ProcesadorCorreo {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final ClienteSMTP smtp = crearClienteSMTP();
    private final GestorComandos gestor;
    private final String direccionPropia = Config.get("correo.direccion").toLowerCase();

    /** Modo correo: las notificaciones salen por SMTP. */
    public ProcesadorCorreo() {
        gestor = new GestorComandos(new NotificadorCorreo(smtp));
    }

    /** Modo local u otros: las notificaciones usan el canal indicado. */
    public ProcesadorCorreo(Notificador notificador) {
        gestor = new GestorComandos(notificador);
    }

    /** El servidor de salida se elige en config.properties (tecnoweb o, para pruebas, Gmail). */
    public static ClienteSMTP crearClienteSMTP() {
        String usuario = Config.get("smtp.usuario");
        return new ClienteSMTP(Config.get("smtp.servidor"), Config.getInt("smtp.puerto"),
                Boolean.parseBoolean(Config.get("smtp.ssl")), usuario, Config.get("smtp.contrasena"),
                usuario.isEmpty() ? Config.get("correo.direccion") : usuario, Config.get("correo.direccion"));
    }

    /** Bucle principal: revisa el buzón cada N segundos hasta que se detenga el programa. */
    public void iniciar() {
        int intervalo = Config.getInt("intervalo.segundos");
        log("Escuchando " + direccionPropia + " cada " + intervalo + " s. Ctrl+C para detener.");
        while (true) {
            try {
                revisarBuzon();
            } catch (IOException e) {
                log("No se pudo revisar el buzón: " + e.getMessage());
            }
            gestor.ejecutarTareasProgramadas();
            try {
                Thread.sleep(intervalo * 1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    public void revisarBuzon() throws IOException {
        try (ClientePOP3 pop3 = new ClientePOP3(Config.get("pop3.servidor"), Config.getInt("pop3.puerto"),
                Config.get("correo.usuario"), Config.get("correo.contrasena"))) {
            pop3.conectar();
            int cantidad = pop3.contarMensajes();
            for (int i = 1; i <= cantidad; i++) {
                CorreoEntrante correo = CorreoEntrante.desde(pop3.leerMensaje(i));
                try {
                    procesar(correo);
                } catch (RuntimeException e) {
                    log("Error inesperado con el mensaje " + i + ": " + e);
                }
                // Se elimina siempre, para no volver a procesar (ni responder) el mismo correo
                pop3.eliminar(i);
            }
        }
    }

    public void ejecutarTareasProgramadas() {
        gestor.ejecutarTareasProgramadas();
    }

    private void procesar(CorreoEntrante correo) {
        String remitente = correo.getRemitente();
        if (debeIgnorarse(remitente)) {
            log("Ignorado (remitente automático o propio): " + remitente + " — " + correo.getAsunto());
            return;
        }
        Adjunto adjunto = correo.getAdjuntos().isEmpty() ? null : correo.getAdjuntos().get(0);
        log("Recibido de " + remitente + ": " + correo.getAsunto()
                + (adjunto == null ? "" : " [adjunto: " + adjunto.nombre() + "]"));
        Resultado resultado = atender(remitente, correo.getAsunto(), adjunto);
        String html = RespuestaHTML.generar(resultado, correo.getAsunto());
        try {
            smtp.enviar(remitente, "Re: " + correo.getAsunto(), html);
            log("  → respuesta enviada (" + (resultado.isExito() ? "éxito" : "error") + ")");
        } catch (IOException e) {
            log("  → no se pudo enviar la respuesta: " + e.getMessage());
            if (e.getMessage().contains("Relaying denied")) {
                log("    El servidor solo reenvía a otros dominios desde la red de la facultad: "
                        + "ejecute el programa desde un laboratorio de la FICCT.");
            }
        }
    }

    /** Interpreta el asunto y lo pasa a la capa de negocio. También lo usa el modo local. */
    public Resultado atender(String remitente, String asunto, Adjunto adjunto) {
        try {
            Comando comando = InterpreteComando.analizar(asunto, remitente, adjunto);
            return gestor.ejecutar(comando);
        } catch (FormatoInvalidoException e) {
            return gestor.rechazarFormato(remitente, asunto, e.getMessage());
        }
    }

    /** Evita bucles de respuestas con rebotes, avisos automáticos o con el propio buzón. */
    private boolean debeIgnorarse(String remitente) {
        return remitente.isEmpty()
                || remitente.equals(direccionPropia)
                || remitente.startsWith("mailer-daemon")
                || remitente.startsWith("postmaster")
                || remitente.startsWith("no-reply")
                || remitente.startsWith("noreply");
    }

    static void log(String mensaje) {
        System.out.println("[" + LocalTime.now().format(HORA) + "] " + mensaje);
    }
}
