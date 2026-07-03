-- ============================================================
-- Datos de prueba — ms-usuarios (Perfulandia SPA)
-- Contraseña de todos: Pass1234
-- Empleados → @perfulandia.cl  |  Cliente → @gmail.com
-- ============================================================

-- HU-09: Rol como ENTIDAD — seed de roles
INSERT IGNORE INTO rol (id, nombre, descripcion) VALUES
(1, 'ADMIN',     'Administrador del sistema — acceso total'),
(2, 'GERENTE',   'Gerente de sucursal — reportes y gestión'),
(3, 'EMPLEADO',  'Empleado de ventas — atención y ventas'),
(4, 'LOGISTICA', 'Personal de logística — envíos y rutas'),
(5, 'CLIENTE',   'Cliente web — compras y soporte');

INSERT IGNORE INTO usuarios (nombre, email, password, rol_id, estado, intentos_fallidos, fecha_registro) VALUES
('Admin',     'admin@perfulandia.cl',     '$2b$12$cNZ8L8xTbKH6BIpMt0TB/uk/PrbtNxvj7QxFUje1m1rTCefQ0/sMO', 1, 'ACTIVO', 0, NOW()),
('Gerente',   'gerente@perfulandia.cl',   '$2b$12$cNZ8L8xTbKH6BIpMt0TB/uk/PrbtNxvj7QxFUje1m1rTCefQ0/sMO', 2, 'ACTIVO', 0, NOW()),
('Empleado',  'empleado@perfulandia.cl',  '$2b$12$cNZ8L8xTbKH6BIpMt0TB/uk/PrbtNxvj7QxFUje1m1rTCefQ0/sMO', 3, 'ACTIVO', 0, NOW()),
('Logística', 'logistica@perfulandia.cl', '$2b$12$cNZ8L8xTbKH6BIpMt0TB/uk/PrbtNxvj7QxFUje1m1rTCefQ0/sMO', 4, 'ACTIVO', 0, NOW()),
('Cliente',   'cliente@gmail.com',        '$2b$12$cNZ8L8xTbKH6BIpMt0TB/uk/PrbtNxvj7QxFUje1m1rTCefQ0/sMO', 5, 'ACTIVO', 0, NOW());

-- ═══════════════════════════════════════════════════════════════
-- HU-09: Seed de Permisos y asignaciones por rol
-- ═══════════════════════════════════════════════════════════════

INSERT IGNORE INTO permiso (id, nombre, descripcion, modulo) VALUES
(1, 'GESTIONAR_USUARIOS',   'Crear, editar y desactivar usuarios',            'USUARIOS'),
(2, 'VER_REPORTES',         'Visualizar reportes de ventas e inventario',     'REPORTES'),
(3, 'GESTIONAR_TICKETS',    'Gestionar tickets de soporte al cliente',        'ATENCION_CLIENTE'),
(4, 'GESTIONAR_PRODUCTOS',  'Agregar, modificar y eliminar productos',        'PRODUCTOS'),
(5, 'GESTIONAR_INVENTARIO', 'Ajustar cantidades de stock',                    'PRODUCTOS'),
(6, 'GESTIONAR_PEDIDOS',    'Supervisar y autorizar pedidos',                 'ENVIOS'),
(7, 'GESTIONAR_ENVIOS',     'Crear y actualizar envíos',                      'ENVIOS'),
(8, 'GESTIONAR_VENTAS',     'Registrar transacciones de venta',               'VENTAS'),
(9, 'GESTIONAR_PROVEEDORES','Mantener información de proveedores',            'PROVEEDORES'),
(10,'GESTIONAR_PAGOS',      'Procesar pagos y facturación',                   'PAGOS'),
(11,'GESTIONAR_SUCURSALES', 'Configurar sucursales',                          'SUCURSALES'),
(12,'CONFIGURAR_PERMISOS',  'Asignar permisos a roles',                       'USUARIOS');

INSERT IGNORE INTO rol_permiso (id, rol, id_permiso) VALUES
-- ADMIN: todos los permisos
(1, 'ADMIN', 1), (2, 'ADMIN', 2), (3, 'ADMIN', 3), (4, 'ADMIN', 4),
(5, 'ADMIN', 5), (6, 'ADMIN', 6), (7, 'ADMIN', 7), (8, 'ADMIN', 8),
(9, 'ADMIN', 9), (10, 'ADMIN', 10), (11, 'ADMIN', 11), (12, 'ADMIN', 12),
-- GERENTE: reportes, productos, sucursales
(13, 'GERENTE', 2), (14, 'GERENTE', 4), (15, 'GERENTE', 5), (16, 'GERENTE', 6), (17, 'GERENTE', 11),
-- EMPLEADO: tickets, ventas, productos
(18, 'EMPLEADO', 3), (19, 'EMPLEADO', 8), (20, 'EMPLEADO', 4), (21, 'EMPLEADO', 5),
-- LOGISTICA: envios, pedidos, proveedores
(22, 'LOGISTICA', 6), (23, 'LOGISTICA', 7), (24, 'LOGISTICA', 9)
;
