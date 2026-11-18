-- ==========================================================
-- REORDENAR IDS DE RESERVAS CRONOLÓGICAMENTE
-- ==========================================================
SET search_path TO sislab;

-- 1. Asegurar que la clave foránea en notificacion tenga ON UPDATE CASCADE
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE constraint_name = 'notificacion_reserva_id_fkey' AND table_schema = 'sislab'
    ) THEN
        ALTER TABLE notificacion DROP CONSTRAINT notificacion_reserva_id_fkey;
    END IF;
END $$;

ALTER TABLE notificacion 
    ADD CONSTRAINT notificacion_reserva_id_fkey 
    FOREIGN KEY (reserva_id) REFERENCES reserva(id) 
    ON UPDATE CASCADE ON DELETE SET NULL;

-- 2. Crear tabla temporal con el nuevo ID cronológico
CREATE TEMP TABLE tmp_orden_reserva AS
SELECT id AS id_antiguo, 
       ROW_NUMBER() OVER (ORDER BY fecha ASC, hora_inicio ASC, id ASC) AS id_nuevo
FROM reserva;

-- 3. Asignar IDs temporales negativos para evitar colisiones únicas
UPDATE reserva r
SET id = -t.id_nuevo
FROM tmp_orden_reserva t
WHERE r.id = t.id_antiguo;

-- 4. Convertir a IDs positivos definitivos
UPDATE reserva
SET id = -id
WHERE id < 0;

-- 5. Sincronizar la secuencia reserva_id_seq con el valor máximo
SELECT setval('sislab.reserva_id_seq', COALESCE((SELECT MAX(id) FROM reserva), 1));

DROP TABLE IF EXISTS tmp_orden_reserva;
