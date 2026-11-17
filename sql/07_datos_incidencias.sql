-- ==========================================================
-- DATOS DE INCIDENCIAS REALES PARA LABORATORIOS FICCT
-- 18 incidencias distribuidas entre PENDIENTE, EN_ATENCION y RESUELTA
-- ==========================================================
SET search_path TO sislab;

INSERT INTO incidencia (equipo_id, reportado_por, descripcion, estado, solucion, fecha_reporte, fecha_solucion)
VALUES
(
  COALESCE((SELECT id FROM equipo WHERE laboratorio_id = (SELECT id FROM laboratorio WHERE codigo = 'LAB-41') AND (UPPER(descripcion) LIKE '%CPU%' OR UPPER(tipo) = 'COMPUTADORA') ORDER BY id LIMIT 1 OFFSET 0), (SELECT id FROM equipo LIMIT 1 OFFSET 0)),
  COALESCE((SELECT id FROM usuario WHERE correo = 'vortiz@gmail.com'), 1),
  'La PC no enciende luego de corte de energía eléctrica; no da señales de corriente ni giran ventiladores.',
  'RESUELTA',
  'Se sustituyó fuente de poder ATX de 500W dañada y se verificó encendido y voltajes normales.',
  CURRENT_TIMESTAMP - INTERVAL '12 days 4 hours',
  CURRENT_TIMESTAMP - INTERVAL '11 days 2 hours'
),
(
  COALESCE((SELECT id FROM equipo WHERE laboratorio_id = (SELECT id FROM laboratorio WHERE codigo = 'LAB-41') AND (UPPER(descripcion) LIKE '%MONITOR%' OR UPPER(tipo) LIKE '%MONITOR%') ORDER BY id LIMIT 1 OFFSET 1), (SELECT id FROM equipo LIMIT 1 OFFSET 1)),
  COALESCE((SELECT id FROM usuario WHERE correo = 'jchahin@gmail.com'), 1),
  'La pantalla parpadea con líneas horizontales intermitentes al mover el cable de video.',
  'RESUELTA',
  'Se reemplazó cable de video VGA con pines doblados por cable nuevo de alta resolución.',
  CURRENT_TIMESTAMP - INTERVAL '10 days 3 hours',
  CURRENT_TIMESTAMP - INTERVAL '9 days 5 hours'
),
(
  COALESCE((SELECT id FROM equipo WHERE laboratorio_id = (SELECT id FROM laboratorio WHERE codigo = 'LAB-43') AND (UPPER(descripcion) LIKE '%CPU%' OR UPPER(tipo) = 'COMPUTADORA') ORDER BY id LIMIT 1 OFFSET 2), (SELECT id FROM equipo LIMIT 1 OFFSET 2)),
  COALESCE((SELECT id FROM usuario WHERE rol_id = (SELECT id FROM rol WHERE codigo = 'AUX') ORDER BY id LIMIT 1 OFFSET 0), 1),
  'Se reinicia súbitamente a los 10 minutos de uso continuo por sobrecalentamiento del procesador.',
  'RESUELTA',
  'Se realizó limpieza profunda del cooler/disipador y aplicación de pasta térmica gris.',
  CURRENT_TIMESTAMP - INTERVAL '8 days 6 hours',
  CURRENT_TIMESTAMP - INTERVAL '7 days 1 hour'
),
(
  COALESCE((SELECT id FROM equipo WHERE laboratorio_id = (SELECT id FROM laboratorio WHERE codigo = 'LAB-44') AND (UPPER(descripcion) LIKE '%CPU%' OR UPPER(tipo) = 'COMPUTADORA') ORDER BY id LIMIT 1 OFFSET 3), (SELECT id FROM equipo LIMIT 1 OFFSET 3)),
  COALESCE((SELECT id FROM usuario WHERE correo = 'sdavalos@gmail.com'), 1),
  'Presenta pantalla azul (BSOD: MEMORY_MANAGEMENT) al abrir entornos NetBeans y navegadores.',
  'EN_ATENCION',
  'En diagnóstico con MemTest86 en taller de soporte; módulo DDR4 de 8GB en verificación.',
  CURRENT_TIMESTAMP - INTERVAL '6 days 8 hours',
  NULL
),
(
  COALESCE((SELECT id FROM equipo WHERE laboratorio_id = (SELECT id FROM laboratorio WHERE codigo = 'LAB-44') AND (UPPER(descripcion) LIKE '%ESTABILIZADOR%' OR id IS NOT NULL) ORDER BY id LIMIT 1 OFFSET 4), (SELECT id FROM equipo LIMIT 1 OFFSET 4)),
  COALESCE((SELECT id FROM usuario WHERE rol_id = (SELECT id FROM rol WHERE codigo = 'AUX') ORDER BY id LIMIT 1 OFFSET 1), 1),
  'Estabilizador emite zumbido continuo muy fuerte y calienta de forma excesiva al conectar la PC.',
  'EN_ATENCION',
  'Desconectado por seguridad; derivado al área técnica para cambio de relé o reemplazo de unidad.',
  CURRENT_TIMESTAMP - INTERVAL '5 days 2 hours',
  NULL
),
(
  COALESCE((SELECT id FROM equipo WHERE laboratorio_id = (SELECT id FROM laboratorio WHERE codigo = 'LAB-45') AND (UPPER(descripcion) LIKE '%CPU%' OR UPPER(tipo) = 'COMPUTADORA') ORDER BY id LIMIT 1 OFFSET 5), (SELECT id FROM equipo LIMIT 1 OFFSET 5)),
  COALESCE((SELECT id FROM usuario WHERE correo = 'rzuna@gmail.com'), 1),
  'Error de inicio en BIOS: "Reboot and Select proper Boot device", no detecta la unidad SSD.',
  'EN_ATENCION',
  'Se verificaron cables SATA y de poder; pendiente clonación e instalación de nueva unidad SSD Kingston.',
  CURRENT_TIMESTAMP - INTERVAL '4 days 5 hours',
  NULL
),
(
  COALESCE((SELECT id FROM equipo WHERE laboratorio_id = (SELECT id FROM laboratorio WHERE codigo = 'LAB-43') AND (UPPER(descripcion) LIKE '%CPU%' OR UPPER(tipo) = 'COMPUTADORA') ORDER BY id LIMIT 1 OFFSET 0), (SELECT id FROM equipo LIMIT 1 OFFSET 6)),
  COALESCE((SELECT id FROM usuario WHERE correo = 'jcortez@gmail.com'), 1),
  'Los puertos USB frontales están dañados físicamente y no reconocen memorias flash de los docentes.',
  'RESUELTA',
  'Se desconectó panel frontal roto y se instaló extensor USB 3.0 directo desde placa trasera.',
  CURRENT_TIMESTAMP - INTERVAL '7 days 2 hours',
  CURRENT_TIMESTAMP - INTERVAL '6 days 4 hours'
),
(
  COALESCE((SELECT id FROM equipo WHERE laboratorio_id = (SELECT id FROM laboratorio WHERE codigo = 'LAB-42') AND (UPPER(descripcion) LIKE '%CPU%' OR UPPER(tipo) = 'COMPUTADORA') ORDER BY id LIMIT 1 OFFSET 0), (SELECT id FROM equipo LIMIT 1 OFFSET 7)),
  COALESCE((SELECT id FROM usuario WHERE correo = 'evargas@gmail.com'), 1),
  'El teclado y mouse inalámbricos no responden o tienen excesivo retraso al escribir.',
  'RESUELTA',
  'Se cambiaron baterías alcalinas AAA y se reubicó receptor USB en puerto frontal sin interferencias.',
  CURRENT_TIMESTAMP - INTERVAL '5 days 10 hours',
  CURRENT_TIMESTAMP - INTERVAL '5 days 1 hour'
),
(
  COALESCE((SELECT id FROM equipo WHERE laboratorio_id = (SELECT id FROM laboratorio WHERE codigo = 'LAB-41') AND (UPPER(descripcion) LIKE '%CPU%' OR UPPER(tipo) = 'COMPUTADORA') ORDER BY id LIMIT 1 OFFSET 8), (SELECT id FROM equipo LIMIT 1 OFFSET 8)),
  COALESCE((SELECT id FROM usuario WHERE rol_id = (SELECT id FROM rol WHERE codigo = 'AUX') ORDER BY id LIMIT 1 OFFSET 2), 1),
  'La PC enciende pero emite 3 pitidos cortos y no envía señal de video a la pantalla.',
  'PENDIENTE',
  NULL,
  CURRENT_TIMESTAMP - INTERVAL '3 days 7 hours',
  NULL
),
(
  COALESCE((SELECT id FROM equipo WHERE laboratorio_id = (SELECT id FROM laboratorio WHERE codigo = 'LAB-44') AND (UPPER(descripcion) LIKE '%MONITOR%' OR UPPER(tipo) LIKE '%MONITOR%') ORDER BY id LIMIT 1 OFFSET 9), (SELECT id FROM equipo LIMIT 1 OFFSET 9)),
  COALESCE((SELECT id FROM usuario WHERE correo = 'cmiranda@gmail.com'), 1),
  'El botón de encendido frontal del monitor está hundido y no se puede apagar ni encender fácilmente.',
  'PENDIENTE',
  NULL,
  CURRENT_TIMESTAMP - INTERVAL '2 days 3 hours',
  NULL
),
(
  COALESCE((SELECT id FROM equipo WHERE laboratorio_id = (SELECT id FROM laboratorio WHERE codigo = 'LAB-45') AND (UPPER(descripcion) LIKE '%CPU%' OR UPPER(tipo) = 'COMPUTADORA') ORDER BY id LIMIT 1 OFFSET 10), (SELECT id FROM equipo LIMIT 1 OFFSET 10)),
  COALESCE((SELECT id FROM usuario WHERE rol_id = (SELECT id FROM rol WHERE codigo = 'AUX') ORDER BY id LIMIT 1 OFFSET 3), 1),
  'Conector de red RJ-45 roto en el extremo del patchcord; pierde conexión a internet al menor movimiento.',
  'RESUELTA',
  'Se recrimpó nuevo conector RJ-45 categoría 6 con capuchón protector y se certificó enlace gigabit.',
  CURRENT_TIMESTAMP - INTERVAL '4 days 7 hours',
  CURRENT_TIMESTAMP - INTERVAL '4 days 2 hours'
),
(
  COALESCE((SELECT id FROM equipo WHERE laboratorio_id = (SELECT id FROM laboratorio WHERE codigo = 'LAB-46') AND (UPPER(descripcion) LIKE '%CPU%' OR UPPER(tipo) = 'COMPUTADORA') ORDER BY id LIMIT 1 OFFSET 11), (SELECT id FROM equipo LIMIT 1 OFFSET 11)),
  COALESCE((SELECT id FROM usuario WHERE correo = 'amollo@gmail.com'), 1),
  'Sistema operativo Fedora Linux no arranca tras actualización incompleta; entra a modo emergencia.',
  'EN_ATENCION',
  'Auxiliar de turno realizando recuperación de paquetes rpm y reconstrucción de imagen initramfs.',
  CURRENT_TIMESTAMP - INTERVAL '1 day 6 hours',
  NULL
),
(
  COALESCE((SELECT id FROM equipo WHERE laboratorio_id = (SELECT id FROM laboratorio WHERE codigo = 'LAB-44') AND (UPPER(descripcion) LIKE '%CPU%' OR UPPER(tipo) = 'COMPUTADORA') ORDER BY id LIMIT 1 OFFSET 12), (SELECT id FROM equipo LIMIT 1 OFFSET 12)),
  COALESCE((SELECT id FROM usuario WHERE rol_id = (SELECT id FROM rol WHERE codigo = 'AUX') ORDER BY id LIMIT 1 OFFSET 4), 1),
  'Ventilador posterior del gabinete produce ruido chirriante constante que perturba las clases.',
  'PENDIENTE',
  NULL,
  CURRENT_TIMESTAMP - INTERVAL '1 day 2 hours',
  NULL
),
(
  COALESCE((SELECT id FROM equipo WHERE (UPPER(descripcion) LIKE '%PROYECTOR%' OR UPPER(tipo) LIKE '%PROYECTOR%' OR id IS NOT NULL) ORDER BY id LIMIT 1 OFFSET 13), (SELECT id FROM equipo LIMIT 1 OFFSET 13)),
  COALESCE((SELECT id FROM usuario WHERE correo = 'vortiz@gmail.com'), 1),
  'El proyector muestra luz indicadora de "LAMP" en rojo y se apaga automáticamente a los 5 minutos.',
  'PENDIENTE',
  NULL,
  CURRENT_TIMESTAMP - INTERVAL '18 hours',
  NULL
),
(
  COALESCE((SELECT id FROM equipo WHERE laboratorio_id = (SELECT id FROM laboratorio WHERE codigo = 'LAB-43') AND (UPPER(descripcion) LIKE '%MONITOR%' OR UPPER(tipo) LIKE '%MONITOR%') ORDER BY id LIMIT 1 OFFSET 14), (SELECT id FROM equipo LIMIT 1 OFFSET 14)),
  COALESCE((SELECT id FROM usuario WHERE rol_id = (SELECT id FROM rol WHERE codigo = 'AUX') ORDER BY id LIMIT 1 OFFSET 5), 1),
  'Monitor pierde el color azul, toda la imagen se visualiza con tonalidad amarilla/rojiza.',
  'RESUELTA',
  'Se reemplazó cable VGA defectuoso y se restablecieron perfiles de color en configuración del monitor.',
  CURRENT_TIMESTAMP - INTERVAL '3 days 4 hours',
  CURRENT_TIMESTAMP - INTERVAL '2 days 6 hours'
),
(
  COALESCE((SELECT id FROM equipo WHERE laboratorio_id = (SELECT id FROM laboratorio WHERE codigo = 'LAB-45') AND (UPPER(descripcion) LIKE '%CPU%' OR UPPER(tipo) = 'COMPUTADORA') ORDER BY id LIMIT 1 OFFSET 15), (SELECT id FROM equipo LIMIT 1 OFFSET 15)),
  COALESCE((SELECT id FROM usuario WHERE correo = 'encargado.lab236@gmail.com'), 1),
  'Teclas "Shift", "Espacio" y "Ctrl" no responden en el teclado USB del equipo.',
  'PENDIENTE',
  NULL,
  CURRENT_TIMESTAMP - INTERVAL '6 hours',
  NULL
),
(
  COALESCE((SELECT id FROM equipo WHERE laboratorio_id = (SELECT id FROM laboratorio WHERE codigo = 'LAB-41') AND (UPPER(descripcion) LIKE '%CPU%' OR UPPER(tipo) = 'COMPUTADORA') ORDER BY id LIMIT 1 OFFSET 16), (SELECT id FROM equipo LIMIT 1 OFFSET 16)),
  COALESCE((SELECT id FROM usuario WHERE correo = 'jchahin@gmail.com'), 1),
  'Tarjeta de red integrada no detecta cable de red; sin conexión física (link apagado).',
  'RESUELTA',
  'Se instaló tarjeta de red PCIe Gigabit TP-Link y se deshabilitó NIC dañada en BIOS.',
  CURRENT_TIMESTAMP - INTERVAL '6 days 3 hours',
  CURRENT_TIMESTAMP - INTERVAL '5 days 8 hours'
),
(
  COALESCE((SELECT id FROM equipo WHERE laboratorio_id = (SELECT id FROM laboratorio WHERE codigo = 'LAB-44') AND (UPPER(descripcion) LIKE '%CPU%' OR UPPER(tipo) = 'COMPUTADORA') ORDER BY id LIMIT 1 OFFSET 17), (SELECT id FROM equipo LIMIT 1 OFFSET 17)),
  COALESCE((SELECT id FROM usuario WHERE rol_id = (SELECT id FROM rol WHERE codigo = 'AUX') ORDER BY id LIMIT 1 OFFSET 6), 1),
  'Mouse óptico tiene la rueda de scroll trabada y el botón izquierdo hace doble clic involuntario.',
  'RESUELTA',
  'Se entregó mouse óptico USB nuevo del stock de repuestos de almacén.',
  CURRENT_TIMESTAMP - INTERVAL '2 days 8 hours',
  CURRENT_TIMESTAMP - INTERVAL '2 days 1 hour'
);
