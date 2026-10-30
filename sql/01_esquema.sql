-- Sistema vía Mail - Gestión de Laboratorios (Grupo 08 SA)
-- Base de datos: db_grupo08sa (PostgreSQL), esquema sislab
-- Se usa un esquema propio porque "public" ya tiene tablas de otros ejemplos (persona, permiso, ...)
CREATE SCHEMA IF NOT EXISTS sislab;
SET search_path TO sislab;

-- Seguridad y usuarios (CU1, CU2)
CREATE TABLE rol (
  id          SERIAL PRIMARY KEY,
  codigo      VARCHAR(3)  NOT NULL UNIQUE,
  nombre      VARCHAR(50) NOT NULL,
  descripcion VARCHAR(200)
);

CREATE TABLE comando (
  id          SERIAL PRIMARY KEY,
  nombre      VARCHAR(10)  NOT NULL UNIQUE,
  descripcion VARCHAR(200) NOT NULL
);

CREATE TABLE rol_comando (
  rol_id     INT NOT NULL REFERENCES rol(id),
  comando_id INT NOT NULL REFERENCES comando(id),
  PRIMARY KEY (rol_id, comando_id)
);

CREATE TABLE usuario (
  id        SERIAL PRIMARY KEY,
  codigo    VARCHAR(20)  NOT NULL UNIQUE,
  nombre    VARCHAR(60)  NOT NULL,
  apellido  VARCHAR(60)  NOT NULL,
  correo    VARCHAR(100) NOT NULL UNIQUE,
  telefono  VARCHAR(20),
  rol_id    INT NOT NULL REFERENCES rol(id),
  estado    VARCHAR(10) NOT NULL DEFAULT 'ACTIVO'
            CHECK (estado IN ('ACTIVO','INACTIVO')),
  creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Configuración semestral (CU2)
CREATE TABLE laboratorio (
  id        SERIAL PRIMARY KEY,
  codigo    VARCHAR(10) NOT NULL UNIQUE,
  nombre    VARCHAR(60) NOT NULL,
  capacidad INT NOT NULL CHECK (capacidad > 0),
  ubicacion VARCHAR(100),
  estado    VARCHAR(15) NOT NULL DEFAULT 'DISPONIBLE'
            CHECK (estado IN ('DISPONIBLE','MANTENIMIENTO','INHABILITADO'))
);

CREATE TABLE semestre (
  id           SERIAL PRIMARY KEY,
  gestion      VARCHAR(10) NOT NULL UNIQUE,
  fecha_inicio DATE NOT NULL,
  fecha_fin    DATE NOT NULL,
  estado       VARCHAR(12) NOT NULL DEFAULT 'PLANIFICADO'
               CHECK (estado IN ('PLANIFICADO','ACTIVO','CERRADO')),
  CHECK (fecha_fin > fecha_inicio)
);
-- Solo puede haber un semestre activo
CREATE UNIQUE INDEX ux_semestre_activo ON semestre (estado) WHERE estado = 'ACTIVO';

CREATE TABLE fecha_bloqueada (
  id             SERIAL PRIMARY KEY,
  semestre_id    INT  NOT NULL REFERENCES semestre(id),
  fecha          DATE NOT NULL,
  tipo           VARCHAR(15) NOT NULL
                 CHECK (tipo IN ('FERIADO','PSA','MANTENIMIENTO','OTRO')),
  motivo         VARCHAR(200),
  laboratorio_id INT REFERENCES laboratorio(id)  -- NULL: afecta a todos
);

-- Carga horaria base (CU3)
CREATE TABLE materia (
  id     SERIAL PRIMARY KEY,
  sigla  VARCHAR(10)  NOT NULL UNIQUE,
  nombre VARCHAR(100) NOT NULL
);

CREATE TABLE horario_clase (
  id             SERIAL PRIMARY KEY,
  semestre_id    INT NOT NULL REFERENCES semestre(id),
  laboratorio_id INT NOT NULL REFERENCES laboratorio(id),
  materia_id     INT NOT NULL REFERENCES materia(id),
  docente_id     INT NOT NULL REFERENCES usuario(id),
  grupo          VARCHAR(5) NOT NULL,
  dia_semana     SMALLINT NOT NULL CHECK (dia_semana BETWEEN 1 AND 6),
  hora_inicio    TIME NOT NULL,
  hora_fin       TIME NOT NULL,
  CHECK (hora_fin > hora_inicio)
);

-- Reservas y notificaciones (CU4)
CREATE TABLE reserva (
  id             SERIAL PRIMARY KEY,
  laboratorio_id INT  NOT NULL REFERENCES laboratorio(id),
  solicitante_id INT  NOT NULL REFERENCES usuario(id),
  fecha          DATE NOT NULL,
  hora_inicio    TIME NOT NULL,
  hora_fin       TIME NOT NULL,
  tipo           VARCHAR(15) NOT NULL
                 CHECK (tipo IN ('EXAMEN','RECUPERACION','PRACTICA','EVENTO')),
  motivo         VARCHAR(200),
  estado         VARCHAR(10) NOT NULL DEFAULT 'PENDIENTE'
                 CHECK (estado IN ('PENDIENTE','APROBADA','RECHAZADA','CANCELADA')),
  resuelto_por   INT REFERENCES usuario(id),
  observacion    VARCHAR(200),
  creado_en      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CHECK (hora_fin > hora_inicio)
);

CREATE TABLE notificacion (
  id         SERIAL PRIMARY KEY,
  usuario_id INT NOT NULL REFERENCES usuario(id),
  asunto     VARCHAR(150) NOT NULL,
  cuerpo     TEXT,
  reserva_id INT REFERENCES reserva(id),
  enviado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Turnos, permutas, chat y asistencia (CU5, CU7)
CREATE TABLE turno (
  id             SERIAL PRIMARY KEY,
  semestre_id    INT NOT NULL REFERENCES semestre(id),
  auxiliar_id    INT NOT NULL REFERENCES usuario(id),
  laboratorio_id INT REFERENCES laboratorio(id),
  dia_semana     SMALLINT NOT NULL CHECK (dia_semana BETWEEN 1 AND 6),
  hora_inicio    TIME NOT NULL,
  hora_fin       TIME NOT NULL,
  CHECK (hora_fin > hora_inicio)
);

CREATE TABLE permuta (
  id              SERIAL PRIMARY KEY,
  turno_id        INT  NOT NULL REFERENCES turno(id),
  solicitante_id  INT  NOT NULL REFERENCES usuario(id),
  reemplazante_id INT  NOT NULL REFERENCES usuario(id),
  fecha           DATE NOT NULL,
  estado          VARCHAR(10) NOT NULL DEFAULT 'PENDIENTE'
                  CHECK (estado IN ('PENDIENTE','ACEPTADA','APROBADA','RECHAZADA')),
  aprobado_por    INT REFERENCES usuario(id),
  creado_en       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CHECK (solicitante_id <> reemplazante_id)
);

CREATE TABLE mensaje (
  id              SERIAL PRIMARY KEY,
  remitente_id    INT  NOT NULL REFERENCES usuario(id),
  destinatario_id INT  NOT NULL REFERENCES usuario(id),
  contenido       TEXT NOT NULL,
  leido           BOOLEAN NOT NULL DEFAULT FALSE,
  enviado_en      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE asistencia (
  id           SERIAL PRIMARY KEY,
  turno_id     INT  NOT NULL REFERENCES turno(id),
  auxiliar_id  INT  NOT NULL REFERENCES usuario(id),
  fecha        DATE NOT NULL,
  hora_entrada TIME,
  hora_salida  TIME,
  estado       VARCHAR(10) NOT NULL
               CHECK (estado IN ('PUNTUAL','ATRASO','FALTA','PERMISO')),
  UNIQUE (turno_id, fecha)
);

CREATE TABLE permiso (
  id             SERIAL PRIMARY KEY,
  auxiliar_id    INT  NOT NULL REFERENCES usuario(id),
  fecha          DATE NOT NULL,
  motivo         VARCHAR(200) NOT NULL,
  registrado_por INT  NOT NULL REFERENCES usuario(id)
);

-- Inventario, incidencias y objetos encontrados (CU6)
CREATE TABLE equipo (
  id             SERIAL PRIMARY KEY,
  codigo         VARCHAR(20) NOT NULL UNIQUE,
  laboratorio_id INT NOT NULL REFERENCES laboratorio(id),
  tipo           VARCHAR(15) NOT NULL
                 CHECK (tipo IN ('COMPUTADORA','PROYECTOR','COMPONENTE','SOFTWARE','OTRO')),
  descripcion    VARCHAR(200) NOT NULL,
  estado         VARCHAR(15) NOT NULL DEFAULT 'OPERATIVO'
                 CHECK (estado IN ('OPERATIVO','EN_REPARACION','BAJA'))
);

CREATE TABLE incidencia (
  id             SERIAL PRIMARY KEY,
  equipo_id      INT NOT NULL REFERENCES equipo(id),
  reportado_por  INT NOT NULL REFERENCES usuario(id),
  descripcion    VARCHAR(300) NOT NULL,
  estado         VARCHAR(12) NOT NULL DEFAULT 'PENDIENTE'
                 CHECK (estado IN ('PENDIENTE','EN_ATENCION','RESUELTA')),
  solucion       VARCHAR(300),
  fecha_reporte  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  fecha_solucion TIMESTAMP
);

CREATE TABLE objeto_encontrado (
  id             SERIAL PRIMARY KEY,
  laboratorio_id INT  NOT NULL REFERENCES laboratorio(id),
  descripcion    VARCHAR(200) NOT NULL,
  fecha_hallazgo DATE NOT NULL,
  registrado_por INT  NOT NULL REFERENCES usuario(id),
  estado         VARCHAR(12) NOT NULL DEFAULT 'EN_CUSTODIA'
                 CHECK (estado IN ('EN_CUSTODIA','ENTREGADO')),
  entregado_a    VARCHAR(100),
  ci_receptor    VARCHAR(15),
  fecha_entrega  TIMESTAMP
);

-- Registro de comandos procesados (todos los CU)
CREATE TABLE bitacora (
  id        SERIAL PRIMARY KEY,
  remitente VARCHAR(100) NOT NULL,
  comando   VARCHAR(10),
  asunto    VARCHAR(300) NOT NULL,
  resultado VARCHAR(10) NOT NULL CHECK (resultado IN ('OK','ERROR','DENEGADO')),
  detalle   VARCHAR(300),
  fecha     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Datos iniciales
INSERT INTO rol (codigo, nombre, descripcion) VALUES
 ('ENC','Encargado de Laboratorios','Administra el sistema'),
 ('DOC','Docente','Solicita reservas y reporta incidencias'),
 ('AUX','Auxiliar','Atiende los laboratorios en su turno'),
 ('DIR','Director de Carrera','Solicita reservas y consulta reportes');

INSERT INTO comando (nombre, descripcion) VALUES
 ('INSUSU','Registrar usuario'), ('MODUSU','Modificar usuario'),
 ('BAJUSU','Dar de baja usuario'), ('ACTUSU','Reactivar usuario'),
 ('LISUSU','Listar usuarios'), ('ASICOM','Asignar comandos a un rol'),
 ('INSLAB','Registrar laboratorio'), ('MODLAB','Modificar laboratorio'),
 ('LISLAB','Listar laboratorios'), ('INSSEM','Registrar semestre'),
 ('ACTSEM','Activar semestre'), ('CERSEM','Cerrar semestre'),
 ('INSFEB','Registrar fecha bloqueada'), ('LISFEB','Listar fechas bloqueadas'),
 ('INSHOR','Registrar clase'), ('CARHOR','Cargar horario desde Excel'),
 ('MODHOR','Modificar clase'), ('ELIHOR','Eliminar clase'),
 ('LISHOR','Listar horario'), ('CONDIS','Consultar disponibilidad'),
 ('INSRES','Solicitar reserva'), ('APRRES','Aprobar reserva'),
 ('RECRES','Rechazar reserva'), ('CANRES','Cancelar reserva'),
 ('LISRES','Listar reservas'), ('SOLPMT','Solicitar permuta'),
 ('ACEPMT','Aceptar permuta'), ('APRPMT','Aprobar permuta'),
 ('RECPMT','Rechazar permuta'), ('LISPMT','Listar permutas'),
 ('ENVMSJ','Enviar mensaje interno'), ('LISMSJ','Listar mensajes'),
 ('INSEQU','Registrar equipo'), ('MODEQU','Modificar equipo'),
 ('BAJEQU','Dar de baja equipo'), ('LISEQU','Listar inventario'),
 ('INSINC','Reportar incidencia'), ('MODINC','Actualizar incidencia'),
 ('LISINC','Listar incidencias'), ('INSOBJ','Registrar objeto encontrado'),
 ('ENTOBJ','Registrar entrega de objeto'), ('LISOBJ','Listar objetos encontrados'),
 ('INSTUR','Asignar turno'), ('LISTUR','Listar turnos'),
 ('MARENT','Registrar entrada'), ('MARSAL','Registrar salida'),
 ('INSPMS','Registrar permiso'), ('LISASI','Consultar asistencia'),
 ('REPOCU','Reporte de ocupación'), ('REPRES','Reporte de reservas'),
 ('REPINC','Reporte de incidencias'), ('REPASI','Reporte de asistencia'),
 ('ESTSEM','Estadísticas del semestre'), ('AYUDA','Listar comandos permitidos');

-- El Encargado puede ejecutar todos los comandos
INSERT INTO rol_comando (rol_id, comando_id)
SELECT r.id, c.id FROM rol r CROSS JOIN comando c WHERE r.codigo = 'ENC';

-- Comandos de los demás roles
-- (corrección: AUX también tiene RECPMT, para que el reemplazante pueda rechazar una permuta)
INSERT INTO rol_comando (rol_id, comando_id)
SELECT r.id, c.id FROM rol r JOIN comando c ON
 (r.codigo = 'DOC' AND c.nombre IN ('LISLAB','LISHOR','CONDIS','INSRES','CANRES',
   'LISRES','INSINC','LISINC','ENVMSJ','LISMSJ','AYUDA'))
 OR (r.codigo = 'AUX' AND c.nombre IN ('LISLAB','LISHOR','CONDIS','INSRES','CANRES',
   'LISRES','SOLPMT','ACEPMT','RECPMT','LISPMT','ENVMSJ','LISMSJ','INSINC','LISINC','INSOBJ',
   'ENTOBJ','LISOBJ','LISTUR','MARENT','MARSAL','AYUDA'))
 OR (r.codigo = 'DIR' AND c.nombre IN ('LISLAB','LISHOR','CONDIS','INSRES','CANRES',
   'LISRES','ENVMSJ','LISMSJ','REPOCU','REPRES','REPINC','REPASI','ESTSEM','AYUDA'));
