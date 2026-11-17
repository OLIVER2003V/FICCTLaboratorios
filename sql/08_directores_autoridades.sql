-- ==========================================================
-- REGISTRO / ACTUALIZACION DE AUTORIDADES Y DIRECTORES (ROL: DIR)
-- ==========================================================
SET search_path TO sislab;

-- 1. Decano: MSc. Ing. Miguel Jesús Peinado Pereira (Código: D01)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM usuario WHERE correo = 'mpeinado@gmail.com' OR codigo IN ('2623', 'D01')) THEN
        UPDATE usuario 
        SET codigo = 'D01',
            nombre = 'MIGUEL JESUS',
            apellido = 'PEINADO PEREIRA',
            correo = 'mpeinado@gmail.com',
            telefono = COALESCE(telefono, '70002623'),
            rol_id = (SELECT id FROM rol WHERE codigo = 'DIR'),
            estado = 'ACTIVO'
        WHERE correo = 'mpeinado@gmail.com' OR codigo IN ('2623', 'D01');
    ELSE
        INSERT INTO usuario (codigo, nombre, apellido, correo, telefono, rol_id, estado)
        VALUES ('D01', 'MIGUEL JESUS', 'PEINADO PEREIRA', 'mpeinado@gmail.com', '70002623',
                (SELECT id FROM rol WHERE codigo = 'DIR'), 'ACTIVO');
    END IF;
END $$;

-- 2. Vicedecano: MSc. Ing. Mauricio Caballero Rúa (Código: D02)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM usuario WHERE correo = 'mcaballero@gmail.com' OR codigo IN ('8160', 'D02')) THEN
        UPDATE usuario 
        SET codigo = 'D02',
            nombre = 'MAURICIO CHRISTI',
            apellido = 'CABALLERO RUA',
            correo = 'mcaballero@gmail.com',
            telefono = COALESCE(telefono, '70008160'),
            rol_id = (SELECT id FROM rol WHERE codigo = 'DIR'),
            estado = 'ACTIVO'
        WHERE correo = 'mcaballero@gmail.com' OR codigo IN ('8160', 'D02');
    ELSE
        INSERT INTO usuario (codigo, nombre, apellido, correo, telefono, rol_id, estado)
        VALUES ('D02', 'MAURICIO CHRISTI', 'CABALLERO RUA', 'mcaballero@gmail.com', '70008160',
                (SELECT id FROM rol WHERE codigo = 'DIR'), 'ACTIVO');
    END IF;
END $$;
