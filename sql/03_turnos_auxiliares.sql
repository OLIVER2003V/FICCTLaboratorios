-- ==========================================================
-- TURNOS DE AUXILIARES - SEMESTRE 2-2026
-- ==========================================================
SET search_path TO sislab;

-- 1. Asegurar que el semestre 2-2026 exista y esté activo
INSERT INTO semestre (gestion, fecha_inicio, fecha_fin, estado)
VALUES ('2-2026', '2026-08-03', '2026-12-19', 'ACTIVO')
ON CONFLICT (gestion) DO UPDATE SET estado = 'ACTIVO';

-- 2. Limpiar turnos anteriores de este semestre para evitar duplicados
DELETE FROM turno WHERE semestre_id = (SELECT id FROM semestre WHERE gestion = '2-2026');

-- ==========================================================
-- 1. MAÑANA (07:00 a 11:00) - LUNES A VIERNES
-- ==========================================================

-- 26693: Alicia Rivas Romana (LAB-45)
INSERT INTO turno (semestre_id, auxiliar_id, laboratorio_id, dia_semana, hora_inicio, hora_fin)
SELECT s.id, u.id, l.id, d, '07:00'::TIME, '11:00'::TIME
FROM semestre s, usuario u, laboratorio l, generate_series(1, 5) d
WHERE s.gestion = '2-2026' AND u.codigo = '26693' AND l.codigo = 'LAB-45';

-- 26700: Diego Rodrigo Ybarra Velasquez (LAB-41)
INSERT INTO turno (semestre_id, auxiliar_id, laboratorio_id, dia_semana, hora_inicio, hora_fin)
SELECT s.id, u.id, l.id, d, '07:00'::TIME, '11:00'::TIME
FROM semestre s, usuario u, laboratorio l, generate_series(1, 5) d
WHERE s.gestion = '2-2026' AND u.codigo = '26700' AND l.codigo = 'LAB-41';

-- 29532: Freddy Andre Padilla Mina (LAB-43)
INSERT INTO turno (semestre_id, auxiliar_id, laboratorio_id, dia_semana, hora_inicio, hora_fin)
SELECT s.id, u.id, l.id, d, '07:00'::TIME, '11:00'::TIME
FROM semestre s, usuario u, laboratorio l, generate_series(1, 5) d
WHERE s.gestion = '2-2026' AND u.codigo = '29532' AND l.codigo = 'LAB-43';

-- 29534: Jose Carlo Fernandez Rodas (LAB-44)
INSERT INTO turno (semestre_id, auxiliar_id, laboratorio_id, dia_semana, hora_inicio, hora_fin)
SELECT s.id, u.id, l.id, d, '07:00'::TIME, '11:00'::TIME
FROM semestre s, usuario u, laboratorio l, generate_series(1, 5) d
WHERE s.gestion = '2-2026' AND u.codigo = '29534' AND l.codigo = 'LAB-44';

-- ==========================================================
-- 2. MEDIO DÍA (11:00 a 15:00) - LUNES A VIERNES
-- ==========================================================

-- 29529: Camila Adriana Galviz Aramayo (LAB-43)
INSERT INTO turno (semestre_id, auxiliar_id, laboratorio_id, dia_semana, hora_inicio, hora_fin)
SELECT s.id, u.id, l.id, d, '11:00'::TIME, '15:00'::TIME
FROM semestre s, usuario u, laboratorio l, generate_series(1, 5) d
WHERE s.gestion = '2-2026' AND u.codigo = '29529' AND l.codigo = 'LAB-43';

-- 29528: Gabriel Andres Lopez Franco (LAB-43)
INSERT INTO turno (semestre_id, auxiliar_id, laboratorio_id, dia_semana, hora_inicio, hora_fin)
SELECT s.id, u.id, l.id, d, '11:00'::TIME, '15:00'::TIME
FROM semestre s, usuario u, laboratorio l, generate_series(1, 5) d
WHERE s.gestion = '2-2026' AND u.codigo = '29528' AND l.codigo = 'LAB-43';

-- 29396: Cesar Alejandro Caballero Caballero (LAB-41)
INSERT INTO turno (semestre_id, auxiliar_id, laboratorio_id, dia_semana, hora_inicio, hora_fin)
SELECT s.id, u.id, l.id, d, '11:00'::TIME, '15:00'::TIME
FROM semestre s, usuario u, laboratorio l, generate_series(1, 5) d
WHERE s.gestion = '2-2026' AND u.codigo = '29396' AND l.codigo = 'LAB-41';

-- 29531: Nicolas Junior Verduguez Teran (LAB-41)
INSERT INTO turno (semestre_id, auxiliar_id, laboratorio_id, dia_semana, hora_inicio, hora_fin)
SELECT s.id, u.id, l.id, d, '11:00'::TIME, '15:00'::TIME
FROM semestre s, usuario u, laboratorio l, generate_series(1, 5) d
WHERE s.gestion = '2-2026' AND u.codigo = '29531' AND l.codigo = 'LAB-41';

-- ==========================================================
-- 3. TARDE (15:00 a 19:00) - LUNES A VIERNES
-- ==========================================================

-- 29406: Arnulfo Velasquez Paz (LAB-46)
INSERT INTO turno (semestre_id, auxiliar_id, laboratorio_id, dia_semana, hora_inicio, hora_fin)
SELECT s.id, u.id, l.id, d, '15:00'::TIME, '19:00'::TIME
FROM semestre s, usuario u, laboratorio l, generate_series(1, 5) d
WHERE s.gestion = '2-2026' AND u.codigo = '29406' AND l.codigo = 'LAB-46';

-- 28081: Nataly Vanessa Martinez Martinez (LAB-44)
INSERT INTO turno (semestre_id, auxiliar_id, laboratorio_id, dia_semana, hora_inicio, hora_fin)
SELECT s.id, u.id, l.id, d, '15:00'::TIME, '19:00'::TIME
FROM semestre s, usuario u, laboratorio l, generate_series(1, 5) d
WHERE s.gestion = '2-2026' AND u.codigo = '28081' AND l.codigo = 'LAB-44';

-- 28087: Gilmer Luis Torrico Maquera (LAB-45)
INSERT INTO turno (semestre_id, auxiliar_id, laboratorio_id, dia_semana, hora_inicio, hora_fin)
SELECT s.id, u.id, l.id, d, '15:00'::TIME, '19:00'::TIME
FROM semestre s, usuario u, laboratorio l, generate_series(1, 5) d
WHERE s.gestion = '2-2026' AND u.codigo = '28087' AND l.codigo = 'LAB-45';

-- 29395: Carlos Leonel Barrios Mayorga (LAB-41)
INSERT INTO turno (semestre_id, auxiliar_id, laboratorio_id, dia_semana, hora_inicio, hora_fin)
SELECT s.id, u.id, l.id, d, '15:00'::TIME, '19:00'::TIME
FROM semestre s, usuario u, laboratorio l, generate_series(1, 5) d
WHERE s.gestion = '2-2026' AND u.codigo = '29395' AND l.codigo = 'LAB-41';

-- ==========================================================
-- 4. NOCHE (19:00 a 23:00) - LUNES A VIERNES
-- ==========================================================

-- 26150: Edmundo Junior Zenteno Rojas (LAB-45)
INSERT INTO turno (semestre_id, auxiliar_id, laboratorio_id, dia_semana, hora_inicio, hora_fin)
SELECT s.id, u.id, l.id, d, '19:00'::TIME, '23:00'::TIME
FROM semestre s, usuario u, laboratorio l, generate_series(1, 5) d
WHERE s.gestion = '2-2026' AND u.codigo = '26150' AND l.codigo = 'LAB-45';

-- 29405: Jairo Sanabria Ortega (LAB-43)
INSERT INTO turno (semestre_id, auxiliar_id, laboratorio_id, dia_semana, hora_inicio, hora_fin)
SELECT s.id, u.id, l.id, d, '19:00'::TIME, '23:00'::TIME
FROM semestre s, usuario u, laboratorio l, generate_series(1, 5) d
WHERE s.gestion = '2-2026' AND u.codigo = '29405' AND l.codigo = 'LAB-43';

-- 28207: Benjamin Condori Vasquez (LAB-41)
INSERT INTO turno (semestre_id, auxiliar_id, laboratorio_id, dia_semana, hora_inicio, hora_fin)
SELECT s.id, u.id, l.id, d, '19:00'::TIME, '23:00'::TIME
FROM semestre s, usuario u, laboratorio l, generate_series(1, 5) d
WHERE s.gestion = '2-2026' AND u.codigo = '28207' AND l.codigo = 'LAB-41';
