package com.perfulandia.usuarios.service;

import com.perfulandia.usuarios.exception.CredencialesInvalidasException;
import com.perfulandia.usuarios.exception.RecursoDuplicadoException;
import com.perfulandia.usuarios.exception.RecursoNoEncontradoException;
import com.perfulandia.usuarios.model.dto.*;
import com.perfulandia.usuarios.model.entity.*;
import com.perfulandia.usuarios.model.enums.EstadoUsuario;
import com.perfulandia.usuarios.model.enums.Rol;
import com.perfulandia.usuarios.repository.TokenInvalidadoRepository;
import com.perfulandia.usuarios.repository.TokenRecuperacionRepository;
import com.perfulandia.usuarios.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class UsuarioService {

    private static final Logger log = LoggerFactory.getLogger(UsuarioService.class);
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenInvalidadoRepository tokenInvalidadoRepository;
    private final TokenRecuperacionRepository tokenRecuperacionRepository;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
            JwtService jwtService, TokenInvalidadoRepository tokenInvalidadoRepository,
            TokenRecuperacionRepository tokenRecuperacionRepository) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.tokenInvalidadoRepository = tokenInvalidadoRepository;
        this.tokenRecuperacionRepository = tokenRecuperacionRepository;
    }

    @Transactional
    public PerfilResponseDTO registrarCliente(RegistroRequestDTO dto) {
        if (usuarioRepository.findByCorreo(dto.correo()).isPresent())
            throw new RecursoDuplicadoException("El correo ya está registrado.");

        Usuario usuario = new Usuario();
        usuario.setNombre(dto.nombre());
        usuario.setCorreo(dto.correo());
        usuario.setContrasena(passwordEncoder.encode(dto.contrasena()));
        usuario.setRol(Rol.CLIENTE);
        usuario.setEstado(EstadoUsuario.ACTIVO);

        Cliente cliente = new Cliente();
        cliente.setDireccion(dto.direccion());
        usuario.setPerfilCliente(cliente);

        Usuario guardado = usuarioRepository.save(usuario);
        return new PerfilResponseDTO(guardado.getIdUsuario(), guardado.getNombre(),
                guardado.getCorreo(), guardado.getRol(),
                guardado.getPerfilCliente().getDireccion(),
                guardado.getMetodoPagoOfuscado() != null ? guardado.getMetodoPagoOfuscado() : "**** **** **** 1234");
    }

    @Transactional(noRollbackFor = CredencialesInvalidasException.class)
    public String iniciarSesion(String correo, String contrasenaPlana) {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new CredencialesInvalidasException("Credenciales incorrectas."));

        if (usuario.getEstado() == EstadoUsuario.INACTIVO)
            throw new CredencialesInvalidasException("Cuenta bloqueada.");

        if (!passwordEncoder.matches(contrasenaPlana, usuario.getContrasena())) {
            manejarIntentoFallido(usuario);
            throw new CredencialesInvalidasException("Credenciales incorrectas.");
        }

        usuario.setIntentosFallidos(0);
        usuarioRepository.save(usuario);
        return jwtService.generarToken(usuario.getCorreo(), usuario.getRol().name());
    }

    private void manejarIntentoFallido(Usuario usuario) {
        usuario.setIntentosFallidos(usuario.getIntentosFallidos() + 1);
        if (usuario.getIntentosFallidos() >= 3) {
            usuario.setEstado(EstadoUsuario.INACTIVO);
            log.warn("Cuenta bloqueada por 3 intentos fallidos: {}", usuario.getCorreo());
        }
        usuarioRepository.save(usuario);
    }

    @Transactional
    public void actualizarPerfil(String correo, ActualizarPerfilDTO dto) {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado."));
        usuario.setNombre(dto.nombre());
        if (usuario.getPerfilCliente() != null)
            usuario.getPerfilCliente().setDireccion(dto.direccion());

        if (dto.nuevoMetodoPago() != null && !dto.nuevoMetodoPago().isBlank()) {
            log.info("simulando validación de tarjeta con pasarela externa");
            String ofuscado = "**** **** **** "
                    + dto.nuevoMetodoPago().substring(Math.max(0, dto.nuevoMetodoPago().length() - 4));
            usuario.setMetodoPagoOfuscado(ofuscado);
        }
        usuarioRepository.save(usuario);
    }

    @Transactional
    public String crearEmpleado(CrearEmpleadoDTO dto) {
        if (usuarioRepository.findByCorreo(dto.correo()).isPresent())
            throw new RecursoDuplicadoException("El correo ya está en uso.");

        String passwordTemporal = generarPasswordSegura();
        Usuario usuario = new Usuario();
        usuario.setNombre(dto.nombre());
        usuario.setCorreo(dto.correo());
        usuario.setContrasena(passwordEncoder.encode(passwordTemporal));
        usuario.setRol(dto.rol());
        usuario.setEstado(EstadoUsuario.ACTIVO);

        usuarioRepository.save(usuario);
        return passwordTemporal;
    }

    private String generarPasswordSegura() {
        String upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lower = "abcdefghijklmnopqrstuvwxyz";
        String digits = "0123456789";
        String all = upper + lower + digits;
        StringBuilder sb = new StringBuilder();
        sb.append(upper.charAt((int) (Math.random() * upper.length())));
        sb.append(lower.charAt((int) (Math.random() * lower.length())));
        sb.append(digits.charAt((int) (Math.random() * digits.length())));
        for (int i = 0; i < 5; i++)
            sb.append(all.charAt((int) (Math.random() * all.length())));
        return sb.toString();
    }

    public Page<PerfilResponseDTO> listarUsuarios(Pageable pageable) {
        return usuarioRepository.findAll(pageable).map(u -> new PerfilResponseDTO(
                u.getIdUsuario(), u.getNombre(), u.getCorreo(), u.getRol(),
                u.getPerfilCliente() != null ? u.getPerfilCliente().getDireccion() : null,
                u.getMetodoPagoOfuscado()));
    }

    @Transactional
    public void actualizarEmpleado(Long id, ActualizarEmpleadoDTO dto, String correoAdmin) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado."));

        // evita que el administrador se degrade o cambie su rol
        if (usuario.getCorreo().equals(correoAdmin) && !dto.rol().equals(usuario.getRol())) {
            throw new RuntimeException("Un administrador no puede cambiar su propio rol.");
        }
        usuario.setNombre(dto.nombre());
        usuario.setCorreo(dto.correo());
        usuario.setRol(dto.rol());
        usuarioRepository.save(usuario);
    }

    @Transactional
    public void desactivarCuenta(Long id, String correoAdmin) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado."));
        if (usuario.getCorreo().equals(correoAdmin))
            throw new RuntimeException("Un administrador no puede desactivar su propia cuenta.");
        usuario.setEstado(EstadoUsuario.INACTIVO);
        usuarioRepository.save(usuario);
    }

    @Transactional
    public void solicitarRecuperacion(String correo) {
        usuarioRepository.findByCorreo(correo).ifPresent(usuario -> {
            TokenRecuperacion tr = new TokenRecuperacion();
            tr.setCorreo(correo);
            tr.setToken(UUID.randomUUID().toString());
            tr.setExpiracion(LocalDateTime.now().plusMinutes(15));
            tokenRecuperacionRepository.save(tr);
            log.info("Token de recuperación generado para {}: {}", correo, tr.getToken());
            // simula log de envío de correo
        });
    }

    @Transactional
    public void restablecerPassword(RestablecerPasswordRequestDTO dto) {
        TokenRecuperacion tokenRec = tokenRecuperacionRepository.findByToken(dto.token())
                .orElseThrow(() -> new RecursoNoEncontradoException("Token inválido o expirado."));
        if (tokenRec.getExpiracion().isBefore(LocalDateTime.now()))
            throw new RuntimeException("El token ha expirado.");

        Usuario usuario = usuarioRepository.findByCorreo(tokenRec.getCorreo())
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado."));
        usuario.setContrasena(passwordEncoder.encode(dto.nuevaContrasena()));
        usuarioRepository.save(usuario);

        tokenRecuperacionRepository.delete(tokenRec); // eliminar token usado
        log.info("Contraseña restablecida para {}", tokenRec.getCorreo());
    }

    @Transactional
    public void cerrarSesion(String token) {
        tokenInvalidadoRepository.save(new TokenInvalidado(token, LocalDateTime.now()));
    }

    public PerfilResponseDTO obtenerPerfil(String correo) {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado."));
        String direccion = usuario.getPerfilCliente() != null ? usuario.getPerfilCliente().getDireccion() : null;
        return new PerfilResponseDTO(usuario.getIdUsuario(), usuario.getNombre(), usuario.getCorreo(),
                usuario.getRol(), direccion, usuario.getMetodoPagoOfuscado());
    }
}