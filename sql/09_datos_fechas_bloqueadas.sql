-- ==========================================================
-- FECHAS BLOQUEADAS REALISTAS DEL SEMESTRE 2-2026 (UAGRM / FICCT)
-- (Sin PSA a petición del usuario)
-- ==========================================================
SET search_path TO sislab;

-- Eliminar cualquier registro de tipo PSA previo si existiera
DELETE FROM fecha_bloqueada WHERE tipo = 'PSA';

INSERT INTO fecha_bloqueada (semestre_id, fecha, tipo, motivo, laboratorio_id)
SELECT (SELECT id FROM semestre WHERE gestion = '2-2026'), '2026-08-06', 'FERIADO', 'Día de la Independencia de Bolivia (Feriado Nacional)', NULL
WHERE NOT EXISTS (SELECT 1 FROM fecha_bloqueada WHERE fecha = '2026-08-06' AND laboratorio_id IS NULL);

INSERT INTO fecha_bloqueada (semestre_id, fecha, tipo, motivo, laboratorio_id)
SELECT (SELECT id FROM semestre WHERE gestion = '2-2026'), '2026-09-24', 'FERIADO', 'Efeméride Departamental de Santa Cruz (Feriado Cívico)', NULL
WHERE NOT EXISTS (SELECT 1 FROM fecha_bloqueada WHERE fecha = '2026-09-24' AND laboratorio_id IS NULL);

INSERT INTO fecha_bloqueada (semestre_id, fecha, tipo, motivo, laboratorio_id)
SELECT (SELECT id FROM semestre WHERE gestion = '2-2026'), '2026-11-02', 'FERIADO', 'Día de Todos los Santos (Feriado Nacional)', NULL
WHERE NOT EXISTS (SELECT 1 FROM fecha_bloqueada WHERE fecha = '2026-11-02' AND laboratorio_id IS NULL);

INSERT INTO fecha_bloqueada (semestre_id, fecha, tipo, motivo, laboratorio_id)
SELECT (SELECT id FROM semestre WHERE gestion = '2-2026'), '2026-09-21', 'OTRO', 'Día del Estudiante Universitario (Suspensión de actividades)', NULL
WHERE NOT EXISTS (SELECT 1 FROM fecha_bloqueada WHERE fecha = '2026-09-21' AND laboratorio_id IS NULL);

INSERT INTO fecha_bloqueada (semestre_id, fecha, tipo, motivo, laboratorio_id)
SELECT (SELECT id FROM semestre WHERE gestion = '2-2026'), '2026-09-12', 'MANTENIMIENTO', 'Mantenimiento preventivo de switch troncal y cableado estructurado', (SELECT id FROM laboratorio WHERE codigo = 'LAB-41')
WHERE NOT EXISTS (SELECT 1 FROM fecha_bloqueada WHERE fecha = '2026-09-12' AND laboratorio_id = (SELECT id FROM laboratorio WHERE codigo = 'LAB-41'));

INSERT INTO fecha_bloqueada (semestre_id, fecha, tipo, motivo, laboratorio_id)
SELECT (SELECT id FROM semestre WHERE gestion = '2-2026'), '2026-10-10', 'MANTENIMIENTO', 'Instalación de actualización de imágenes del sistema y software base', (SELECT id FROM laboratorio WHERE codigo = 'LAB-43')
WHERE NOT EXISTS (SELECT 1 FROM fecha_bloqueada WHERE fecha = '2026-10-10' AND laboratorio_id = (SELECT id FROM laboratorio WHERE codigo = 'LAB-43'));

INSERT INTO fecha_bloqueada (semestre_id, fecha, tipo, motivo, laboratorio_id)
SELECT (SELECT id FROM semestre WHERE gestion = '2-2026'), '2026-11-20', 'MANTENIMIENTO', 'Fumigación semestral y limpieza de equipos de climatización', NULL
WHERE NOT EXISTS (SELECT 1 FROM fecha_bloqueada WHERE fecha = '2026-11-20' AND laboratorio_id IS NULL);
