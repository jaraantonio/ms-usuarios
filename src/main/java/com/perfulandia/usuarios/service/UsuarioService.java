package com.perfulandia.usuarios.service;

import com.perfulandia.usuarios.client.NotificacionesWebClient;
import com.perfulandia.usuarios.exception.CredencialesInvalidasException;
import com.perfulandia.usuarios.exception.RecursoDuplicadoException;
import com.perfulandia.usuarios.exception.RecursoNoEncontradoException;
import com.perfulandia.usuarios.model.dto.*;
import com.perfulandia.usuarios.model.entity.Rol;
import com.perfulandia.usuarios.model.entity.TokenRecuperacion;
import com.perfulandia.usuarios.model.entity.Usuario;
import com.perfulandia.usuarios.model.enums.EstadoUsuario;
import com.perfulandia.usuarios.repository.RolRepository;
import com.perfulandia.usuarios.repository.TokenRecuperacionRepository;
import com.perfulandia.usuarios.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final TokenRecuperacionRepository tokenRecuperacionRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificacionesWebClient notificacionesWebClient;

    // ============================================================
    // HU-01: Registro de cliente
    // ============================================================
    @Transactional
    public PerfilResponseDTO registrarCliente(RegistroRequestDTO dto) {
        log.info("Registrando cliente con email: {}", dto.email());

        if (usuarioRepository.findByEmail(dto.email()).isPresent()) {
            throw new RecursoDuplicadoException("El correo ya está registrado");
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(dto.nombre());
        usuario.setEmail(dto.email());
        usuario.setTelefono(dto.telefono());
        usuario.setPassword(passwordEncoder.encode(dto.password()));
        usuario.setRol(rolRepository.findByNombre("CLIENTE")
                .orElseThrow(() -> new RuntimeException("Rol CLIENTE no encontrado en BD")));
        usuario.setEstado(EstadoUsuario.ACTIVO);
        usuario.setDireccion(dto.direccion());
        // HU-01: Solo ofuscar si hay método de pago; null si no se proporcionó
        usuario.setMetodoPagoOfuscado(null);

        Usuario guardado = usuarioRepository.save(usuario);
        log.info("Cliente registrado con ID: {}", guardado.getId());
        return toPerfilResponse(guardado);
    }

    // ============================================================
    // HU-02: Login con bloqueo tras 3 intentos
    // ============================================================
    @Transactional(noRollbackFor = CredencialesInvalidasException.class)
    public LoginResponseDTO autenticar(LoginRequestDTO dto) {
        log.info("Intento de login para: {}", dto.email());

        Usuario usuario = usuarioRepository.findByEmail(dto.email())
                .orElseThrow(() -> new CredencialesInvalidasException("Correo o contraseña incorrectos"));

        if (usuario.getEstado() == EstadoUsuario.INACTIVO) {
            throw new CredencialesInvalidasException("Cuenta bloqueada. Contacte al administrador.");
        }

        if (!passwordEncoder.matches(dto.password(), usuario.getPassword())) {
            usuario.setIntentosFallidos(usuario.getIntentosFallidos() + 1);
            if (usuario.getIntentosFallidos() >= 3) {
                usuario.setEstado(EstadoUsuario.INACTIVO);
                log.warn("Cuenta bloqueada por 3 intentos fallidos: {}", usuario.getEmail());
            }
            usuarioRepository.save(usuario);
            throw new CredencialesInvalidasException("Correo o contraseña incorrectos");
        }

        // Resetear intentos fallidos en login exitoso
        usuario.setIntentosFallidos(0);
        usuarioRepository.save(usuario);

        log.info("Login exitoso para: {}", usuario.getEmail());
        return new LoginResponseDTO(usuario.getRol().getNombre());
    }

    // ============================================================
    // HU-03: Obtener perfil
    // ============================================================
    @Transactional(readOnly = true)
    public PerfilResponseDTO obtenerPerfil(Long id) {
        log.info("Consultando perfil de usuario ID: {}", id);
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado con ID: " + id));
        return toPerfilResponse(usuario);
    }

    // ============================================================
    // HU-04: Actualizar perfil
    // ============================================================
    @Transactional
    public PerfilResponseDTO actualizarPerfil(Long id, ActualizarPerfilDTO dto) {
        log.info("Actualizando perfil de usuario ID: {}", id);
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado con ID: " + id));

        usuario.setNombre(dto.nombre());
        usuario.setDireccion(dto.direccion());

        if (dto.nuevoMetodoPago() != null && !dto.nuevoMetodoPago().isBlank()) {
            String ofuscado = "**** **** **** " +
                    dto.nuevoMetodoPago().substring(Math.max(0, dto.nuevoMetodoPago().length() - 4));
            usuario.setMetodoPagoOfuscado(ofuscado);
        }

        Usuario guardado = usuarioRepository.save(usuario);
        log.info("Perfil actualizado exitosamente para ID: {}", id);
        return toPerfilResponse(guardado);
    }

    // ============================================================
    // HU-CP: Cambiar contraseña (usuario autenticado)
    // ============================================================
    @Transactional
    public void cambiarPassword(Long id, CambiarPasswordRequestDTO dto) {
        log.info("Cambiando contraseña para usuario ID: {}", id);
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado con ID: " + id));

        if (!passwordEncoder.matches(dto.passwordActual(), usuario.getPassword())) {
            throw new CredencialesInvalidasException("La contraseña actual no es correcta");
        }

        usuario.setPassword(passwordEncoder.encode(dto.nuevaPassword()));
        usuarioRepository.save(usuario);
        log.info("Contraseña cambiada exitosamente para ID: {}", id);
    }

    // ============================================================
    // HU-44a: Recuperar contraseña
    // ============================================================
    @Transactional
    public String recuperarPassword(String email) {
        log.info("Solicitud de recuperación de contraseña para: {}", email);

        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email);
        if (usuarioOpt.isPresent()) {
            String token = UUID.randomUUID().toString();
            TokenRecuperacion tr = new TokenRecuperacion();
            tr.setToken(token);
            tr.setCorreo(email);
            tr.setExpiracion(LocalDateTime.now().plusMinutes(15));
            tr.setUsado(false);
            tokenRecuperacionRepository.save(tr);
            log.info("Token de recuperación generado para: {}", email);

            try {
                notificacionesWebClient.enviarNotificacion(new CorreoRequestDTO(email), token);
                log.info("Notificación de recuperación registrada para: {}", email);
            } catch (Exception e) {
                log.error("No se pudo registrar la notificación de recuperación para {}: {}", email, e.getMessage());
            }

            return "Si el correo está registrado, recibirás un enlace de recuperación.";
        }

        return "Si el correo está registrado, recibirás un enlace de recuperación.";
    }

    // ============================================================
    // HU-44b: Restablecer contraseña (con token)
    // ============================================================
    @Transactional
    public void restablecerPassword(RestablecerPasswordRequestDTO dto) {
        log.info("Restableciendo contraseña con token");

        TokenRecuperacion tr = tokenRecuperacionRepository.findByToken(dto.token())
                .orElseThrow(() -> new IllegalArgumentException("Token inválido o expirado"));

        if (tr.isUsado()) {
            throw new IllegalArgumentException("El token ya ha sido utilizado");
        }

        if (tr.getExpiracion().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("El token ha expirado");
        }

        Usuario usuario = usuarioRepository.findByEmail(tr.getCorreo())
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        usuario.setPassword(passwordEncoder.encode(dto.nuevaContrasena()));
        usuario.setIntentosFallidos(0);
        usuario.setEstado(EstadoUsuario.ACTIVO);
        usuarioRepository.save(usuario);

        tr.setUsado(true);
        tokenRecuperacionRepository.save(tr);

        log.info("Contraseña restablecida exitosamente para: {}", tr.getCorreo());
    }

    // ============================================================
    // HU-06: Listar usuarios (ADMIN)
    // ============================================================
    @Transactional(readOnly = true)
    public List<PerfilResponseDTO> listarUsuarios(String rol, String estado) {
        log.info("Listando usuarios - rol: {}, estado: {}", rol, estado);

        List<Usuario> usuarios;

        if (rol != null && estado != null) {
            Rol rolEntity = rolRepository.findByNombre(rol)
                    .orElseThrow(() -> new IllegalArgumentException("Rol inválido: " + rol));
            usuarios = usuarioRepository.findByRolAndEstado(
                    rolEntity, EstadoUsuario.valueOf(estado));
        } else if (rol != null) {
            Rol rolEntity = rolRepository.findByNombre(rol)
                    .orElseThrow(() -> new IllegalArgumentException("Rol inválido: " + rol));
            usuarios = usuarioRepository.findByRol(rolEntity);
        } else if (estado != null) {
            usuarios = usuarioRepository.findByEstado(EstadoUsuario.valueOf(estado));
        } else {
            usuarios = usuarioRepository.findAll();
        }

        return usuarios.stream().map(this::toPerfilResponse).collect(Collectors.toList());
    }

    // ============================================================
    // HU-05: Crear usuario (ADMIN) - devuelve contraseña temporal
    // ============================================================
    @Transactional
    public CrearEmpleadoResponseDTO crearUsuarioAdmin(CrearEmpleadoDTO dto) {
        log.info("Administrador creando usuario con email: {}", dto.email());

        if (usuarioRepository.findByEmail(dto.email()).isPresent()) {
            throw new RecursoDuplicadoException("El correo ya está registrado");
        }

        String passwordTemporal = UUID.randomUUID().toString().substring(0, 12);

        Usuario usuario = new Usuario();
        usuario.setNombre(dto.nombre());
        usuario.setEmail(dto.email());
        usuario.setPassword(passwordEncoder.encode(passwordTemporal));
        Rol rolEntity = rolRepository.findByNombre(dto.rol())
                .orElseThrow(() -> new IllegalArgumentException("Rol inválido: " + dto.rol()));
        usuario.setRol(rolEntity);
        usuario.setEstado(EstadoUsuario.ACTIVO);
        usuario.setMetodoPagoOfuscado(null);
        usuario.setIdSucursalAsignada(dto.idSucursalAsignada());

        Usuario guardado = usuarioRepository.save(usuario);
        log.info("Usuario creado con ID: {} (contraseña temporal: {})", guardado.getId(), passwordTemporal);

        return new CrearEmpleadoResponseDTO(
                guardado.getId(),
                guardado.getNombre(),
                guardado.getEmail(),
                guardado.getRol().getNombre(),
                guardado.getEstado(),
                guardado.getDireccion(),
                guardado.getMetodoPagoOfuscado(),
                passwordTemporal
        );
    }

    // ============================================================
    // HU-07: Actualizar usuario (ADMIN)
    // ============================================================
    @Transactional
    public PerfilResponseDTO actualizarUsuarioAdmin(Long id, ActualizarEmpleadoDTO dto) {
        log.info("Administrador actualizando usuario ID: {}", id);

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado con ID: " + id));

        if ("ADMIN".equals(usuario.getRol().getNombre()) && !"ADMIN".equals(dto.rol())) {
            throw new IllegalArgumentException("No se puede cambiar el rol de un ADMIN a otro rol");
        }

        Rol nuevoRol = rolRepository.findByNombre(dto.rol())
                .orElseThrow(() -> new IllegalArgumentException("Rol inválido: " + dto.rol()));
        usuario.setNombre(dto.nombre());
        usuario.setEmail(dto.email());
        usuario.setRol(nuevoRol);
        usuario.setIdSucursalAsignada(dto.idSucursalAsignada());
        Usuario guardado = usuarioRepository.save(usuario);
        log.info("Usuario actualizado exitosamente ID: {}", id);
        return toPerfilResponse(guardado);
    }

    // ============================================================
    // HU-08: Desactivar usuario (ADMIN) - borrado lógico
    // ============================================================
    @Transactional
    public void desactivarUsuario(Long id) {
        log.info("Administrador desactivando usuario ID: {}", id);

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado con ID: " + id));

        if ("ADMIN".equals(usuario.getRol().getNombre())) {
            throw new IllegalArgumentException("No se puede desactivar a un usuario ADMIN");
        }

        if (usuario.getEstado() == EstadoUsuario.INACTIVO) {
            throw new IllegalArgumentException("El usuario ya se encuentra inactivo");
        }

        usuario.setEstado(EstadoUsuario.INACTIVO);
        usuarioRepository.save(usuario);
        log.info("Usuario desactivado exitosamente ID: {}", id);
    }

    // ============================================================
    // HU-02 complemento: Desbloquear usuario (ADMIN)
    // ============================================================
    @Transactional
    public void desbloquearUsuario(Long id) {
        log.info("Administrador desbloqueando usuario ID: {}", id);

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado con ID: " + id));

        if (usuario.getEstado() != EstadoUsuario.INACTIVO) {
            throw new IllegalArgumentException("El usuario no se encuentra bloqueado");
        }

        usuario.setEstado(EstadoUsuario.ACTIVO);
        usuario.setIntentosFallidos(0);
        usuarioRepository.save(usuario);
        log.info("Usuario desbloqueado exitosamente ID: {}", id);
    }

    // ─── Método auxiliar ───

    private PerfilResponseDTO toPerfilResponse(Usuario usuario) {
        return new PerfilResponseDTO(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getEmail(),
                usuario.getTelefono(),
                usuario.getRol().getNombre(),
                usuario.getEstado(),
                usuario.getDireccion(),
                usuario.getMetodoPagoOfuscado());
    }
}
