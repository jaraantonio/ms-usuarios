-- ============================================
-- MS Usuarios y Autenticación - Perfulandia SPA
-- Script Flyway: V1__perfulandia_usuarios.sql
-- ============================================

-- Tabla principal de usuarios
CREATE TABLE IF NOT EXISTS usuarios (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    telefono VARCHAR(20),
    password VARCHAR(255) NOT NULL,
    rol ENUM('ADMIN', 'CLIENTE', 'GERENTE', 'EMPLEADO', 'LOGISTICA') NOT NULL,
    estado ENUM('ACTIVO', 'INACTIVO') NOT NULL DEFAULT 'ACTIVO',
    intentos_fallidos INT NOT NULL DEFAULT 0,
    direccion VARCHAR(500),
    id_sucursal_asignada BIGINT,
    metodo_pago_ofuscado VARCHAR(500),
    fecha_registro DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabla de tokens de recuperación de contraseña
CREATE TABLE IF NOT EXISTS tokens_recuperacion (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    token VARCHAR(255) NOT NULL UNIQUE,
    correo VARCHAR(150) NOT NULL,
    expiracion DATETIME NOT NULL,
    usado BOOLEAN NOT NULL DEFAULT FALSE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabla de tokens invalidados (blacklist)
CREATE TABLE IF NOT EXISTS tokens_invalidados (
    token VARCHAR(500) PRIMARY KEY,
    fecha_invalidacion DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;