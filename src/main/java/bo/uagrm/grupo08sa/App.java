package bo.uagrm.grupo08sa;

import bo.uagrm.grupo08sa.datos.InicializadorBD;
import bo.uagrm.grupo08sa.presentacion.ConsolaLocal;
import bo.uagrm.grupo08sa.presentacion.ProcesadorCorreo;
import bo.uagrm.grupo08sa.presentacion.correo.ClientePOP3;
import bo.uagrm.grupo08sa.presentacion.correo.ClienteSMTP;

import java.nio.file.Path;

/**
 * Sistema de Gestión de Laboratorios vía Mail — Grupo 08 SA.
 *
 *   java -jar SistemaLaboratorios.jar              escucha el buzón continuamente
 *   java -jar SistemaLaboratorios.jar --una-vez    procesa el buzón una sola vez
 *   java -jar SistemaLaboratorios.jar --local      prueba comandos desde la consola, sin correo
 *   java -jar SistemaLaboratorios.jar --probar     verifica PostgreSQL, POP3 y si el SMTP permite responder a Gmail
 *   java -jar SistemaLaboratorios.jar --crear-bd   crea las tablas y datos iniciales (sql/01 y sql/02)
 *   java -jar SistemaLaboratorios.jar --reiniciar-bd  borra todo (pide confirmación) y vuelve a crear la base
 */
public class App {

    public static void main(String[] args) throws Exception {
        String modo = args.length > 0 ? args[0] : "";
        switch (modo) {
            case "" -> new ProcesadorCorreo().iniciar();
            case "--una-vez" -> new ProcesadorCorreo().revisarBuzon();
            case "--local" -> new ConsolaLocal().iniciar();
            case "--probar" -> probarConexiones();
            case "--crear-bd" -> crearBaseDeDatos();
            case "--reiniciar-bd" -> reiniciarBaseDeDatos();
            default -> System.out.println("Modo desconocido: " + modo
                    + ". Use --una-vez, --local, --probar, --crear-bd o --reiniciar-bd.");
        }
    }

    private static void probarConexiones() {
        try {
            System.out.println("PostgreSQL: conectado. Tablas del sistema "
                    + (InicializadorBD.existenTablas() ? "presentes." : "NO creadas (use --crear-bd)."));
        } catch (Exception e) {
            System.out.println("PostgreSQL: ERROR → " + e.getMessage());
        }
        try (ClientePOP3 pop3 = new ClientePOP3(Config.get("pop3.servidor"), Config.getInt("pop3.puerto"),
                Config.get("correo.usuario"), Config.get("correo.contrasena"))) {
            pop3.conectar();
            System.out.println("POP3: conectado. Mensajes en el buzón: " + pop3.contarMensajes());
        } catch (Exception e) {
            System.out.println("POP3: ERROR → " + e.getMessage());
        }
        try {
            ClienteSMTP smtp = ProcesadorCorreo.crearClienteSMTP();
            System.out.println("SMTP: " + smtp.probarReenvio(Config.get("prueba.remitente")));
        } catch (Exception e) {
            System.out.println("SMTP: ERROR → " + e.getMessage());
        }
    }

    /** Borra el esquema sislab y lo vuelve a crear con los datos iniciales (solo el Encargado). */
    private static void reiniciarBaseDeDatos() throws Exception {
        System.out.print("Se BORRARÁN todos los datos del sistema (esquema sislab). Escriba SI para continuar: ");
        String respuesta = new java.util.Scanner(System.in).nextLine().replace("﻿", "").trim();
        if (!respuesta.equals("SI")) {
            System.out.println("Cancelado; no se modificó nada.");
            return;
        }
        InicializadorBD.ejecutarScript(Path.of("sql", "00_eliminar_todo.sql"));
        crearBaseDeDatos();
    }

    private static void crearBaseDeDatos() throws Exception {
        if (InicializadorBD.existenTablas()) {
            System.out.println("Las tablas ya existen; no se modificó nada. "
                    + "Para empezar de cero ejecute antes sql/00_eliminar_todo.sql.");
            return;
        }
        InicializadorBD.ejecutarScript(Path.of("sql", "01_esquema.sql"));
        InicializadorBD.ejecutarScript(Path.of("sql", "02_datos_iniciales.sql"));
        System.out.println("Base de datos creada con los roles, comandos, permisos y el Encargado inicial.");
    }
}
