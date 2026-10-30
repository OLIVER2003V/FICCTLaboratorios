-- Encargado inicial: sin él nadie puede ejecutar INSUSU (solo el rol ENC puede registrar usuarios)
SET search_path TO sislab;
INSERT INTO usuario (codigo, nombre, apellido, correo, telefono, rol_id)
SELECT '222058730', 'Oliver', 'Ventura Valverde', 'venturavalverdeo@gmail.com', NULL, id
FROM rol WHERE codigo = 'ENC';
