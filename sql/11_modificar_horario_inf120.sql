-- ==========================================================
-- ACTUALIZAR HORARIO INF120 GRUPO SA EN LAB-44 A 08:30 - 10:00
-- ==========================================================
SET search_path TO sislab;

UPDATE horario_clase
SET hora_inicio = '08:30:00',
    hora_fin = '10:00:00'
WHERE laboratorio_id = (SELECT id FROM laboratorio WHERE codigo = 'LAB-44')
  AND materia_id = (SELECT id FROM materia WHERE sigla = 'INF120')
  AND grupo = 'SA';
