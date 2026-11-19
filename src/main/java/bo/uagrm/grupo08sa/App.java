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
            case "--cargar-turnos" -> cargarTurnos();
            case "--cargar-permutas" -> cargarPermutas();
            case "--actualizar-inventario" -> actualizarInventario();
            case "--restaurar-objetos" -> restaurarObjetos();
            case "--cargar-incidencias" -> cargarIncidencias();
            case "--cargar-directores" -> cargarDirectores();
            case "--cargar-bloqueos" -> cargarBloqueos();
            case "--actualizar-horario-inf120" -> actualizarHorarioInf120();
            case "--cargar-reservas-balcazar" -> cargarReservasBalcazar();
            case "--actualizar-reservas-defensa" -> actualizarReservasDefensa();
            case "--verificar-tablas" -> verificarTablas();
            case "--diagnostico-reservas" -> diagnosticarReservas();
            case "--reordenar-ids-reservas" -> reordenarIdsReservas();
            case "--exportar-cu1" -> exportarCU1();
            case "--exportar-cu2" -> exportarCU2();
            case "--exportar-cu3" -> exportarCU3();
            case "--exportar-cu4" -> exportarCU4();
            case "--exportar-cu5" -> exportarCU5();
            case "--reiniciar-bd" -> reiniciarBaseDeDatos();
            default -> System.out.println("Modo desconocido: " + modo
                    + ". Use --una-vez, --local, --probar, --crear-bd, --cargar-turnos, --cargar-permutas, --actualizar-inventario, --restaurar-objetos, --cargar-incidencias, --cargar-directores, --cargar-bloqueos, --verificar-tablas o --reiniciar-bd.");
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

    private static void cargarTurnos() throws Exception {
        System.out.println("Cargando turnos de auxiliares (semestre 2-2026, 4 franjas horarias)...");
        InicializadorBD.ejecutarScript(Path.of("sql", "03_turnos_auxiliares.sql"));
        System.out.println("Turnos cargados exitosamente (75 turnos registrados de Lunes a Viernes).");
    }

    private static void cargarPermutas() throws Exception {
        System.out.println("Cargando permutas y mensajes de prueba (semestre 2-2026, época de exámenes)...");
        InicializadorBD.ejecutarScript(Path.of("sql", "04_datos_permutas_mensajes.sql"));
        System.out.println("Permutas y mensajes cargados exitosamente.");
    }

    private static void actualizarInventario() throws Exception {
        System.out.println("Actualizando formato de inventario (CPUs: PC-DOC, PC-01... | Monitores: MONITOR-DOC, MONITOR-01...)...");
        InicializadorBD.ejecutarScript(Path.of("sql", "05_actualizar_inventario_formato.sql"));
        System.out.println("Inventario actualizado y ordenado exitosamente para todos los laboratorios.");
    }

    private static void restaurarObjetos() throws Exception {
        System.out.println("Restaurando auxiliares en objetos encontrados (108 registros)...");
        InicializadorBD.ejecutarScript(Path.of("sql", "06_restaurar_auxiliares_objetos.sql"));
        System.out.println("Objetos encontrados actualizados con sus auxiliares reales exitosamente.");
    }

    private static void cargarIncidencias() throws Exception {
        System.out.println("Cargando incidencias de prueba (18 fallas en diversos laboratorios)...");
        InicializadorBD.ejecutarScript(Path.of("sql", "07_datos_incidencias.sql"));
        System.out.println("Incidencias cargadas exitosamente.");
    }

    private static void cargarDirectores() throws Exception {
        System.out.println("Registrando y actualizando autoridades/directores (Decano: D01, Vicedecano: D02)...");
        InicializadorBD.ejecutarScript(Path.of("sql", "08_directores_autoridades.sql"));
        System.out.println("Autoridades registradas con éxito con rol DIR (D01: Peinado, D02: Caballero).");
    }

    private static void cargarBloqueos() throws Exception {
        System.out.println("Cargando calendario académico de fechas bloqueadas y feriados (semestre 2-2026)...");
        InicializadorBD.ejecutarScript(Path.of("sql", "09_datos_fechas_bloqueadas.sql"));
        System.out.println("Fechas bloqueadas registradas con éxito (feriados nacionales, departamentales, PSA y mantenimientos).");
    }

    private static void actualizarHorarioInf120() throws Exception {
        System.out.println("Actualizando horario de INF120-SA en LAB-44 a 08:30 - 10:00 (Lunes, Miércoles, Viernes)...");
        InicializadorBD.ejecutarScript(Path.of("sql", "11_modificar_horario_inf120.sql"));
        System.out.println("Horario de INF120-SA actualizado exitosamente a 08:30 - 10:00.");
    }

    private static void cargarReservasBalcazar() throws Exception {
        System.out.println("Registrando reservas para docente Evans Balcazar en LAB-44 (17, 18 y 20 de noviembre)...");
        InicializadorBD.ejecutarScript(Path.of("sql", "12_insertar_reservas_balcazar.sql"));
        System.out.println("Reservas registradas y aprobadas exitosamente en LAB-44.");
    }

    private static void actualizarReservasDefensa() throws Exception {
        System.out.println("Actualizando tipo de reservas a DEFENSA DE PROYECTO en LAB-44...");
        InicializadorBD.ejecutarScript(Path.of("sql", "13_actualizar_reservas_defensa_proyecto.sql"));
        System.out.println("Reservas actualizadas con éxito a tipo DEFENSA DE PROYECTO.");
    }

    private static void diagnosticarReservas() {
        try (java.sql.Connection cn = bo.uagrm.grupo08sa.datos.Conexion.getConexion();
             java.sql.Statement st = cn.createStatement()) {
            System.out.println("=== USUARIOS CON NOMBRE/CORREO JAIRO/SANABRIA/29405 ===");
            var rsU = st.executeQuery(
                    "SELECT u.id, u.codigo, u.nombre, u.apellido, u.correo, r.codigo as rol "
                    + "FROM usuario u JOIN rol r ON u.rol_id = r.id "
                    + "WHERE u.nombre ILIKE '%jairo%' OR u.correo ILIKE '%jairo%' OR u.correo ILIKE '%sanabria%' OR u.codigo = '29405'");
            while (rsU.next()) {
                System.out.printf("ID: %d | Cod: %s | Nombre: %s %s | Correo: %s | Rol: %s%n",
                        rsU.getInt("id"), rsU.getString("codigo"), rsU.getString("nombre"),
                        rsU.getString("apellido"), rsU.getString("correo"), rsU.getString("rol"));
            }

            System.out.println("\n=== ULTIMOS COMANDOS PROCESADOS (TABLA bitacora) ===");
            var rsM = st.executeQuery(
                    "SELECT id, remitente, asunto, comando, resultado, fecha FROM bitacora ORDER BY id DESC LIMIT 10");
            while (rsM.next()) {
                System.out.printf("ID: %d | De: %s | Asunto: %s | Cmd: %s | Res: %s | Fecha: %s%n",
                        rsM.getInt("id"), rsM.getString("remitente"), rsM.getString("asunto"),
                        rsM.getString("comando"), rsM.getString("resultado"), rsM.getTimestamp("fecha"));
            }

            System.out.println("\n=== LISTADO DE RESERVAS ACTUALES ===");
            var rsR = st.executeQuery(
                    "SELECT r.id, l.codigo as lab, r.fecha, r.hora_inicio, r.hora_fin, r.tipo, "
                    + "r.estado, u.nombre || ' ' || u.apellido as solicitante "
                    + "FROM reserva r JOIN laboratorio l ON r.laboratorio_id = l.id "
                    + "JOIN usuario u ON r.solicitante_id = u.id "
                    + "ORDER BY r.fecha ASC, r.hora_inicio ASC, r.id ASC");
            while (rsR.next()) {
                System.out.printf("ID: %2d | Lab: %-10s | Fecha: %s %s-%s | %-20s | %-10s | Solicitante: %s%n",
                        rsR.getInt("id"), rsR.getString("lab"), rsR.getDate("fecha"),
                        rsR.getTime("hora_inicio"), rsR.getTime("hora_fin"),
                        rsR.getString("tipo"), rsR.getString("estado"), rsR.getString("solicitante"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void reordenarIdsReservas() {
        try {
            System.out.println("Reordenando IDs de reservas cronológicamente (por fecha, hora_inicio)...");
            InicializadorBD.ejecutarScript(Path.of("sql", "14_reordenar_ids_reservas.sql"));
            System.out.println("IDs de reservas reordenados exitosamente.");
            diagnosticarReservas();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void exportarCU1() {
        try (java.sql.Connection cn = bo.uagrm.grupo08sa.datos.Conexion.getConexion();
             java.sql.Statement st = cn.createStatement()) {
            StringBuilder sb = new StringBuilder();
            sb.append("================================================================================\n");
            sb.append("          SISTEMA DE GESTION DE LABORATORIOS FICCT - GRUPO 08 SA\n");
            sb.append("      CASO DE USO 1 (CU1): GESTION DE USUARIOS, ROLES Y SEGURIDAD\n");
            sb.append("================================================================================\n\n");

            sb.append("--------------------------------------------------------------------------------\n");
            sb.append("1. DISENO LOGICO (MODELO CONCEPTUAL / RELACIONAL)\n");
            sb.append("--------------------------------------------------------------------------------\n");
            sb.append("El modulo de Seguridad y Gestion de Usuarios (CU1) administra las identidades,\n");
            sb.append("cuentas de correo electronico autorizadas y privilegios de acceso al sistema.\n\n");
            sb.append("A. ENTIDADES Y DEFINICION LOGICA:\n");
            sb.append("  * ROL (id, codigo, nombre, descripcion)\n");
            sb.append("    Define los perfiles del sistema: Encargado (ENC), Director (DIR),\n");
            sb.append("    Docente (DOC) y Auxiliar (AUX).\n\n");
            sb.append("  * USUARIO (id, codigo, nombre, apellido, correo, telefono, cargo, aula, gestion, rol_id, estado, creado_en)\n");
            sb.append("    Almacena las personas autorizadas que interactuan via correo electronico.\n\n");
            sb.append("  * COMANDO (id, nombre, descripcion)\n");
            sb.append("    Catalogo de comandos interpretables por el procesador de correos.\n\n");
            sb.append("  * ROL_COMANDO (rol_id, comando_id)\n");
            sb.append("    Matriz de permisos (M:N) que asocia que comandos puede ejecutar cada rol.\n\n");
            sb.append("B. RELACIONES Y CARDINALIDADES:\n");
            sb.append("  * ROL (1) ------ (N) USUARIO\n");
            sb.append("    Un rol puede estar asignado a uno o muchos usuarios. Todo usuario tiene asignado\n");
            sb.append("    exactamente un rol obligatorio mediante la clave foranea 'rol_id'.\n");
            sb.append("  * ROL (N) ------ (M) COMANDO\n");
            sb.append("    Relacion de muchos a muchos implementada a traves de la tabla puente ROL_COMANDO.\n\n");
            sb.append("C. REGLAS DE NEGOCIO E INTEGRIDAD:\n");
            sb.append("  1. Unicidad de Correo: El correo electronico debe ser unico y valido (Gmail).\n");
            sb.append("  2. Unicidad de Codigo: Cada usuario posee un codigo unico (Registro Universitario o COD_EMP).\n");
            sb.append("  3. Dominio de Roles: Exclusivamente 'ENC', 'DIR', 'DOC' o 'AUX'.\n");
            sb.append("  4. Estado y Baja Logica: El estado solo admite 'ACTIVO' o 'INACTIVO'. No se borran\n");
            sb.append("     usuarios fisicamente para mantener la integridad referencial historica.\n");
            sb.append("  5. Atributos de Auxiliares: Para rol 'AUX', opcionalmente se almacena el aula asignada\n");
            sb.append("     (ej. '236-41') y la gestion academica activa (ej. '2-2026').\n\n");

            sb.append("--------------------------------------------------------------------------------\n");
            sb.append("2. DISENO FISICO (DICCIONARIO DE DATOS EN POSTGRESQL)\n");
            sb.append("--------------------------------------------------------------------------------\n\n");

            sb.append("TABLA: rol\n");
            sb.append("+-------------+--------------+----------+---------------------------------------------------+\n");
            sb.append("| Columna     | Tipo         | Nulo     | Descripcion / Observacion                         |\n");
            sb.append("+-------------+--------------+----------+---------------------------------------------------+\n");
            sb.append("| id          | INTEGER      | NOT NULL | Clave primaria ( SERIAL )                         |\n");
            sb.append("| codigo      | VARCHAR(3)   | NOT NULL | Codigo unico del rol ( ENC, DIR, DOC, AUX )       |\n");
            sb.append("| nombre      | VARCHAR(50)  | NOT NULL | Nombre descriptivo del rol                        |\n");
            sb.append("| descripcion | VARCHAR(200) | Nulo     | Detalle y alcance de funciones del rol            |\n");
            sb.append("+-------------+--------------+----------+---------------------------------------------------+\n\n");

            sb.append("TABLA: usuario\n");
            sb.append("+-------------+--------------+----------+---------------------------------------------------+\n");
            sb.append("| Columna     | Tipo         | Nulo     | Descripcion / Observacion                         |\n");
            sb.append("+-------------+--------------+----------+---------------------------------------------------+\n");
            sb.append("| id          | INTEGER      | NOT NULL | Clave primaria ( SERIAL )                         |\n");
            sb.append("| codigo      | VARCHAR(20)  | NOT NULL | Codigo unico ( Registro Univ., COD_EMP, D01... )  |\n");
            sb.append("| nombre      | VARCHAR(60)  | NOT NULL | Nombres del usuario                               |\n");
            sb.append("| apellido    | VARCHAR(60)  | NOT NULL | Apellidos del usuario                             |\n");
            sb.append("| correo      | VARCHAR(100) | NOT NULL | Correo electronico unico (Gmail de envio)         |\n");
            sb.append("| telefono    | VARCHAR(20)  | Nulo     | Telefono o celular de contacto                    |\n");
            sb.append("| cargo       | VARCHAR(60)  | Nulo     | Cargo institucional (ej. Decano, Vicedecano)      |\n");
            sb.append("| aula        | VARCHAR(20)  | Nulo     | Modulo y aula asignada (ej. 236-41 para AUX)      |\n");
            sb.append("| gestion     | VARCHAR(10)  | Nulo     | Gestion del semestre asignado (ej. 2-2026 )       |\n");
            sb.append("| rol_id      | INTEGER      | NOT NULL | Clave foranea referencia a rol(id)                |\n");
            sb.append("| estado      | VARCHAR(10)  | NOT NULL | 'ACTIVO' o 'INACTIVO' (DEFAULT 'ACTIVO')          |\n");
            sb.append("| creado_en   | TIMESTAMP    | NOT NULL | Fecha y hora de creacion (DEFAULT CURRENT_TIME..) |\n");
            sb.append("+-------------+--------------+----------+---------------------------------------------------+\n\n");

            sb.append("TABLA: comando\n");
            sb.append("+-------------+--------------+----------+---------------------------------------------------+\n");
            sb.append("| Columna     | Tipo         | Nulo     | Descripcion / Observacion                         |\n");
            sb.append("+-------------+--------------+----------+---------------------------------------------------+\n");
            sb.append("| id          | INTEGER      | NOT NULL | Clave primaria ( SERIAL )                         |\n");
            sb.append("| nombre      | VARCHAR(10)  | NOT NULL | Nombre unico del comando (ej. INSUSU, LISRES)     |\n");
            sb.append("| descripcion | VARCHAR(200) | NOT NULL | Descripcion de la funcion y parametros            |\n");
            sb.append("+-------------+--------------+----------+---------------------------------------------------+\n\n");

            sb.append("TABLA: rol_comando\n");
            sb.append("+-------------+--------------+----------+---------------------------------------------------+\n");
            sb.append("| Columna     | Tipo         | Nulo     | Descripcion / Observacion                         |\n");
            sb.append("+-------------+--------------+----------+---------------------------------------------------+\n");
            sb.append("| rol_id      | INTEGER      | NOT NULL | Clave foranea referencia a rol(id)                |\n");
            sb.append("| comando_id  | INTEGER      | NOT NULL | Clave foranea referencia a comando(id)            |\n");
            sb.append("+-------------+--------------+----------+---------------------------------------------------+\n");
            sb.append("  * Clave Primaria compuesta: (rol_id, comando_id)\n\n");

            sb.append("--------------------------------------------------------------------------------\n");
            sb.append("3. TABLAS RELLENAS CON DATOS REALES DE LA BASE DE DATOS\n");
            sb.append("--------------------------------------------------------------------------------\n\n");

            sb.append("A. DATOS DE LA TABLA: rol\n");
            sb.append("+----+--------+-----------------------------+--------------------------------------------------------------------+\n");
            sb.append("| id | codigo | nombre                      | descripcion                                                        |\n");
            sb.append("+----+--------+-----------------------------+--------------------------------------------------------------------+\n");
            var rsRol = st.executeQuery("SELECT * FROM rol ORDER BY id");
            while (rsRol.next()) {
                sb.append(String.format("| %-2d | %-6s | %-27s | %-66s |\n",
                        rsRol.getInt("id"), rsRol.getString("codigo"),
                        rsRol.getString("nombre"), rsRol.getString("descripcion")));
            }
            sb.append("+----+--------+-----------------------------+--------------------------------------------------------------------+\n\n");

            sb.append("B. DATOS DE LA TABLA: usuario\n");
            sb.append("+-----+------------+---------------------------------+---------------------------------+-----------------------------------+------------+-------------+---------+--------+----------+\n");
            sb.append("| id  | codigo     | nombre                          | apellido                        | correo                            | telefono   | aula        | gestion | rol    | estado   |\n");
            sb.append("+-----+------------+---------------------------------+---------------------------------+-----------------------------------+------------+-------------+---------+--------+----------+\n");

            var rsUsu = st.executeQuery(
                    "SELECT u.id, u.codigo, u.nombre, u.apellido, u.correo, "
                    + "COALESCE(u.telefono, '') as telefono, COALESCE(u.aula, '') as aula, "
                    + "COALESCE(u.gestion, '') as gestion, r.codigo as rol, u.estado "
                    + "FROM usuario u JOIN rol r ON u.rol_id = r.id "
                    + "ORDER BY CASE r.codigo WHEN 'ENC' THEN 1 WHEN 'DIR' THEN 2 WHEN 'DOC' THEN 3 WHEN 'AUX' THEN 4 ELSE 5 END, u.id");

            int totalUsu = 0;
            while (rsUsu.next()) {
                totalUsu++;
                sb.append(String.format("| %-3d | %-10s | %-31s | %-31s | %-33s | %-10s | %-11s | %-7s | %-6s | %-8s |\n",
                        rsUsu.getInt("id"), rsUsu.getString("codigo"),
                        rsUsu.getString("nombre"), rsUsu.getString("apellido"),
                        rsUsu.getString("correo"), rsUsu.getString("telefono"),
                        rsUsu.getString("aula"), rsUsu.getString("gestion"),
                        rsUsu.getString("rol"), rsUsu.getString("estado")));
            }
            sb.append("+-----+------------+---------------------------------+---------------------------------+-----------------------------------+------------+-------------+---------+--------+----------+\n");
            sb.append(String.format("Total de usuarios registrados en el sistema: %d\n\n", totalUsu));

            sb.append("--------------------------------------------------------------------------------\n");
            sb.append("4. SCRIPT SQL COMPLETO (DDL Y DML)\n");
            sb.append("--------------------------------------------------------------------------------\n");
            sb.append("-- SCRIPT DDL: CREACION DE TABLAS (CU1)\n");
            sb.append("CREATE TABLE rol (\n");
            sb.append("  id          SERIAL PRIMARY KEY,\n");
            sb.append("  codigo      VARCHAR(3)  NOT NULL UNIQUE,\n");
            sb.append("  nombre      VARCHAR(50) NOT NULL,\n");
            sb.append("  descripcion VARCHAR(200)\n");
            sb.append(");\n\n");
            sb.append("CREATE TABLE comando (\n");
            sb.append("  id          SERIAL PRIMARY KEY,\n");
            sb.append("  nombre      VARCHAR(10)  NOT NULL UNIQUE,\n");
            sb.append("  descripcion VARCHAR(200) NOT NULL\n");
            sb.append(");\n\n");
            sb.append("CREATE TABLE rol_comando (\n");
            sb.append("  rol_id     INT NOT NULL REFERENCES rol(id),\n");
            sb.append("  comando_id INT NOT NULL REFERENCES comando(id),\n");
            sb.append("  PRIMARY KEY (rol_id, comando_id)\n");
            sb.append(");\n\n");
            sb.append("CREATE TABLE usuario (\n");
            sb.append("  id        SERIAL PRIMARY KEY,\n");
            sb.append("  codigo    VARCHAR(20)  NOT NULL UNIQUE,\n");
            sb.append("  nombre    VARCHAR(60)  NOT NULL,\n");
            sb.append("  apellido  VARCHAR(60)  NOT NULL,\n");
            sb.append("  correo    VARCHAR(100) NOT NULL UNIQUE,\n");
            sb.append("  telefono  VARCHAR(20),\n");
            sb.append("  cargo     VARCHAR(60),\n");
            sb.append("  aula      VARCHAR(20),\n");
            sb.append("  gestion   VARCHAR(10),\n");
            sb.append("  rol_id    INT NOT NULL REFERENCES rol(id),\n");
            sb.append("  estado    VARCHAR(10) NOT NULL DEFAULT 'ACTIVO'\n");
            sb.append("            CHECK (estado IN ('ACTIVO','INACTIVO')),\n");
            sb.append("  creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP\n");
            sb.append(");\n\n");

            sb.append("-- SCRIPT DML: INSERTS DE ROLES\n");
            sb.append("INSERT INTO rol (codigo, nombre, descripcion) VALUES\n");
            sb.append("  ('ENC', 'Encargado de Laboratorios', 'Administrador general del sistema y los laboratorios'),\n");
            sb.append("  ('DOC', 'Docente', 'Profesor que solicita reservas de laboratorio'),\n");
            sb.append("  ('AUX', 'Auxiliar de Laboratorio', 'Auxiliar encargado de turnos y reporte de objetos/fallas'),\n");
            sb.append("  ('DIR', 'Dirección / Decanato', 'Autoridades académicas con acceso de consulta y reportes');\n\n");

            sb.append("-- SCRIPT DML: INSERTS DE USUARIOS (DATOS REALES DEL CU1)\n");
            sb.append("INSERT INTO usuario (codigo, nombre, apellido, correo, telefono, cargo, aula, gestion, rol_id, estado) VALUES\n");

            var rsIns = st.executeQuery(
                    "SELECT u.codigo, u.nombre, u.apellido, u.correo, u.telefono, u.cargo, u.aula, u.gestion, u.rol_id, u.estado "
                    + "FROM usuario u JOIN rol r ON u.rol_id = r.id "
                    + "ORDER BY CASE r.codigo WHEN 'ENC' THEN 1 WHEN 'DIR' THEN 2 WHEN 'DOC' THEN 3 WHEN 'AUX' THEN 4 ELSE 5 END, u.id");

            boolean first = true;
            while (rsIns.next()) {
                if (!first) {
                    sb.append(",\n");
                }
                first = false;
                String tel = rsIns.getString("telefono") == null ? "NULL" : "'" + rsIns.getString("telefono").replace("'", "''") + "'";
                String car = rsIns.getString("cargo") == null ? "NULL" : "'" + rsIns.getString("cargo").replace("'", "''") + "'";
                String aul = rsIns.getString("aula") == null ? "NULL" : "'" + rsIns.getString("aula").replace("'", "''") + "'";
                String ges = rsIns.getString("gestion") == null ? "NULL" : "'" + rsIns.getString("gestion").replace("'", "''") + "'";

                sb.append(String.format("  ('%s', '%s', '%s', '%s', %s, %s, %s, %s, %d, '%s')",
                        rsIns.getString("codigo").replace("'", "''"),
                        rsIns.getString("nombre").replace("'", "''"),
                        rsIns.getString("apellido").replace("'", "''"),
                        rsIns.getString("correo").replace("'", "''"),
                        tel, car, aul, ges,
                        rsIns.getInt("rol_id"),
                        rsIns.getString("estado")));
            }
            sb.append(";\n\n");

            java.nio.file.Path rutaTxt = Path.of("CU1_DISENO_Y_DATOS_USUARIOS.txt");
            java.nio.file.Files.writeString(rutaTxt, sb.toString(), java.nio.charset.StandardCharsets.UTF_8);
            System.out.println("Archivo generado exitosamente en: " + rutaTxt.toAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void exportarCU2() {
        try (java.sql.Connection cn = bo.uagrm.grupo08sa.datos.Conexion.getConexion();
             java.sql.Statement st = cn.createStatement()) {
            StringBuilder sb = new StringBuilder();
            sb.append("================================================================================\n");
            sb.append("          SISTEMA DE GESTION DE LABORATORIOS FICCT - GRUPO 08 SA\n");
            sb.append("   CASO DE USO 2 (CU2): GESTION DE PERFILES Y CONFIGURACION SEMESTRAL\n");
            sb.append("================================================================================\n\n");

            sb.append("--------------------------------------------------------------------------------\n");
            sb.append("1. DISENO LOGICO (MODELO CONCEPTUAL / RELACIONAL)\n");
            sb.append("--------------------------------------------------------------------------------\n");
            sb.append("El modulo de Configuracion Semestral y Perfiles (CU2) administra la infraestructura\n");
            sb.append("fisica (laboratorios), la planificacion del calendario academico (semestres, feriados,\n");
            sb.append("mantenimientos y suspensiones de actividades) y la matriz de perfiles y permisos.\n\n");

            sb.append("A. ENTIDADES Y DEFINICION LOGICA:\n");
            sb.append("  * LABORATORIO (id, codigo, nombre, capacidad, ubicacion, estado)\n");
            sb.append("    Modulos y salas de computo o auditorios disponibles en la facultad.\n\n");
            sb.append("  * SEMESTRE (id, gestion, fecha_inicio, fecha_fin, estado)\n");
            sb.append("    Periodos academicos semestrales (ej. '2-2026', '1-2027'). Controla el ciclo\n");
            sb.append("    de vida: PLANIFICADO -> ACTIVO -> CERRADO.\n\n");
            sb.append("  * FECHA_BLOQUEADA (id, semestre_id, fecha, tipo, motivo, laboratorio_id)\n");
            sb.append("    Dias no habiles o de restriccion total/parcial (feriados nacionales, departamentales,\n");
            sb.append("    mantenimiento preventivo, jornadas PSA, fumigacion).\n\n");
            sb.append("  * ROL_COMANDO (rol_id, comando_id)\n");
            sb.append("    Matriz de perfiles de acceso que define que comandos estan habilitados para cada rol\n");
            sb.append("    (gestionado dinamicamente con el comando ASICOM).\n\n");

            sb.append("B. RELACIONES Y CARDINALIDADES:\n");
            sb.append("  * SEMESTRE (1) ──── (N) FECHA_BLOQUEADA\n");
            sb.append("    Un semestre agrupa una o muchas fechas bloqueadas. Toda fecha bloqueada pertenece\n");
            sb.append("    obligatoriamente a un semestre mediante la clave foranea 'semestre_id'.\n");
            sb.append("  * LABORATORIO (1) ──── (N) FECHA_BLOQUEADA (Opcional / Nulo)\n");
            sb.append("    Un bloqueo puede afectar especificamente a un solo laboratorio ('laboratorio_id')\n");
            sb.append("    o si 'laboratorio_id' es NULL, el bloqueo tiene alcance global (Todos los laboratorios).\n");
            sb.append("  * ROL (N) ──── (M) COMANDO (via ROL_COMANDO)\n");
            sb.append("    Permite asignar o revocar comandos a roles para configurar privilegios.\n\n");

            sb.append("C. REGLAS DE NEGOCIO E INTEGRIDAD:\n");
            sb.append("  1. Restriccion de Semestre Activo: Solo puede existir UN (1) semestre en estado 'ACTIVO'\n");
            sb.append("     al mismo tiempo (garantizado por el indice unico condicional ux_semestre_activo).\n");
            sb.append("  2. Secuencia Temporal: La fecha de finalizacion ('fecha_fin') de un semestre debe ser\n");
            sb.append("     estrictamente posterior a su fecha de inicio ('fecha_inicio').\n");
            sb.append("  3. Ciclo de Vida Semestral: Un semestre nace como 'PLANIFICADO', pasa a 'ACTIVO' (cerrando\n");
            sb.append("     el semestre previo) y concluye como 'CERRADO'.\n");
            sb.append("  4. Capacidad de Laboratorio: La capacidad debe ser estrictamente mayor a 0.\n");
            sb.append("  5. Estados de Laboratorio: Exclusivamente 'DISPONIBLE', 'MANTENIMIENTO' o 'INHABILITADO'.\n");
            sb.append("  6. Tipos de Fechas Bloqueadas: 'FERIADO', 'PSA', 'MANTENIMIENTO' u 'OTRO'.\n");
            sb.append("  7. Bloqueo Preventivo: Las fechas bloqueadas impiden de forma estricta cualquier reserva\n");
            sb.append("     o clase en los laboratorios afectados durante ese dia.\n\n");

            sb.append("--------------------------------------------------------------------------------\n");
            sb.append("2. DISENO FISICO (DICCIONARIO DE DATOS EN POSTGRESQL)\n");
            sb.append("--------------------------------------------------------------------------------\n\n");

            sb.append("TABLA: laboratorio\n");
            sb.append("+-------------+--------------+----------+---------------------------------------------------+\n");
            sb.append("| Columna     | Tipo         | Nulo     | Descripcion / Observacion                         |\n");
            sb.append("+-------------+--------------+----------+---------------------------------------------------+\n");
            sb.append("| id          | INTEGER      | NOT NULL | Clave primaria ( SERIAL )                         |\n");
            sb.append("| codigo      | VARCHAR(10)  | NOT NULL | Codigo unico del laboratorio ( LAB-41 ... AUD )   |\n");
            sb.append("| nombre      | VARCHAR(60)  | NOT NULL | Nombre descriptivo del laboratorio                |\n");
            sb.append("| capacidad   | INTEGER      | NOT NULL | Cantidad maxima de puestos de trabajo (> 0)       |\n");
            sb.append("| ubicacion   | VARCHAR(100) | Nulo     | Modulo y ubicacion fisica en el campus            |\n");
            sb.append("| estado      | VARCHAR(15)  | NOT NULL | DISPONIBLE, MANTENIMIENTO, INHABILITADO           |\n");
            sb.append("+-------------+--------------+----------+---------------------------------------------------+\n\n");

            sb.append("TABLA: semestre\n");
            sb.append("+-------------+--------------+----------+---------------------------------------------------+\n");
            sb.append("| Columna     | Tipo         | Nulo     | Descripcion / Observacion                         |\n");
            sb.append("+-------------+--------------+----------+---------------------------------------------------+\n");
            sb.append("| id          | INTEGER      | NOT NULL | Clave primaria ( SERIAL )                         |\n");
            sb.append("| gestion     | VARCHAR(10)  | NOT NULL | Gestion academica unica (ej. 2-2026, 1-2027)      |\n");
            sb.append("| fecha_inicio| DATE         | NOT NULL | Fecha de inicio del semestre academico            |\n");
            sb.append("| fecha_fin   | DATE         | NOT NULL | Fecha de culminacion (debe ser > fecha_inicio)    |\n");
            sb.append("| estado      | VARCHAR(12)  | NOT NULL | PLANIFICADO, ACTIVO, CERRADO                      |\n");
            sb.append("+-------------+--------------+----------+---------------------------------------------------+\n");
            sb.append("  * Indice unico parcial: ux_semestre_activo WHERE estado = 'ACTIVO'\n\n");

            sb.append("TABLA: fecha_bloqueada\n");
            sb.append("+-------------+--------------+----------+---------------------------------------------------+\n");
            sb.append("| Columna     | Tipo         | Nulo     | Descripcion / Observacion                         |\n");
            sb.append("+-------------+--------------+----------+---------------------------------------------------+\n");
            sb.append("| id          | INTEGER      | NOT NULL | Clave primaria ( SERIAL )                         |\n");
            sb.append("| semestre_id | INTEGER      | NOT NULL | Clave foranea referencia a semestre(id)           |\n");
            sb.append("| fecha       | DATE         | NOT NULL | Fecha del calendario afectada por la restriccion  |\n");
            sb.append("| tipo        | VARCHAR(15)  | NOT NULL | FERIADO, PSA, MANTENIMIENTO, OTRO                 |\n");
            sb.append("| motivo      | VARCHAR(200) | Nulo     | Descripcion o justificacion del bloqueo           |\n");
            sb.append("| laboratorio_id| INTEGER    | Nulo     | Referencia a laboratorio(id) (NULL = Todos)       |\n");
            sb.append("+-------------+--------------+----------+---------------------------------------------------+\n\n");

            sb.append("--------------------------------------------------------------------------------\n");
            sb.append("3. TABLAS RELLENAS CON DATOS REALES DE LA BASE DE DATOS\n");
            sb.append("--------------------------------------------------------------------------------\n\n");

            sb.append("A. DATOS DE LA TABLA: laboratorio (Infraestructura de laboratorios)\n");
            sb.append("+----+------------+-------------------------------------+-----------+------------------------+-------------+\n");
            sb.append("| id | codigo     | nombre                              | capacidad | ubicacion              | estado      |\n");
            sb.append("+----+------------+-------------------------------------+-----------+------------------------+-------------+\n");
            var rsLab = st.executeQuery("SELECT * FROM laboratorio ORDER BY id");
            while (rsLab.next()) {
                sb.append(String.format("| %-2d | %-10s | %-35s | %-9d | %-22s | %-11s |\n",
                        rsLab.getInt("id"), rsLab.getString("codigo"),
                        rsLab.getString("nombre"), rsLab.getInt("capacidad"),
                        rsLab.getString("ubicacion"), rsLab.getString("estado")));
            }
            sb.append("+----+------------+-------------------------------------+-----------+------------------------+-------------+\n\n");

            sb.append("B. DATOS DE LA TABLA: semestre (Planificacion academica)\n");
            sb.append("+----+------------+--------------+------------+-------------+\n");
            sb.append("| id | gestion    | fecha_inicio | fecha_fin  | estado      |\n");
            sb.append("+----+------------+--------------+------------+-------------+\n");
            var rsSem = st.executeQuery("SELECT * FROM semestre ORDER BY id");
            while (rsSem.next()) {
                sb.append(String.format("| %-2d | %-10s | %-12s | %-10s | %-11s |\n",
                        rsSem.getInt("id"), rsSem.getString("gestion"),
                        rsSem.getDate("fecha_inicio"), rsSem.getDate("fecha_fin"),
                        rsSem.getString("estado")));
            }
            sb.append("+----+------------+--------------+------------+-------------+\n\n");

            sb.append("C. DATOS DE LA TABLA: fecha_bloqueada (Calendario academico y feriados)\n");
            sb.append("+----+---------+------------+---------------+------------------------------------------------------------+-------------+\n");
            sb.append("| id | gestion | fecha      | tipo          | motivo                                                     | laboratorio |\n");
            sb.append("+----+---------+------------+---------------+------------------------------------------------------------+-------------+\n");
            var rsFb = st.executeQuery(
                    "SELECT fb.id, s.gestion, fb.fecha, fb.tipo, fb.motivo, COALESCE(l.codigo, 'Todos') as lab "
                    + "FROM fecha_bloqueada fb JOIN semestre s ON fb.semestre_id = s.id "
                    + "LEFT JOIN laboratorio l ON fb.laboratorio_id = l.id ORDER BY fb.fecha");
            while (rsFb.next()) {
                sb.append(String.format("| %-2d | %-7s | %-10s | %-13s | %-58s | %-11s |\n",
                        rsFb.getInt("id"), rsFb.getString("gestion"),
                        rsFb.getDate("fecha"), rsFb.getString("tipo"),
                        rsFb.getString("motivo"), rsFb.getString("lab")));
            }
            sb.append("+----+---------+------------+---------------+------------------------------------------------------------+-------------+\n\n");

            sb.append("--------------------------------------------------------------------------------\n");
            sb.append("4. SCRIPT SQL COMPLETO (DDL Y DML)\n");
            sb.append("--------------------------------------------------------------------------------\n");
            sb.append("-- SCRIPT DDL: CREACION DE TABLAS (CU2)\n");
            sb.append("CREATE TABLE laboratorio (\n");
            sb.append("  id        SERIAL PRIMARY KEY,\n");
            sb.append("  codigo    VARCHAR(10) NOT NULL UNIQUE,\n");
            sb.append("  nombre    VARCHAR(60) NOT NULL,\n");
            sb.append("  capacidad INT NOT NULL CHECK (capacidad > 0),\n");
            sb.append("  ubicacion VARCHAR(100),\n");
            sb.append("  estado    VARCHAR(15) NOT NULL DEFAULT 'DISPONIBLE'\n");
            sb.append("            CHECK (estado IN ('DISPONIBLE','MANTENIMIENTO','INHABILITADO'))\n");
            sb.append(");\n\n");

            sb.append("CREATE TABLE semestre (\n");
            sb.append("  id           SERIAL PRIMARY KEY,\n");
            sb.append("  gestion      VARCHAR(10) NOT NULL UNIQUE,\n");
            sb.append("  fecha_inicio DATE NOT NULL,\n");
            sb.append("  fecha_fin    DATE NOT NULL,\n");
            sb.append("  estado       VARCHAR(12) NOT NULL DEFAULT 'PLANIFICADO'\n");
            sb.append("               CHECK (estado IN ('PLANIFICADO','ACTIVO','CERRADO')),\n");
            sb.append("  CHECK (fecha_fin > fecha_inicio)\n");
            sb.append(");\n");
            sb.append("CREATE UNIQUE INDEX ux_semestre_activo ON semestre (estado) WHERE estado = 'ACTIVO';\n\n");

            sb.append("CREATE TABLE fecha_bloqueada (\n");
            sb.append("  id             SERIAL PRIMARY KEY,\n");
            sb.append("  semestre_id    INT  NOT NULL REFERENCES semestre(id),\n");
            sb.append("  fecha          DATE NOT NULL,\n");
            sb.append("  tipo           VARCHAR(15) NOT NULL\n");
            sb.append("                 CHECK (tipo IN ('FERIADO','PSA','MANTENIMIENTO','OTRO')),\n");
            sb.append("  motivo         VARCHAR(200),\n");
            sb.append("  laboratorio_id INT REFERENCES laboratorio(id)  -- NULL: afecta a todos\n");
            sb.append(");\n\n");

            sb.append("-- SCRIPT DML: INSERTS DE LABORATORIOS (DATOS REALES)\n");
            sb.append("INSERT INTO laboratorio (codigo, nombre, capacidad, ubicacion, estado) VALUES\n");
            var rsLabIns = st.executeQuery("SELECT codigo, nombre, capacidad, ubicacion, estado FROM laboratorio ORDER BY id");
            boolean firstLab = true;
            while (rsLabIns.next()) {
                if (!firstLab) sb.append(",\n");
                firstLab = false;
                String ubi = rsLabIns.getString("ubicacion") == null ? "NULL" : "'" + rsLabIns.getString("ubicacion").replace("'", "''") + "'";
                sb.append(String.format("  ('%s', '%s', %d, %s, '%s')",
                        rsLabIns.getString("codigo"), rsLabIns.getString("nombre"),
                        rsLabIns.getInt("capacidad"), ubi, rsLabIns.getString("estado")));
            }
            sb.append(";\n\n");

            sb.append("-- SCRIPT DML: INSERTS DE SEMESTRE (DATOS REALES)\n");
            sb.append("INSERT INTO semestre (gestion, fecha_inicio, fecha_fin, estado) VALUES\n");
            var rsSemIns = st.executeQuery("SELECT gestion, fecha_inicio, fecha_fin, estado FROM semestre ORDER BY id");
            boolean firstSem = true;
            while (rsSemIns.next()) {
                if (!firstSem) sb.append(",\n");
                firstSem = false;
                sb.append(String.format("  ('%s', '%s', '%s', '%s')",
                        rsSemIns.getString("gestion"), rsSemIns.getDate("fecha_inicio"),
                        rsSemIns.getDate("fecha_fin"), rsSemIns.getString("estado")));
            }
            sb.append(";\n\n");

            sb.append("-- SCRIPT DML: INSERTS DE FECHAS BLOQUEADAS (DATOS REALES)\n");
            sb.append("INSERT INTO fecha_bloqueada (semestre_id, fecha, tipo, motivo, laboratorio_id) VALUES\n");
            var rsFbIns = st.executeQuery(
                    "SELECT s.gestion, fb.fecha, fb.tipo, fb.motivo, l.codigo as lab "
                    + "FROM fecha_bloqueada fb JOIN semestre s ON fb.semestre_id = s.id "
                    + "LEFT JOIN laboratorio l ON fb.laboratorio_id = l.id ORDER BY fb.fecha");
            boolean firstFb = true;
            while (rsFbIns.next()) {
                if (!firstFb) sb.append(",\n");
                firstFb = false;
                String mot = rsFbIns.getString("motivo") == null ? "NULL" : "'" + rsFbIns.getString("motivo").replace("'", "''") + "'";
                String labSql = rsFbIns.getString("lab") == null ? "NULL" : "(SELECT id FROM laboratorio WHERE codigo = '" + rsFbIns.getString("lab") + "')";
                sb.append(String.format("  ((SELECT id FROM semestre WHERE gestion = '%s'), '%s', '%s', %s, %s)",
                        rsFbIns.getString("gestion"), rsFbIns.getDate("fecha"),
                        rsFbIns.getString("tipo"), mot, labSql));
            }
            sb.append(";\n\n");

            java.nio.file.Path rutaTxt = Path.of("CU2_DISENO_Y_DATOS_CONFIGURACION.txt");
            java.nio.file.Files.writeString(rutaTxt, sb.toString(), java.nio.charset.StandardCharsets.UTF_8);
            System.out.println("Archivo CU2 generado exitosamente en: " + rutaTxt.toAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void exportarCU3() {
        try (java.sql.Connection cn = bo.uagrm.grupo08sa.datos.Conexion.getConexion();
             java.sql.Statement st = cn.createStatement()) {
            StringBuilder sb = new StringBuilder();
            sb.append("================================================================================\n");
            sb.append("          SISTEMA DE GESTION DE LABORATORIOS FICCT - GRUPO 08 SA\n");
            sb.append(" CASO DE USO 3 (CU3): GESTION DE CARGA HORARIA BASE Y CONSULTAR DISPONIBILIDAD\n");
            sb.append("================================================================================\n\n");

            sb.append("--------------------------------------------------------------------------------\n");
            sb.append("1. DISENO LOGICO (MODELO CONCEPTUAL / RELACIONAL)\n");
            sb.append("--------------------------------------------------------------------------------\n");
            sb.append("El modulo de Carga Horaria Base y Consulta de Disponibilidad (CU3) administra las\n");
            sb.append("asignaturas de la facultad y la programacion academica recurrente de clases semestrales\n");
            sb.append("(lunes a sabado) que ocupan los laboratorios, sirviendo de base para la deteccion\n");
            sb.append("automatica de conflictos y consulta de disponibilidad de aulas (comando CONDIS).\n\n");

            sb.append("A. ENTIDADES Y DEFINICION LOGICA:\n");
            sb.append("  * MATERIA (id, sigla, nombre)\n");
            sb.append("    Catalogo de asignaturas oficiales de las carreras de la FICCT (ej. INF110, INF120, etc.).\n\n");
            sb.append("  * HORARIO_CLASE (id, semestre_id, laboratorio_id, materia_id, docente_id, grupo, dia_semana, hora_inicio, hora_fin)\n");
            sb.append("    Sesiones semanales recurrentes asignadas a un grupo y docente en un laboratorio especifico.\n\n");

            sb.append("B. RELACIONES Y CARDINALIDADES:\n");
            sb.append("  * MATERIA (1) ──── (N) HORARIO_CLASE\n");
            sb.append("    Una materia puede tener multiples clases o grupos asignados (teoria/laboratorio).\n");
            sb.append("    Toda sesion de clase pertenece obligatoriamente a una materia ('materia_id').\n");
            sb.append("  * SEMESTRE (1) ──── (N) HORARIO_CLASE\n");
            sb.append("    La carga horaria esta vinculada a un periodo semestral especifico ('semestre_id').\n");
            sb.append("  * LABORATORIO (1) ──── (N) HORARIO_CLASE\n");
            sb.append("    Un laboratorio alberga diferentes clases a lo largo de la semana ('laboratorio_id').\n");
            sb.append("  * USUARIO [DOCENTE] (1) ──── (N) HORARIO_CLASE\n");
            sb.append("    Un docente imparte clases en diferentes grupos y laboratorios ('docente_id').\n\n");

            sb.append("C. REGLAS DE NEGOCIO E INTEGRIDAD:\n");
            sb.append("  1. Unicidad de Sigla: Cada asignatura tiene una sigla academica unica (ej. 'INF120').\n");
            sb.append("  2. Dias Habiles de Clase: Codificados de 1 a 6 (1=Lunes, 2=Martes, 3=Miercoles, 4=Jueves, 5=Viernes, 6=Sabado).\n");
            sb.append("  3. Coherencia de Horas: La hora de finalizacion debe ser estrictamente posterior a la de inicio ('hora_fin > hora_inicio').\n");
            sb.append("  4. Franja de Atencion: Los horarios deben estar comprendidos entre las 07:00 y las 22:30.\n");
            sb.append("  5. Prevencion Estricta de Choques en Laboratorio: No se permiten dos clases superpuestas en el mismo\n");
            sb.append("     laboratorio, mismo dia y mismo semestre academico.\n");
            sb.append("  6. Prevencion de Choques Docentes: Un docente no puede tener dos clases simultaneas en aulas distintas.\n");
            sb.append("  7. Bloqueo para Reservas: Las horas ocupadas por la carga horaria base quedan automaticamente reservadas,\n");
            sb.append("     impidiendo que otros solicitantes reserven el laboratorio en esos horarios (salvo permutas CU5).\n\n");

            sb.append("--------------------------------------------------------------------------------\n");
            sb.append("2. DISENO FISICO (DICCIONARIO DE DATOS EN POSTGRESQL)\n");
            sb.append("--------------------------------------------------------------------------------\n\n");

            sb.append("TABLA: materia\n");
            sb.append("+-------------+--------------+----------+---------------------------------------------------+\n");
            sb.append("| Columna     | Tipo         | Nulo     | Descripcion / Observacion                         |\n");
            sb.append("+-------------+--------------+----------+---------------------------------------------------+\n");
            sb.append("| id          | INTEGER      | NOT NULL | Clave primaria ( SERIAL )                         |\n");
            sb.append("| sigla       | VARCHAR(10)  | NOT NULL | Sigla unica de la materia (ej. INF120, MAT101)    |\n");
            sb.append("| nombre      | VARCHAR(100) | NOT NULL | Nombre oficial de la asignatura                   |\n");
            sb.append("+-------------+--------------+----------+---------------------------------------------------+\n\n");

            sb.append("TABLA: horario_clase\n");
            sb.append("+-------------+--------------+----------+---------------------------------------------------+\n");
            sb.append("| Columna     | Tipo         | Nulo     | Descripcion / Observacion                         |\n");
            sb.append("+-------------+--------------+----------+---------------------------------------------------+\n");
            sb.append("| id          | INTEGER      | NOT NULL | Clave primaria ( SERIAL )                         |\n");
            sb.append("| semestre_id | INTEGER      | NOT NULL | Clave foranea referencia a semestre(id)           |\n");
            sb.append("| laboratorio_id| INTEGER    | NOT NULL | Clave foranea referencia a laboratorio(id)        |\n");
            sb.append("| materia_id  | INTEGER      | NOT NULL | Clave foranea referencia a materia(id)            |\n");
            sb.append("| docente_id  | INTEGER      | NOT NULL | Clave foranea referencia a usuario(id)            |\n");
            sb.append("| grupo       | VARCHAR(5)   | NOT NULL | Sigla del grupo academico (ej. SA, SC, Z1, 1)     |\n");
            sb.append("| dia_semana  | SMALLINT     | NOT NULL | Dia: 1=Lun, 2=Mar, 3=Mie, 4=Jue, 5=Vie, 6=Sab     |\n");
            sb.append("| hora_inicio | TIME         | NOT NULL | Hora de inicio de la clase                        |\n");
            sb.append("| hora_fin    | TIME         | NOT NULL | Hora de fin de la clase (CHECK hora_fin > inicio) |\n");
            sb.append("+-------------+--------------+----------+---------------------------------------------------+\n\n");

            sb.append("--------------------------------------------------------------------------------\n");
            sb.append("3. TABLAS RELLENAS CON DATOS REALES DE LA BASE DE DATOS\n");
            sb.append("--------------------------------------------------------------------------------\n\n");

            sb.append("A. DATOS DE LA TABLA: materia (Catalogo de asignaturas)\n");
            sb.append("+----+------------+------------------------------------------------------------+\n");
            sb.append("| id | sigla      | nombre                                                     |\n");
            sb.append("+----+------------+------------------------------------------------------------+\n");
            var rsMat = st.executeQuery("SELECT * FROM materia ORDER BY sigla");
            int totalMat = 0;
            while (rsMat.next()) {
                totalMat++;
                sb.append(String.format("| %-2d | %-10s | %-58s |\n",
                        rsMat.getInt("id"), rsMat.getString("sigla"), rsMat.getString("nombre")));
            }
            sb.append("+----+------------+------------------------------------------------------------+\n");
            sb.append(String.format("Total de materias registradas: %d\n\n", totalMat));

            sb.append("B. DATOS DE LA TABLA: horario_clase (Carga horaria base semanal)\n");
            sb.append("+-----+---------+------------+------------+---------------------------------+-------+---------------------------------+-----------+-----------------+\n");
            sb.append("| id  | gestion | lab        | sigla      | materia                         | grupo | docente                         | dia       | horario         |\n");
            sb.append("+-----+---------+------------+------------+---------------------------------+-------+---------------------------------+-----------+-----------------+\n");
            String[] nombresDias = {"", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado"};
            var rsHc = st.executeQuery(
                    "SELECT hc.id, s.gestion, l.codigo as lab, m.sigla, m.nombre as materia, hc.grupo, "
                    + "u.nombre || ' ' || u.apellido as docente, hc.dia_semana, hc.hora_inicio, hc.hora_fin "
                    + "FROM horario_clase hc "
                    + "JOIN semestre s ON hc.semestre_id = s.id "
                    + "JOIN laboratorio l ON hc.laboratorio_id = l.id "
                    + "JOIN materia m ON hc.materia_id = m.id "
                    + "JOIN usuario u ON hc.docente_id = u.id "
                    + "ORDER BY l.codigo, hc.dia_semana, hc.hora_inicio");
            int totalHc = 0;
            while (rsHc.next()) {
                totalHc++;
                int dia = rsHc.getInt("dia_semana");
                String diaStr = (dia >= 1 && dia <= 6) ? nombresDias[dia] : String.valueOf(dia);
                String horario = rsHc.getTime("hora_inicio").toString().substring(0, 5) + " - " + rsHc.getTime("hora_fin").toString().substring(0, 5);
                sb.append(String.format("| %-3d | %-7s | %-10s | %-10s | %-31s | %-5s | %-31s | %-9s | %-15s |\n",
                        rsHc.getInt("id"), rsHc.getString("gestion"), rsHc.getString("lab"),
                        rsHc.getString("sigla"), rsHc.getString("materia"), rsHc.getString("grupo"),
                        rsHc.getString("docente"), diaStr, horario));
            }
            sb.append("+-----+---------+------------+------------+---------------------------------+-------+---------------------------------+-----------+-----------------+\n");
            sb.append(String.format("Total de sesiones de horario de clase registradas: %d\n\n", totalHc));

            sb.append("--------------------------------------------------------------------------------\n");
            sb.append("4. SCRIPT SQL COMPLETO (DDL Y DML)\n");
            sb.append("--------------------------------------------------------------------------------\n");
            sb.append("-- SCRIPT DDL: CREACION DE TABLAS (CU3)\n");
            sb.append("CREATE TABLE materia (\n");
            sb.append("  id     SERIAL PRIMARY KEY,\n");
            sb.append("  sigla  VARCHAR(10)  NOT NULL UNIQUE,\n");
            sb.append("  nombre VARCHAR(100) NOT NULL\n");
            sb.append(");\n\n");

            sb.append("CREATE TABLE horario_clase (\n");
            sb.append("  id             SERIAL PRIMARY KEY,\n");
            sb.append("  semestre_id    INT NOT NULL REFERENCES semestre(id),\n");
            sb.append("  laboratorio_id INT NOT NULL REFERENCES laboratorio(id),\n");
            sb.append("  materia_id     INT NOT NULL REFERENCES materia(id),\n");
            sb.append("  docente_id     INT NOT NULL REFERENCES usuario(id),\n");
            sb.append("  grupo          VARCHAR(5) NOT NULL,\n");
            sb.append("  dia_semana     SMALLINT NOT NULL CHECK (dia_semana BETWEEN 1 AND 6),\n");
            sb.append("  hora_inicio    TIME NOT NULL,\n");
            sb.append("  hora_fin       TIME NOT NULL,\n");
            sb.append("  CHECK (hora_fin > hora_inicio)\n");
            sb.append(");\n\n");

            sb.append("-- SCRIPT DML: INSERTS DE MATERIAS (DATOS REALES)\n");
            sb.append("INSERT INTO materia (sigla, nombre) VALUES\n");
            var rsMatIns = st.executeQuery("SELECT sigla, nombre FROM materia ORDER BY sigla");
            boolean firstMat = true;
            while (rsMatIns.next()) {
                if (!firstMat) sb.append(",\n");
                firstMat = false;
                sb.append(String.format("  ('%s', '%s')",
                        rsMatIns.getString("sigla").replace("'", "''"),
                        rsMatIns.getString("nombre").replace("'", "''")));
            }
            sb.append(";\n\n");

            sb.append("-- SCRIPT DML: INSERTS DE HORARIOS DE CLASE (DATOS REALES)\n");
            sb.append("INSERT INTO horario_clase (semestre_id, laboratorio_id, materia_id, docente_id, grupo, dia_semana, hora_inicio, hora_fin) VALUES\n");
            var rsHcIns = st.executeQuery(
                    "SELECT s.gestion, l.codigo as lab, m.sigla, u.codigo as docente_cod, "
                    + "hc.grupo, hc.dia_semana, hc.hora_inicio, hc.hora_fin "
                    + "FROM horario_clase hc "
                    + "JOIN semestre s ON hc.semestre_id = s.id "
                    + "JOIN laboratorio l ON hc.laboratorio_id = l.id "
                    + "JOIN materia m ON hc.materia_id = m.id "
                    + "JOIN usuario u ON hc.docente_id = u.id "
                    + "ORDER BY hc.id");
            boolean firstHc = true;
            while (rsHcIns.next()) {
                if (!firstHc) sb.append(",\n");
                firstHc = false;
                sb.append(String.format("  ((SELECT id FROM semestre WHERE gestion = '%s'), "
                        + "(SELECT id FROM laboratorio WHERE codigo = '%s'), "
                        + "(SELECT id FROM materia WHERE sigla = '%s'), "
                        + "(SELECT id FROM usuario WHERE codigo = '%s'), "
                        + "'%s', %d, '%s', '%s')",
                        rsHcIns.getString("gestion"),
                        rsHcIns.getString("lab"),
                        rsHcIns.getString("sigla"),
                        rsHcIns.getString("docente_cod"),
                        rsHcIns.getString("grupo"),
                        rsHcIns.getInt("dia_semana"),
                        rsHcIns.getTime("hora_inicio"),
                        rsHcIns.getTime("hora_fin")));
            }
            sb.append(";\n\n");

            java.nio.file.Path rutaTxt = Path.of("CU3_DISENO_Y_DATOS_CARGA_HORARIA.txt");
            java.nio.file.Files.writeString(rutaTxt, sb.toString(), java.nio.charset.StandardCharsets.UTF_8);
            System.out.println("Archivo CU3 generado exitosamente en: " + rutaTxt.toAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void exportarCU4() {
        try (java.sql.Connection cn = bo.uagrm.grupo08sa.datos.Conexion.getConexion();
             java.sql.Statement st = cn.createStatement()) {
            StringBuilder sb = new StringBuilder();
            sb.append("================================================================================\n");
            sb.append("          SISTEMA DE GESTION DE LABORATORIOS FICCT - GRUPO 08 SA\n");
            sb.append("   CASO DE USO 4 (CU4): GESTION DE RESERVAS EXTRAORDINARIAS Y NOTIFICACIONES\n");
            sb.append("================================================================================\n\n");

            sb.append("--------------------------------------------------------------------------------\n");
            sb.append("1. DISENO LOGICO (MODELO CONCEPTUAL / RELACIONAL)\n");
            sb.append("--------------------------------------------------------------------------------\n");
            sb.append("El modulo de Reservas Extraordinarias y Notificaciones (CU4) administra las\n");
            sb.append("solicitudes especiales de laboratorios realizadas por docentes o directores para\n");
            sb.append("examenes, recuperaciones, practicas, eventos academicos y defensas de proyecto,\n");
            sb.append("asi como el flujo de aprobacion/rechazo por el Encargado y la notificacion automatica.\n\n");

            sb.append("A. ENTIDADES Y DEFINICION LOGICA:\n");
            sb.append("  * RESERVA (id, laboratorio_id, solicitante_id, fecha, hora_inicio, hora_fin, tipo, motivo, estado, resuelto_por, observacion, creado_en)\n");
            sb.append("    Registra la peticion de uso temporal de un laboratorio para una fecha y franja horaria determinada.\n\n");
            sb.append("  * NOTIFICACION (id, usuario_id, asunto, cuerpo, reserva_id, enviado_en)\n");
            sb.append("    Historial de mensajes y avisos emitidos hacia los solicitantes informando resoluciones o confirmaciones.\n\n");

            sb.append("B. RELACIONES Y CARDINALIDADES:\n");
            sb.append("  * LABORATORIO (1) ──── (N) RESERVA\n");
            sb.append("    Un laboratorio alberga multiples reservas a lo largo del semestre ('laboratorio_id').\n");
            sb.append("  * USUARIO [SOLICITANTE] (1) ──── (N) RESERVA\n");
            sb.append("    Un docente o director puede realizar multiples solicitudes de reserva ('solicitante_id').\n");
            sb.append("  * USUARIO [ENCARGADO] (1) ──── (N) RESERVA (Opcional / Nulo mientras este PENDIENTE)\n");
            sb.append("    El encargado evalua y resuelve la solicitud aprobando o rechazando ('resuelto_por').\n");
            sb.append("  * USUARIO (1) ──── (N) NOTIFICACION\n");
            sb.append("    Cada notificacion tiene un usuario destinatario ('usuario_id').\n");
            sb.append("  * RESERVA (1) ──── (N) NOTIFICACION (Opcional / ON UPDATE CASCADE ON DELETE SET NULL)\n");
            sb.append("    Una reserva genera una o varias notificaciones de estado ('reserva_id').\n\n");

            sb.append("C. REGLAS DE NEGOCIO E INTEGRIDAD:\n");
            sb.append("  1. Restriccion de Fechas Futuras: Las reservas solo pueden solicitarse y aprobarse para fechas\n");
            sb.append("     y horas estrictamente futuras respecto al momento de la operacion.\n");
            sb.append("  2. Tipos de Reserva Permitidos: 'EXAMEN', 'RECUPERACION', 'PRACTICA', 'EVENTO',\n");
            sb.append("     'DEFENSA DE PROYECTO', 'DEFENSA'.\n");
            sb.append("  3. Ciclo de Vida: Inicia como 'PENDIENTE'. El Encargado puede cambiar a 'APROBADA' o 'RECHAZADA'.\n");
            sb.append("     El solicitante puede pasarla a 'CANCELADA' antes de que ocurra.\n");
            sb.append("  4. Control de Choques (Disponibilidad Estricta): No se permite aprobar una reserva si se superpone\n");
            sb.append("     con una clase de carga horaria base (CU3), con otra reserva ya 'APROBADA' o con una fecha bloqueada (CU2).\n");
            sb.append("  5. Coherencia de Horas: La hora de fin debe ser estrictamente posterior a la hora de inicio ('hora_fin > hora_inicio').\n");
            sb.append("  6. Auditoria Obligatoria: Al aprobar o rechazar se registra el Encargado ('resuelto_por') y una observacion explicativa.\n\n");

            sb.append("--------------------------------------------------------------------------------\n");
            sb.append("2. DISENO FISICO (DICCIONARIO DE DATOS EN POSTGRESQL)\n");
            sb.append("--------------------------------------------------------------------------------\n\n");

            sb.append("TABLA: reserva\n");
            sb.append("+-------------+--------------+----------+---------------------------------------------------+\n");
            sb.append("| Columna     | Tipo         | Nulo     | Descripcion / Observacion                         |\n");
            sb.append("+-------------+--------------+----------+---------------------------------------------------+\n");
            sb.append("| id          | INTEGER      | NOT NULL | Clave primaria ( SERIAL )                         |\n");
            sb.append("| laboratorio_id| INTEGER    | NOT NULL | Clave foranea referencia a laboratorio(id)        |\n");
            sb.append("| solicitante_id| INTEGER    | NOT NULL | Clave foranea referencia a usuario(id)            |\n");
            sb.append("| fecha       | DATE         | NOT NULL | Fecha de la reserva (debe ser futura)             |\n");
            sb.append("| hora_inicio | TIME         | NOT NULL | Hora de inicio solicitada                         |\n");
            sb.append("| hora_fin    | TIME         | NOT NULL | Hora de fin solicitada (CHECK hora_fin > inicio)  |\n");
            sb.append("| tipo        | VARCHAR(30)  | NOT NULL | EXAMEN, PRACTICA, EVENTO, DEFENSA DE PROYECTO...  |\n");
            sb.append("| motivo      | VARCHAR(200) | Nulo     | Descripcion o justificacion de la solicitud       |\n");
            sb.append("| estado      | VARCHAR(10)  | NOT NULL | PENDIENTE, APROBADA, RECHAZADA, CANCELADA         |\n");
            sb.append("| resuelto_por| INTEGER      | Nulo     | Referencia a usuario(id) del Encargado resolutor  |\n");
            sb.append("| observacion | VARCHAR(200) | Nulo     | Nota de resolucion emitida por el Encargado       |\n");
            sb.append("| creado_en   | TIMESTAMP    | NOT NULL | Fecha y hora de creacion (DEFAULT CURRENT_TIME..) |\n");
            sb.append("+-------------+--------------+----------+---------------------------------------------------+\n\n");

            sb.append("TABLA: notificacion\n");
            sb.append("+-------------+--------------+----------+---------------------------------------------------+\n");
            sb.append("| Columna     | Tipo         | Nulo     | Descripcion / Observacion                         |\n");
            sb.append("+-------------+--------------+----------+---------------------------------------------------+\n");
            sb.append("| id          | INTEGER      | NOT NULL | Clave primaria ( SERIAL )                         |\n");
            sb.append("| usuario_id  | INTEGER      | NOT NULL | Clave foranea referencia a usuario(id)            |\n");
            sb.append("| asunto      | VARCHAR(150) | NOT NULL | Asunto del aviso o correo de notificacion         |\n");
            sb.append("| cuerpo      | TEXT         | Nulo     | Contenido o detalle completo del mensaje          |\n");
            sb.append("| reserva_id  | INTEGER      | Nulo     | Clave foranea referencia a reserva(id) (ON UPDATE)|\n");
            sb.append("| enviado_en  | TIMESTAMP    | NOT NULL | Fecha y hora de emision (DEFAULT CURRENT_TIME..)  |\n");
            sb.append("+-------------+--------------+----------+---------------------------------------------------+\n\n");

            sb.append("--------------------------------------------------------------------------------\n");
            sb.append("3. TABLAS RELLENAS CON DATOS REALES DE LA BASE DE DATOS\n");
            sb.append("--------------------------------------------------------------------------------\n\n");

            sb.append("A. DATOS DE LA TABLA: reserva (35 reservas ordenadas cronologicamente)\n");
            sb.append("+----+------------+------------+-----------------+---------------------+---------------------------------+---------------------------------+------------+---------------------------------+\n");
            sb.append("| id | lab        | fecha      | horario         | tipo                | motivo                          | solicitante                     | estado     | observacion                     |\n");
            sb.append("+----+------------+------------+-----------------+---------------------+---------------------------------+---------------------------------+------------+---------------------------------+\n");
            var rsRes = st.executeQuery(
                    "SELECT r.id, l.codigo as lab, r.fecha, r.hora_inicio, r.hora_fin, r.tipo, "
                    + "COALESCE(r.motivo, '') as motivo, u.nombre || ' ' || u.apellido as solicitante, "
                    + "r.estado, COALESCE(r.observacion, '') as observacion "
                    + "FROM reserva r JOIN laboratorio l ON r.laboratorio_id = l.id "
                    + "JOIN usuario u ON r.solicitante_id = u.id "
                    + "ORDER BY r.id ASC");
            int totalRes = 0;
            while (rsRes.next()) {
                totalRes++;
                String horario = rsRes.getTime("hora_inicio").toString().substring(0, 5) + " - " + rsRes.getTime("hora_fin").toString().substring(0, 5);
                sb.append(String.format("| %-2d | %-10s | %-10s | %-15s | %-19s | %-31s | %-31s | %-10s | %-31s |\n",
                        rsRes.getInt("id"), rsRes.getString("lab"), rsRes.getDate("fecha"), horario,
                        rsRes.getString("tipo"), rsRes.getString("motivo"), rsRes.getString("solicitante"),
                        rsRes.getString("estado"), rsRes.getString("observacion")));
            }
            sb.append("+----+------------+------------+-----------------+---------------------+---------------------------------+---------------------------------+------------+---------------------------------+\n");
            sb.append(String.format("Total de reservas registradas en el sistema: %d\n\n", totalRes));

            sb.append("B. DATOS DE LA TABLA: notificacion (Notificaciones vinculadas a reservas)\n");
            sb.append("+----+---------------------------------+------------------------------------------------------------+------------+---------------------+\n");
            sb.append("| id | usuario                         | asunto                                                     | reserva_id | enviado_en          |\n");
            sb.append("+----+---------------------------------+------------------------------------------------------------+------------+---------------------+\n");
            var rsNot = st.executeQuery(
                    "SELECT n.id, u.nombre || ' ' || u.apellido as usuario, n.asunto, n.reserva_id, n.enviado_en "
                    + "FROM notificacion n JOIN usuario u ON n.usuario_id = u.id ORDER BY n.id ASC");
            int totalNot = 0;
            while (rsNot.next()) {
                totalNot++;
                String resId = rsNot.getString("reserva_id") == null ? "-" : rsNot.getString("reserva_id");
                sb.append(String.format("| %-2d | %-31s | %-58s | %-10s | %-19s |\n",
                        rsNot.getInt("id"), rsNot.getString("usuario"),
                        rsNot.getString("asunto"), resId, rsNot.getTimestamp("enviado_en")));
            }
            sb.append("+----+---------------------------------+------------------------------------------------------------+------------+---------------------+\n");
            sb.append(String.format("Total de notificaciones registradas: %d\n\n", totalNot));

            sb.append("--------------------------------------------------------------------------------\n");
            sb.append("4. SCRIPT SQL COMPLETO (DDL Y DML)\n");
            sb.append("--------------------------------------------------------------------------------\n");
            sb.append("-- SCRIPT DDL: CREACION DE TABLAS (CU4)\n");
            sb.append("CREATE TABLE reserva (\n");
            sb.append("  id             SERIAL PRIMARY KEY,\n");
            sb.append("  laboratorio_id INT  NOT NULL REFERENCES laboratorio(id),\n");
            sb.append("  solicitante_id INT  NOT NULL REFERENCES usuario(id),\n");
            sb.append("  fecha          DATE NOT NULL,\n");
            sb.append("  hora_inicio    TIME NOT NULL,\n");
            sb.append("  hora_fin       TIME NOT NULL,\n");
            sb.append("  tipo           VARCHAR(30) NOT NULL\n");
            sb.append("                 CHECK (tipo IN ('EXAMEN','RECUPERACION','PRACTICA','EVENTO','DEFENSA DE PROYECTO','DEFENSA')),\n");
            sb.append("  motivo         VARCHAR(200),\n");
            sb.append("  estado         VARCHAR(10) NOT NULL DEFAULT 'PENDIENTE'\n");
            sb.append("                 CHECK (estado IN ('PENDIENTE','APROBADA','RECHAZADA','CANCELADA')),\n");
            sb.append("  resuelto_por   INT REFERENCES usuario(id),\n");
            sb.append("  observacion    VARCHAR(200),\n");
            sb.append("  creado_en      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,\n");
            sb.append("  CHECK (hora_fin > hora_inicio)\n");
            sb.append(");\n\n");

            sb.append("CREATE TABLE notificacion (\n");
            sb.append("  id         SERIAL PRIMARY KEY,\n");
            sb.append("  usuario_id INT NOT NULL REFERENCES usuario(id),\n");
            sb.append("  asunto     VARCHAR(150) NOT NULL,\n");
            sb.append("  cuerpo     TEXT,\n");
            sb.append("  reserva_id INT REFERENCES reserva(id) ON UPDATE CASCADE ON DELETE SET NULL,\n");
            sb.append("  enviado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP\n");
            sb.append(");\n\n");

            sb.append("-- SCRIPT DML: INSERTS DE RESERVAS (DATOS REALES)\n");
            sb.append("INSERT INTO reserva (laboratorio_id, solicitante_id, fecha, hora_inicio, hora_fin, tipo, motivo, estado, resuelto_por, observacion) VALUES\n");
            var rsResIns = st.executeQuery(
                    "SELECT l.codigo as lab, u.codigo as sol_cod, r.fecha, r.hora_inicio, r.hora_fin, "
                    + "r.tipo, r.motivo, r.estado, u_res.codigo as res_cod, r.observacion "
                    + "FROM reserva r JOIN laboratorio l ON r.laboratorio_id = l.id "
                    + "JOIN usuario u ON r.solicitante_id = u.id "
                    + "LEFT JOIN usuario u_res ON r.resuelto_por = u_res.id "
                    + "ORDER BY r.id ASC");
            boolean firstRes = true;
            while (rsResIns.next()) {
                if (!firstRes) sb.append(",\n");
                firstRes = false;
                String mot = rsResIns.getString("motivo") == null ? "NULL" : "'" + rsResIns.getString("motivo").replace("'", "''") + "'";
                String resCod = rsResIns.getString("res_cod") == null ? "NULL" : "(SELECT id FROM usuario WHERE codigo = '" + rsResIns.getString("res_cod") + "')";
                String obs = rsResIns.getString("observacion") == null ? "NULL" : "'" + rsResIns.getString("observacion").replace("'", "''") + "'";

                sb.append(String.format("  ((SELECT id FROM laboratorio WHERE codigo = '%s'), "
                        + "(SELECT id FROM usuario WHERE codigo = '%s'), "
                        + "'%s', '%s', '%s', '%s', %s, '%s', %s, %s)",
                        rsResIns.getString("lab"),
                        rsResIns.getString("sol_cod"),
                        rsResIns.getDate("fecha"),
                        rsResIns.getTime("hora_inicio"),
                        rsResIns.getTime("hora_fin"),
                        rsResIns.getString("tipo"),
                        mot,
                        rsResIns.getString("estado"),
                        resCod,
                        obs));
            }
            sb.append(";\n\n");

            sb.append("-- SCRIPT DML: INSERTS DE NOTIFICACIONES (DATOS REALES)\n");
            sb.append("INSERT INTO notificacion (usuario_id, asunto, cuerpo, reserva_id, enviado_en) VALUES\n");
            var rsNotIns = st.executeQuery(
                    "SELECT u.codigo as usu_cod, n.asunto, n.cuerpo, n.reserva_id, n.enviado_en "
                    + "FROM notificacion n JOIN usuario u ON n.usuario_id = u.id ORDER BY n.id ASC");
            boolean firstNot = true;
            while (rsNotIns.next()) {
                if (!firstNot) sb.append(",\n");
                firstNot = false;
                String cuerpo = rsNotIns.getString("cuerpo") == null ? "NULL" : "'" + rsNotIns.getString("cuerpo").replace("'", "''") + "'";
                String resId = rsNotIns.getString("reserva_id") == null ? "NULL" : rsNotIns.getString("reserva_id");

                sb.append(String.format("  ((SELECT id FROM usuario WHERE codigo = '%s'), '%s', %s, %s, '%s')",
                        rsNotIns.getString("usu_cod"),
                        rsNotIns.getString("asunto").replace("'", "''"),
                        cuerpo,
                        resId,
                        rsNotIns.getTimestamp("enviado_en")));
            }
            sb.append(";\n\n");

            java.nio.file.Path rutaTxt = Path.of("CU4_DISENO_Y_DATOS_RESERVAS.txt");
            java.nio.file.Files.writeString(rutaTxt, sb.toString(), java.nio.charset.StandardCharsets.UTF_8);
            System.out.println("Archivo CU4 generado exitosamente en: " + rutaTxt.toAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void exportarCU5() {
        try (java.sql.Connection cn = bo.uagrm.grupo08sa.datos.Conexion.getConexion();
             java.sql.Statement st = cn.createStatement()) {
            StringBuilder sb = new StringBuilder();
            sb.append("================================================================================\n");
            sb.append("          SISTEMA DE GESTION DE LABORATORIOS FICCT - GRUPO 08 SA\n");
            sb.append(" CASO DE USO 5 (CU5): GESTION DE PERMUTAS TEMPORALES DE HORARIO Y CHAT INTERNO\n");
            sb.append("================================================================================\n\n");

            sb.append("--------------------------------------------------------------------------------\n");
            sb.append("1. DISENO LOGICO (MODELO CONCEPTUAL / RELACIONAL)\n");
            sb.append("--------------------------------------------------------------------------------\n");
            sb.append("El modulo de Permutas de Horario y Mensajeria Interna (CU5) administra la asignacion\n");
            sb.append("recurrente de turnos de atencion a los auxiliares de laboratorio, el flujo formal de\n");
            sb.append("intercambio temporal de turnos en 3 fases (solicitud -> aceptacion -> aprobacion) y\n");
            sb.append("la comunicacion interna mediante mensajes entre auxiliares y con el Encargado.\n\n");

            sb.append("A. ENTIDADES Y DEFINICION LOGICA:\n");
            sb.append("  * TURNO (id, semestre_id, auxiliar_id, laboratorio_id, dia_semana, hora_inicio, hora_fin)\n");
            sb.append("    Horario semanal obligatorio de atencion asignado a un auxiliar en un laboratorio.\n\n");
            sb.append("  * PERMUTA (id, turno_id, solicitante_id, reemplazante_id, fecha, estado, aprobado_por, creado_en)\n");
            sb.append("    Acuerdo temporal y puntual para que un auxiliar cubra el turno de otro en una fecha dada.\n\n");
            sb.append("  * MENSAJE (id, remitente_id, destinatario_id, contenido, leido, enviado_en)\n");
            sb.append("    Mensajeria interna privada para coordinar relevos, permutas o consultas administrativas.\n\n");

            sb.append("B. RELACIONES Y CARDINALIDADES:\n");
            sb.append("  * SEMESTRE (1) ──── (N) TURNO\n");
            sb.append("    Cada turno pertenece a un periodo semestral especifico ('semestre_id').\n");
            sb.append("  * USUARIO [AUXILIAR] (1) ──── (N) TURNO\n");
            sb.append("    Un auxiliar tiene uno o varios turnos asignados a la semana ('auxiliar_id').\n");
            sb.append("  * LABORATORIO (1) ──── (N) TURNO (Opcional / Nulo si atiende modulo general)\n");
            sb.append("    El turno se cumple en un laboratorio especifico ('laboratorio_id').\n");
            sb.append("  * TURNO (1) ──── (N) PERMUTA\n");
            sb.append("    Un turno base puede ser permutado en diferentes fechas ('turno_id').\n");
            sb.append("  * USUARIO (1) ──── (N) PERMUTA (como solicitante y como reemplazante)\n");
            sb.append("    Participan el titular que cede el turno ('solicitante_id') y quien lo cubre ('reemplazante_id').\n");
            sb.append("  * USUARIO [ENCARGADO] (1) ──── (N) PERMUTA (Opcional mientras este PENDIENTE o ACEPTADA)\n");
            sb.append("    El Encargado autoriza o rechaza formalmente el relevo ('aprobado_por').\n");
            sb.append("  * USUARIO (1) ──── (N) MENSAJE (como remitente y como destinatario)\n");
            sb.append("    Relacion reflexiva de comunicacion entre usuarios autorizados ('remitente_id', 'destinatario_id').\n\n");

            sb.append("C. REGLAS DE NEGOCIO E INTEGRIDAD:\n");
            sb.append("  1. Flujo Estricto de 3 Fases: SOLPMT (solicitud por el titular) -> ACEPMT / RECPMT (aceptacion\n");
            sb.append("     o rechazo por el reemplazante) -> APRPMT / RECPMT (aprobacion o rechazo final por el Encargado).\n");
            sb.append("  2. Temporalidad y Fecha Unica: La permuta aplica exclusivamente para la fecha especificada.\n");
            sb.append("     No modifica la asignacion permanente del turno base en la tabla 'turno'.\n");
            sb.append("  3. Distincion Obligatoria de Actores: El solicitante no puede ser igual al reemplazante ('CHECK solicitante <> reemplazante').\n");
            sb.append("  4. No Choque de Turnos: El reemplazante no puede tener otro turno asignado en el mismo dia y franja horaria.\n");
            sb.append("  5. Impacto en Asistencia: Cuando una permuta esta 'APROBADA', la obligacion de marcar asistencia\n");
            sb.append("     (CU7) para ese turno y fecha se transfiere formalmente al reemplazante.\n");
            sb.append("  6. Estados Permitidos: 'PENDIENTE', 'ACEPTADA', 'APROBADA', 'RECHAZADA'.\n\n");

            sb.append("--------------------------------------------------------------------------------\n");
            sb.append("2. DISENO FISICO (DICCIONARIO DE DATOS EN POSTGRESQL)\n");
            sb.append("--------------------------------------------------------------------------------\n\n");

            sb.append("TABLA: turno\n");
            sb.append("+-------------+--------------+----------+---------------------------------------------------+\n");
            sb.append("| Columna     | Tipo         | Nulo     | Descripcion / Observacion                         |\n");
            sb.append("+-------------+--------------+----------+---------------------------------------------------+\n");
            sb.append("| id          | INTEGER      | NOT NULL | Clave primaria ( SERIAL )                         |\n");
            sb.append("| semestre_id | INTEGER      | NOT NULL | Clave foranea referencia a semestre(id)           |\n");
            sb.append("| auxiliar_id | INTEGER      | NOT NULL | Clave foranea referencia a usuario(id)            |\n");
            sb.append("| laboratorio_id| INTEGER    | Nulo     | Clave foranea referencia a laboratorio(id)        |\n");
            sb.append("| dia_semana  | SMALLINT     | NOT NULL | Dia: 1=Lun, 2=Mar, 3=Mie, 4=Jue, 5=Vie, 6=Sab     |\n");
            sb.append("| hora_inicio | TIME         | NOT NULL | Hora de inicio del turno                          |\n");
            sb.append("| hora_fin    | TIME         | NOT NULL | Hora de fin del turno (CHECK hora_fin > inicio)   |\n");
            sb.append("+-------------+--------------+----------+---------------------------------------------------+\n\n");

            sb.append("TABLA: permuta\n");
            sb.append("+-------------+--------------+----------+---------------------------------------------------+\n");
            sb.append("| Columna     | Tipo         | Nulo     | Descripcion / Observacion                         |\n");
            sb.append("+-------------+--------------+----------+---------------------------------------------------+\n");
            sb.append("| id          | INTEGER      | NOT NULL | Clave primaria ( SERIAL )                         |\n");
            sb.append("| turno_id    | INTEGER      | NOT NULL | Clave foranea referencia a turno(id)              |\n");
            sb.append("| solicitante_id| INTEGER    | NOT NULL | Clave foranea referencia a usuario(id)            |\n");
            sb.append("| reemplazante_id| INTEGER   | NOT NULL | Clave foranea referencia a usuario(id)            |\n");
            sb.append("| fecha       | DATE         | NOT NULL | Fecha especifica en que se realiza el relevo      |\n");
            sb.append("| estado      | VARCHAR(10)  | NOT NULL | PENDIENTE, ACEPTADA, APROBADA, RECHAZADA          |\n");
            sb.append("| aprobado_por| INTEGER      | Nulo     | Referencia a usuario(id) del Encargado aprobador  |\n");
            sb.append("| creado_en   | TIMESTAMP    | NOT NULL | Fecha y hora de creacion (DEFAULT CURRENT_TIME..) |\n");
            sb.append("+-------------+--------------+----------+---------------------------------------------------+\n");
            sb.append("  * Restriccion: CHECK (solicitante_id <> reemplazante_id)\n\n");

            sb.append("TABLA: mensaje\n");
            sb.append("+-------------+--------------+----------+---------------------------------------------------+\n");
            sb.append("| Columna     | Tipo         | Nulo     | Descripcion / Observacion                         |\n");
            sb.append("+-------------+--------------+----------+---------------------------------------------------+\n");
            sb.append("| id          | INTEGER      | NOT NULL | Clave primaria ( SERIAL )                         |\n");
            sb.append("| remitente_id| INTEGER      | NOT NULL | Clave foranea referencia a usuario(id)            |\n");
            sb.append("| destinatario_id| INTEGER   | NOT NULL | Clave foranea referencia a usuario(id)            |\n");
            sb.append("| contenido   | TEXT         | NOT NULL | Texto del mensaje enviado                         |\n");
            sb.append("| leido       | BOOLEAN      | NOT NULL | Estado de lectura (DEFAULT FALSE)                 |\n");
            sb.append("| enviado_en  | TIMESTAMP    | NOT NULL | Fecha y hora de envio (DEFAULT CURRENT_TIME..)    |\n");
            sb.append("+-------------+--------------+----------+---------------------------------------------------+\n\n");

            sb.append("--------------------------------------------------------------------------------\n");
            sb.append("3. TABLAS RELLENAS CON DATOS REALES DE LA BASE DE DATOS\n");
            sb.append("--------------------------------------------------------------------------------\n\n");

            sb.append("A. DATOS DE LA TABLA: turno (75 turnos de atencion de auxiliares)\n");
            sb.append("+----+---------+------------+------------+---------------------------------+-----------+-----------------+\n");
            sb.append("| id | gestion | lab        | cod_aux    | auxiliar                        | dia       | horario         |\n");
            sb.append("+----+---------+------------+------------+---------------------------------+-----------+-----------------+\n");
            String[] nombresDias = {"", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado"};
            var rsTur = st.executeQuery(
                    "SELECT t.id, s.gestion, COALESCE(l.codigo, 'Todos') as lab, u.codigo as aux_cod, "
                    + "u.nombre || ' ' || u.apellido as auxiliar, t.dia_semana, t.hora_inicio, t.hora_fin "
                    + "FROM turno t JOIN semestre s ON t.semestre_id = s.id "
                    + "JOIN usuario u ON t.auxiliar_id = u.id "
                    + "LEFT JOIN laboratorio l ON t.laboratorio_id = l.id "
                    + "ORDER BY t.dia_semana, t.hora_inicio, l.codigo");
            int totalTur = 0;
            while (rsTur.next()) {
                totalTur++;
                int dia = rsTur.getInt("dia_semana");
                String diaStr = (dia >= 1 && dia <= 6) ? nombresDias[dia] : String.valueOf(dia);
                String horario = rsTur.getTime("hora_inicio").toString().substring(0, 5) + " - " + rsTur.getTime("hora_fin").toString().substring(0, 5);
                sb.append(String.format("| %-2d | %-7s | %-10s | %-10s | %-31s | %-9s | %-15s |\n",
                        rsTur.getInt("id"), rsTur.getString("gestion"), rsTur.getString("lab"),
                        rsTur.getString("aux_cod"), rsTur.getString("auxiliar"), diaStr, horario));
            }
            sb.append("+----+---------+------------+------------+---------------------------------+-----------+-----------------+\n");
            sb.append(String.format("Total de turnos registrados: %d\n\n", totalTur));

            sb.append("B. DATOS DE LA TABLA: permuta (Permutas en epoca de examenes)\n");
            sb.append("+----+------------+----------+------------+---------------------------------+---------------------------------+------------+---------------------------------+\n");
            sb.append("| id | fecha      | turno_id | lab        | solicitante                     | reemplazante                    | estado     | aprobado_por                    |\n");
            sb.append("+----+------------+----------+------------+---------------------------------+---------------------------------+------------+---------------------------------+\n");
            var rsPmt = st.executeQuery(
                    "SELECT p.id, p.fecha, t.id as turno_id, COALESCE(l.codigo, 'Todos') as lab, "
                    + "u_sol.nombre || ' ' || u_sol.apellido as solicitante, "
                    + "u_ree.nombre || ' ' || u_ree.apellido as reemplazante, "
                    + "p.estado, COALESCE(u_ap.nombre || ' ' || u_ap.apellido, '-') as aprobado_por "
                    + "FROM permuta p JOIN turno t ON p.turno_id = t.id "
                    + "JOIN usuario u_sol ON p.solicitante_id = u_sol.id "
                    + "JOIN usuario u_ree ON p.reemplazante_id = u_ree.id "
                    + "LEFT JOIN laboratorio l ON t.laboratorio_id = l.id "
                    + "LEFT JOIN usuario u_ap ON p.aprobado_por = u_ap.id "
                    + "ORDER BY p.fecha, p.id");
            int totalPmt = 0;
            while (rsPmt.next()) {
                totalPmt++;
                sb.append(String.format("| %-2d | %-10s | %-8d | %-10s | %-31s | %-31s | %-10s | %-31s |\n",
                        rsPmt.getInt("id"), rsPmt.getDate("fecha"), rsPmt.getInt("turno_id"),
                        rsPmt.getString("lab"), rsPmt.getString("solicitante"),
                        rsPmt.getString("reemplazante"), rsPmt.getString("estado"),
                        rsPmt.getString("aprobado_por")));
            }
            sb.append("+----+------------+----------+------------+---------------------------------+---------------------------------+------------+---------------------------------+\n");
            sb.append(String.format("Total de permutas registradas: %d\n\n", totalPmt));

            sb.append("C. DATOS DE LA TABLA: mensaje (Chat interno de coordinacion)\n");
            sb.append("+----+---------------------------------+---------------------------------+------------------------------------------------------------+--------+---------------------+\n");
            sb.append("| id | remitente                       | destinatario                    | contenido                                                  | leido  | enviado_en          |\n");
            sb.append("+----+---------------------------------+---------------------------------+------------------------------------------------------------+--------+---------------------+\n");
            var rsMsj = st.executeQuery(
                    "SELECT m.id, u_rem.nombre || ' ' || u_rem.apellido as remitente, "
                    + "u_des.nombre || ' ' || u_des.apellido as destinatario, "
                    + "m.contenido, m.leido, m.enviado_en "
                    + "FROM mensaje m JOIN usuario u_rem ON m.remitente_id = u_rem.id "
                    + "JOIN usuario u_des ON m.destinatario_id = u_des.id ORDER BY m.id ASC");
            int totalMsj = 0;
            while (rsMsj.next()) {
                totalMsj++;
                String cont = rsMsj.getString("contenido").length() > 58 ? rsMsj.getString("contenido").substring(0, 55) + "..." : rsMsj.getString("contenido");
                sb.append(String.format("| %-2d | %-31s | %-31s | %-58s | %-6s | %-19s |\n",
                        rsMsj.getInt("id"), rsMsj.getString("remitente"),
                        rsMsj.getString("destinatario"), cont,
                        rsMsj.getBoolean("leido") ? "SI" : "NO", rsMsj.getTimestamp("enviado_en")));
            }
            sb.append("+----+---------------------------------+---------------------------------+------------------------------------------------------------+--------+---------------------+\n");
            sb.append(String.format("Total de mensajes registrados: %d\n\n", totalMsj));

            sb.append("--------------------------------------------------------------------------------\n");
            sb.append("4. SCRIPT SQL COMPLETO (DDL Y DML)\n");
            sb.append("--------------------------------------------------------------------------------\n");
            sb.append("-- SCRIPT DDL: CREACION DE TABLAS (CU5)\n");
            sb.append("CREATE TABLE turno (\n");
            sb.append("  id             SERIAL PRIMARY KEY,\n");
            sb.append("  semestre_id    INT NOT NULL REFERENCES semestre(id),\n");
            sb.append("  auxiliar_id    INT NOT NULL REFERENCES usuario(id),\n");
            sb.append("  laboratorio_id INT REFERENCES laboratorio(id),\n");
            sb.append("  dia_semana     SMALLINT NOT NULL CHECK (dia_semana BETWEEN 1 AND 6),\n");
            sb.append("  hora_inicio    TIME NOT NULL,\n");
            sb.append("  hora_fin       TIME NOT NULL,\n");
            sb.append("  CHECK (hora_fin > hora_inicio)\n");
            sb.append(");\n\n");

            sb.append("CREATE TABLE permuta (\n");
            sb.append("  id              SERIAL PRIMARY KEY,\n");
            sb.append("  turno_id        INT  NOT NULL REFERENCES turno(id),\n");
            sb.append("  solicitante_id  INT  NOT NULL REFERENCES usuario(id),\n");
            sb.append("  reemplazante_id INT  NOT NULL REFERENCES usuario(id),\n");
            sb.append("  fecha           DATE NOT NULL,\n");
            sb.append("  estado          VARCHAR(10) NOT NULL DEFAULT 'PENDIENTE'\n");
            sb.append("                  CHECK (estado IN ('PENDIENTE','ACEPTADA','APROBADA','RECHAZADA')),\n");
            sb.append("  aprobado_por    INT REFERENCES usuario(id),\n");
            sb.append("  creado_en       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,\n");
            sb.append("  CHECK (solicitante_id <> reemplazante_id)\n");
            sb.append(");\n\n");

            sb.append("CREATE TABLE mensaje (\n");
            sb.append("  id              SERIAL PRIMARY KEY,\n");
            sb.append("  remitente_id    INT  NOT NULL REFERENCES usuario(id),\n");
            sb.append("  destinatario_id INT  NOT NULL REFERENCES usuario(id),\n");
            sb.append("  contenido       TEXT NOT NULL,\n");
            sb.append("  leido           BOOLEAN NOT NULL DEFAULT FALSE,\n");
            sb.append("  enviado_en      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP\n");
            sb.append(");\n\n");

            sb.append("-- SCRIPT DML: INSERTS DE TURNOS (DATOS REALES)\n");
            sb.append("INSERT INTO turno (semestre_id, auxiliar_id, laboratorio_id, dia_semana, hora_inicio, hora_fin) VALUES\n");
            var rsTurIns = st.executeQuery(
                    "SELECT s.gestion, u.codigo as aux_cod, l.codigo as lab, t.dia_semana, t.hora_inicio, t.hora_fin "
                    + "FROM turno t JOIN semestre s ON t.semestre_id = s.id "
                    + "JOIN usuario u ON t.auxiliar_id = u.id "
                    + "LEFT JOIN laboratorio l ON t.laboratorio_id = l.id ORDER BY t.id");
            boolean firstTur = true;
            while (rsTurIns.next()) {
                if (!firstTur) sb.append(",\n");
                firstTur = false;
                String labSql = rsTurIns.getString("lab") == null ? "NULL" : "(SELECT id FROM laboratorio WHERE codigo = '" + rsTurIns.getString("lab") + "')";
                sb.append(String.format("  ((SELECT id FROM semestre WHERE gestion = '%s'), "
                        + "(SELECT id FROM usuario WHERE codigo = '%s'), "
                        + "%s, %d, '%s', '%s')",
                        rsTurIns.getString("gestion"),
                        rsTurIns.getString("aux_cod"),
                        labSql,
                        rsTurIns.getInt("dia_semana"),
                        rsTurIns.getTime("hora_inicio"),
                        rsTurIns.getTime("hora_fin")));
            }
            sb.append(";\n\n");

            sb.append("-- SCRIPT DML: INSERTS DE PERMUTAS (DATOS REALES)\n");
            sb.append("INSERT INTO permuta (turno_id, solicitante_id, reemplazante_id, fecha, estado, aprobado_por, creado_en) VALUES\n");
            var rsPmtIns = st.executeQuery(
                    "SELECT p.turno_id, u_sol.codigo as sol_cod, u_ree.codigo as ree_cod, "
                    + "p.fecha, p.estado, u_ap.codigo as ap_cod, p.creado_en "
                    + "FROM permuta p JOIN usuario u_sol ON p.solicitante_id = u_sol.id "
                    + "JOIN usuario u_ree ON p.reemplazante_id = u_ree.id "
                    + "LEFT JOIN usuario u_ap ON p.aprobado_por = u_ap.id ORDER BY p.id");
            boolean firstPmt = true;
            while (rsPmtIns.next()) {
                if (!firstPmt) sb.append(",\n");
                firstPmt = false;
                String apSql = rsPmtIns.getString("ap_cod") == null ? "NULL" : "(SELECT id FROM usuario WHERE codigo = '" + rsPmtIns.getString("ap_cod") + "')";
                sb.append(String.format("  (%d, (SELECT id FROM usuario WHERE codigo = '%s'), "
                        + "(SELECT id FROM usuario WHERE codigo = '%s'), "
                        + "'%s', '%s', %s, '%s')",
                        rsPmtIns.getInt("turno_id"),
                        rsPmtIns.getString("sol_cod"),
                        rsPmtIns.getString("ree_cod"),
                        rsPmtIns.getDate("fecha"),
                        rsPmtIns.getString("estado"),
                        apSql,
                        rsPmtIns.getTimestamp("creado_en")));
            }
            sb.append(";\n\n");

            sb.append("-- SCRIPT DML: INSERTS DE MENSAJES (DATOS REALES)\n");
            sb.append("INSERT INTO mensaje (remitente_id, destinatario_id, contenido, leido, enviado_en) VALUES\n");
            var rsMsjIns = st.executeQuery(
                    "SELECT u_rem.codigo as rem_cod, u_des.codigo as des_cod, m.contenido, m.leido, m.enviado_en "
                    + "FROM mensaje m JOIN usuario u_rem ON m.remitente_id = u_rem.id "
                    + "JOIN usuario u_des ON m.destinatario_id = u_des.id ORDER BY m.id");
            boolean firstMsj = true;
            while (rsMsjIns.next()) {
                if (!firstMsj) sb.append(",\n");
                firstMsj = false;
                sb.append(String.format("  ((SELECT id FROM usuario WHERE codigo = '%s'), "
                        + "(SELECT id FROM usuario WHERE codigo = '%s'), "
                        + "'%s', %s, '%s')",
                        rsMsjIns.getString("rem_cod"),
                        rsMsjIns.getString("des_cod"),
                        rsMsjIns.getString("contenido").replace("'", "''"),
                        rsMsjIns.getBoolean("leido") ? "TRUE" : "FALSE",
                        rsMsjIns.getTimestamp("enviado_en")));
            }
            sb.append(";\n\n");

            java.nio.file.Path rutaTxt = Path.of("CU5_DISENO_Y_DATOS_PERMUTAS_CHAT.txt");
            java.nio.file.Files.writeString(rutaTxt, sb.toString(), java.nio.charset.StandardCharsets.UTF_8);
            System.out.println("Archivo CU5 generado exitosamente en: " + rutaTxt.toAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void verificarTablas() {
        try (java.sql.Connection cn = bo.uagrm.grupo08sa.datos.Conexion.getConexion();
             java.sql.Statement st = cn.createStatement()) {
            java.sql.ResultSet rs = st.executeQuery(
                    "SELECT table_name FROM information_schema.tables WHERE table_schema = 'sislab' ORDER BY table_name");
            java.util.List<String> tablas = new java.util.ArrayList<>();
            while (rs.next()) {
                tablas.add(rs.getString(1));
            }
            System.out.println("==========================================================");
            System.out.println("  ESTADO DE TODAS LAS TABLAS EN LA BASE DE DATOS (sislab)");
            System.out.println("==========================================================");
            for (String t : tablas) {
                try (java.sql.ResultSet crs = st.executeQuery("SELECT COUNT(*) FROM sislab." + t)) {
                    if (crs.next()) {
                        int c = crs.getInt(1);
                        String estado = c > 0 ? "[LLENA] -> " + c + " filas" : "[VACIA] -> 0 filas";
                        System.out.printf("%-22s : %s%n", t, estado);
                    }
                }
            }
            System.out.println("==========================================================");
        } catch (Exception e) {
            System.err.println("Error al verificar tablas: " + e.getMessage());
        }
    }
}
