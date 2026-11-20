-- ==========================================================
-- ACTUALIZAR RESERVAS A TIPO 'DEFENSA DE PROYECTO'
-- ==========================================================
SET search_path TO sislab;

-- 1. Ampliar tamaño de columna tipo y actualizar el constraint
ALTER TABLE reserva ALTER COLUMN tipo TYPE VARCHAR(30);
ALTER TABLE reserva DROP CONSTRAINT IF EXISTS reserva_tipo_check;
ALTER TABLE reserva ADD CONSTRAINT reserva_tipo_check 
  CHECK (tipo IN ('EXAMEN','RECUPERACION','PRACTICA','EVENTO','DEFENSA DE PROYECTO','DEFENSA'));

-- 2. Eliminar las reservas de los días 17, 18 y 20 para actualizarlas limpiamente
DELETE FROM reserva 
WHERE laboratorio_id = (SELECT id FROM laboratorio WHERE codigo = 'LAB-44')
  AND fecha IN ('2026-11-17', '2026-11-18', '2026-11-20');

-- 3. Insertar las 3 reservas con tipo 'DEFENSA DE PROYECTO'
INSERT INTO reserva (laboratorio_id, solicitante_id, fecha, hora_inicio, hora_fin, tipo, motivo, estado, resuelto_por, observacion)
VALUES
  -- Martes 17 de noviembre
  ((SELECT id FROM laboratorio WHERE codigo = 'LAB-44'),
   (SELECT id FROM usuario WHERE codigo = '3768'),
   '2026-11-17', '07:00:00', '22:30:00', 'DEFENSA DE PROYECTO',
   'INF513 SC - TECNOLOGIA WEB',
   'APROBADA', (SELECT id FROM usuario WHERE codigo = '01'), 'Aprobado por Encargado de Laboratorios'),

  -- Miércoles 18 de noviembre
  ((SELECT id FROM laboratorio WHERE codigo = 'LAB-44'),
   (SELECT id FROM usuario WHERE codigo = '3768'),
   '2026-11-18', '07:00:00', '22:30:00', 'DEFENSA DE PROYECTO',
   'INF120 SA - TECNOLOGIA WEB',
   'APROBADA', (SELECT id FROM usuario WHERE codigo = '01'), 'Aprobado por Encargado de Laboratorios'),

  -- Viernes 20 de noviembre
  ((SELECT id FROM laboratorio WHERE codigo = 'LAB-44'),
   (SELECT id FROM usuario WHERE codigo = '3768'),
   '2026-11-20', '07:00:00', '22:30:00', 'DEFENSA DE PROYECTO',
   'INF120 SA - TECNOLOGIA WEB',
   'APROBADA', (SELECT id FROM usuario WHERE codigo = '01'), 'Aprobado por Encargado de Laboratorios');
