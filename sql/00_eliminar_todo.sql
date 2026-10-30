-- Borra todo el sistema (solo para reiniciar la base en desarrollo).
-- Solo afecta al esquema sislab; las tablas de "public" y "agendajet" no se tocan.
DROP SCHEMA IF EXISTS sislab CASCADE;
