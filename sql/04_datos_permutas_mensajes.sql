-- ==========================================================
-- DATOS DE PRUEBA: PERMUTAS Y MENSAJES (ÉPOCA DE EXÁMENES)
-- Semestre 2-2026 (Noviembre 2026)
-- ==========================================================
SET search_path TO sislab;

-- 1. Limpiar permutas y mensajes previos de prueba
DELETE FROM permuta;
DELETE FROM mensaje;

-- ==========================================================
-- TABLA PERMUTA (5 Permutas reales por época de exámenes)
-- ==========================================================

-- 1. LUNES 23/11/2026 (Día 1): Mañana 07:00 - 11:00 (LAB-44)
-- Solicitante: Jose Carlo Fernandez (29534)
-- Reemplazante: Jairo Sanabria (29405) [Tiene turno noche, no choca]
-- Motivo: Examen final de Inteligencia Artificial
-- Estado: APROBADA por el encargado
INSERT INTO permuta (turno_id, solicitante_id, reemplazante_id, fecha, estado, aprobado_por, creado_en)
SELECT t.id, u_sol.id, u_ree.id, '2026-11-23'::DATE, 'APROBADA', u_enc.id, CURRENT_TIMESTAMP - INTERVAL '2 days'
FROM turno t
JOIN usuario u_sol ON u_sol.codigo = '29534'
JOIN usuario u_ree ON u_ree.codigo = '29405'
CROSS JOIN (SELECT id FROM usuario WHERE correo = 'encargado.lab236@gmail.com' LIMIT 1) u_enc
WHERE t.auxiliar_id = u_sol.id AND t.dia_semana = 1
LIMIT 1;

-- 2. MARTES 24/11/2026 (Día 2): Mañana 07:00 - 11:00 (LAB-43)
-- Solicitante: Freddy Andre Padilla (29532)
-- Reemplazante: Gabriel Andres Lopez (29528) [Tiene turno mediodía, no choca]
-- Motivo: 2do Parcial de Sistemas Distribuidos
-- Estado: APROBADA por el encargado
INSERT INTO permuta (turno_id, solicitante_id, reemplazante_id, fecha, estado, aprobado_por, creado_en)
SELECT t.id, u_sol.id, u_ree.id, '2026-11-24'::DATE, 'APROBADA', u_enc.id, CURRENT_TIMESTAMP - INTERVAL '1 day'
FROM turno t
JOIN usuario u_sol ON u_sol.codigo = '29532'
JOIN usuario u_ree ON u_ree.codigo = '29528'
CROSS JOIN (SELECT id FROM usuario WHERE correo = 'encargado.lab236@gmail.com' LIMIT 1) u_enc
WHERE t.auxiliar_id = u_sol.id AND t.dia_semana = 2
LIMIT 1;

-- 3. MIÉRCOLES 25/11/2026 (Día 3): Mañana 07:00 - 11:00 (LAB-45)
-- Solicitante: Alicia Rivas Romana (26693)
-- Reemplazante: Camila Adriana Galviz (29529) [Tiene turno mediodía, no choca]
-- Motivo: Presentación de proyecto de Redes I
-- Estado: ACEPTADA (por el reemplazante, pendiente de aprobación del encargado)
INSERT INTO permuta (turno_id, solicitante_id, reemplazante_id, fecha, estado, aprobado_por, creado_en)
SELECT t.id, u_sol.id, u_ree.id, '2026-11-25'::DATE, 'ACEPTADA', NULL, CURRENT_TIMESTAMP - INTERVAL '12 hours'
FROM turno t
JOIN usuario u_sol ON u_sol.codigo = '26693'
JOIN usuario u_ree ON u_ree.codigo = '29529'
WHERE t.auxiliar_id = u_sol.id AND t.dia_semana = 3
LIMIT 1;

-- 4. JUEVES 26/11/2026 (Día 4): Tarde 15:00 - 19:00 (LAB-46)
-- Solicitante: Arnulfo Velasquez Paz (29406)
-- Reemplazante: Edmundo Junior Zenteno (26150) [Tiene turno noche, no choca]
-- Motivo: Examen final de Compiladores
-- Estado: PENDIENTE (esperando respuesta del reemplazante)
INSERT INTO permuta (turno_id, solicitante_id, reemplazante_id, fecha, estado, aprobado_por, creado_en)
SELECT t.id, u_sol.id, u_ree.id, '2026-11-26'::DATE, 'PENDIENTE', NULL, CURRENT_TIMESTAMP - INTERVAL '3 hours'
FROM turno t
JOIN usuario u_sol ON u_sol.codigo = '29406'
JOIN usuario u_ree ON u_ree.codigo = '26150'
WHERE t.auxiliar_id = u_sol.id AND t.dia_semana = 4
LIMIT 1;

-- 5. VIERNES 27/11/2026 (Día 5): Noche 19:00 - 23:00 (LAB-43)
-- Solicitante: Jairo Sanabria Ortega (29405)
-- Reemplazante: Cesar Alejandro Caballero (29396) [Tiene turno mediodía, no choca]
-- Motivo: Examen final de Taller de Grado
-- Estado: APROBADA por el encargado
INSERT INTO permuta (turno_id, solicitante_id, reemplazante_id, fecha, estado, aprobado_por, creado_en)
SELECT t.id, u_sol.id, u_ree.id, '2026-11-27'::DATE, 'APROBADA', u_enc.id, CURRENT_TIMESTAMP - INTERVAL '4 hours'
FROM turno t
JOIN usuario u_sol ON u_sol.codigo = '29405'
JOIN usuario u_ree ON u_ree.codigo = '29396'
CROSS JOIN (SELECT id FROM usuario WHERE correo = 'encargado.lab236@gmail.com' LIMIT 1) u_enc
WHERE t.auxiliar_id = u_sol.id AND t.dia_semana = 5
LIMIT 1;


-- ==========================================================
-- TABLA MENSAJE (5 Mensajes reales coordinando turnos y exámenes)
-- ==========================================================

-- 1. Coordinación de la permuta del Lunes
INSERT INTO mensaje (remitente_id, destinatario_id, contenido, leido, enviado_en)
SELECT u_sol.id, u_ree.id, 
  'Hola Jairo, tengo examen final de Inteligencia Artificial el lunes 23 a las 8am. ¿Me puedes cubrir en Lab 44? Ya te envié la solicitud SOLPMT.',
  TRUE, CURRENT_TIMESTAMP - INTERVAL '2 days'
FROM usuario u_sol, usuario u_ree
WHERE u_sol.codigo = '29534' AND u_ree.codigo = '29405';

-- 2. Respuesta de confirmación
INSERT INTO mensaje (remitente_id, destinatario_id, contenido, leido, enviado_en)
SELECT u_ree.id, u_sol.id, 
  'Listo Jose Carlo, ya acepté la permuta en el sistema. Éxitos en tu examen.',
  TRUE, CURRENT_TIMESTAMP - INTERVAL '2 days' + INTERVAL '30 minutes'
FROM usuario u_sol, usuario u_ree
WHERE u_sol.codigo = '29534' AND u_ree.codigo = '29405';

-- 3. Aviso al Encargado
INSERT INTO mensaje (remitente_id, destinatario_id, contenido, leido, enviado_en)
SELECT u_sol.id, u_enc.id, 
  'Buenas tardes estimado encargado, por motivo de examen parcial el martes 24 solicité permuta con Gabriel Lopez. Quedamos atentos a su aprobación.',
  TRUE, CURRENT_TIMESTAMP - INTERVAL '1 day'
FROM usuario u_sol, (SELECT id FROM usuario WHERE correo = 'encargado.lab236@gmail.com' LIMIT 1) u_enc
WHERE u_sol.codigo = '29532';

-- 4. Notificación de consulta de Alicia a Camila
INSERT INTO mensaje (remitente_id, destinatario_id, contenido, leido, enviado_en)
SELECT u_sol.id, u_ree.id, 
  'Hola Camila, tengo defensa de proyecto de Redes el miércoles 25 por la mañana. Te pasé la permuta para cubrir Lab 45. Por favor confírmame.',
  FALSE, CURRENT_TIMESTAMP - INTERVAL '12 hours'
FROM usuario u_sol, usuario u_ree
WHERE u_sol.codigo = '26693' AND u_ree.codigo = '29529';

-- 5. Recordatorio general del Encargado a los auxiliares
INSERT INTO mensaje (remitente_id, destinatario_id, contenido, leido, enviado_en)
SELECT u_enc.id, u_aux.id, 
  'Recordatorio general: Por semana de exámenes finales, registrar con anticipación sus permutas mediante SOLPMT para no perjudicar la atención en laboratorios.',
  FALSE, CURRENT_TIMESTAMP - INTERVAL '5 hours'
FROM (SELECT id FROM usuario WHERE correo = 'encargado.lab236@gmail.com' LIMIT 1) u_enc,
     usuario u_aux
WHERE u_aux.codigo = '29405';
