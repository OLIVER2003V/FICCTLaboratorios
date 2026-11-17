-- ==========================================================
-- ACTUALIZACIÓN Y ORDENAMIENTO DE INVENTARIO
-- Formato unificado para CPUs y Monitores:
--   - Menor activo: Docente (PC- DOC / MONITOR - DOC)
--   - Siguientes: Estudiantes (PC-01, PC-02... / MONITOR - 01, MONITOR - 02...)
-- ==========================================================
SET search_path TO sislab;

-- 1. Actualizar CPUs en todos los laboratorios
WITH cpus AS (
  SELECT id,
         laboratorio_id,
         activo,
         ROW_NUMBER() OVER (PARTITION BY laboratorio_id ORDER BY activo ASC) AS rn
  FROM sislab.equipo
  WHERE UPPER(descripcion) LIKE '%CPU%' 
     OR UPPER(codigo) LIKE '%PC-%' 
     OR UPPER(tipo) = 'COMPUTADORA'
)
UPDATE sislab.equipo e
SET descripcion = CASE 
                    WHEN c.rn = 1 THEN 'CPU (PC- DOC)'
                    ELSE 'CPU (PC-' || LPAD((c.rn - 1)::TEXT, 2, '0') || ')'
                  END
FROM cpus c
WHERE e.id = c.id;

-- 2. Actualizar Monitores en todos los laboratorios
WITH monitores AS (
  SELECT id,
         laboratorio_id,
         activo,
         ROW_NUMBER() OVER (PARTITION BY laboratorio_id ORDER BY activo ASC) AS rn
  FROM sislab.equipo
  WHERE (UPPER(descripcion) LIKE '%MONITOR%' OR UPPER(tipo) LIKE '%MONITOR%')
    AND UPPER(descripcion) NOT LIKE '%CPU%'
)
UPDATE sislab.equipo e
SET descripcion = CASE 
                    WHEN m.rn = 1 THEN 'MONITOR - DOC'
                    ELSE 'MONITOR - ' || LPAD((m.rn - 1)::TEXT, 2, '0')
                  END
FROM monitores m
WHERE e.id = m.id;

-- 3. Quitar 'MESA PARA COMPUTADORA' para que quede solo 'MESA' en todos los laboratorios
UPDATE sislab.equipo
SET descripcion = 'MESA'
WHERE UPPER(descripcion) LIKE 'MESA%';

-- 4. En LAB-42 dar de BAJA todos los CPUs y Monitores de estudiantes (conservando solo PC-DOC y MONITOR-DOC)
WITH cpus_lab42 AS (
  SELECT id,
         ROW_NUMBER() OVER (ORDER BY activo ASC) AS rn
  FROM sislab.equipo
  WHERE laboratorio_id = (SELECT id FROM sislab.laboratorio WHERE codigo = 'LAB-42' LIMIT 1)
    AND ((UPPER(descripcion) LIKE '%CPU%' OR UPPER(codigo) LIKE '%PC-%') AND UPPER(descripcion) NOT LIKE '%MONITOR%')
)
UPDATE sislab.equipo e
SET estado = 'BAJA'
FROM cpus_lab42 c
WHERE e.id = c.id AND c.rn > 1;

WITH mon_lab42 AS (
  SELECT id,
         ROW_NUMBER() OVER (ORDER BY activo ASC) AS rn
  FROM sislab.equipo
  WHERE laboratorio_id = (SELECT id FROM sislab.laboratorio WHERE codigo = 'LAB-42' LIMIT 1)
    AND UPPER(descripcion) LIKE '%MONITOR%'
)
UPDATE sislab.equipo e
SET estado = 'BAJA'
FROM mon_lab42 m
WHERE e.id = m.id AND m.rn > 1;

-- 5. En LAB-47 y LAB-48 dar de BAJA todos los equipos
UPDATE sislab.equipo
SET estado = 'BAJA'
WHERE laboratorio_id IN (
  SELECT id FROM sislab.laboratorio WHERE codigo IN ('LAB-47', 'LAB-48')
);
