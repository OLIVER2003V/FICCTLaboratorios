-- ==========================================================
-- REGISTRO DE RESERVAS PARA DOCENTE EVANS BALCAZAR EN LAB-44
-- ==========================================================
SET search_path TO sislab;

-- 1. Liberar la fecha 2026-11-20 de fecha_bloqueada para evitar conflicto con la reserva solicitada
DELETE FROM fecha_bloqueada WHERE fecha = '2026-11-20';

-- 2. Eliminar reservas previas duplicadas si existieran para esas fechas y lab
DELETE FROM reserva 
WHERE laboratorio_id = (SELECT id FROM laboratorio WHERE codigo = 'LAB-44')
  AND fecha IN ('2026-11-17', '2026-11-18', '2026-11-20');

-- 3. Insertar las 3 reservas solicitadas como APROBADAS
INSERT INTO reserva (laboratorio_id, solicitante_id, fecha, hora_inicio, hora_fin, tipo, motivo, estado, resuelto_por, observacion)
VALUES
  -- Martes 17 de noviembre
  ((SELECT id FROM laboratorio WHERE codigo = 'LAB-44'),
   (SELECT id FROM usuario WHERE codigo = '3768'),
   '2026-11-17', '07:00:00', '22:30:00', 'PRACTICA',
   'INF513 SC - TECNOLOGIA WEB (Jornada intensiva de laboratorio)',
   'APROBADA', (SELECT id FROM usuario WHERE codigo = '01'), 'Aprobado por Encargado de Laboratorios'),

  -- Miércoles 18 de noviembre
  ((SELECT id FROM laboratorio WHERE codigo = 'LAB-44'),
   (SELECT id FROM usuario WHERE codigo = '3768'),
   '2026-11-18', '07:00:00', '22:30:00', 'PRACTICA',
   'INF120 SA - PROGRAMACION I (Taller continuo y defensas de laboratorio)',
   'APROBADA', (SELECT id FROM usuario WHERE codigo = '01'), 'Aprobado por Encargado de Laboratorios'),

  -- Viernes 20 de noviembre
  ((SELECT id FROM laboratorio WHERE codigo = 'LAB-44'),
   (SELECT id FROM usuario WHERE codigo = '3768'),
   '2026-11-20', '07:00:00', '22:30:00', 'EXAMEN',
   'INF120 SA - PROGRAMACION I (Evaluación final y presentación de proyectos)',
   'APROBADA', (SELECT id FROM usuario WHERE codigo = '01'), 'Aprobado por Encargado de Laboratorios');
