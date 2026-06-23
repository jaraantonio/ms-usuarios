-- ============================================================
-- Datos de prueba — ms-usuarios (Perfulandia SPA)
-- Contraseña de todos: Pass1234
-- Empleados → @perfulandia.cl  |  Cliente → @gmail.com
-- ============================================================
INSERT IGNORE INTO usuarios (nombre, email, password, rol, estado, intentos_fallidos, fecha_registro) VALUES
('Admin',     'admin@perfulandia.cl',     '$2b$12$cNZ8L8xTbKH6BIpMt0TB/uk/PrbtNxvj7QxFUje1m1rTCefQ0/sMO', 'ADMIN',     'ACTIVO', 0, NOW()),
('Gerente',   'gerente@perfulandia.cl',   '$2b$12$cNZ8L8xTbKH6BIpMt0TB/uk/PrbtNxvj7QxFUje1m1rTCefQ0/sMO', 'GERENTE',   'ACTIVO', 0, NOW()),
('Empleado',  'empleado@perfulandia.cl',  '$2b$12$cNZ8L8xTbKH6BIpMt0TB/uk/PrbtNxvj7QxFUje1m1rTCefQ0/sMO', 'EMPLEADO',  'ACTIVO', 0, NOW()),
('Logística', 'logistica@perfulandia.cl', '$2b$12$cNZ8L8xTbKH6BIpMt0TB/uk/PrbtNxvj7QxFUje1m1rTCefQ0/sMO', 'LOGISTICA', 'ACTIVO', 0, NOW()),
('Cliente',   'cliente@gmail.com',        '$2b$12$cNZ8L8xTbKH6BIpMt0TB/uk/PrbtNxvj7QxFUje1m1rTCefQ0/sMO', 'CLIENTE',   'ACTIVO', 0, NOW());
