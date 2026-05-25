package com.perfulandia.usuarios.service;

import com.perfulandia.usuarios.exception.CredencialesInvalidasException;
import com.perfulandia.usuarios.exception.RecursoDuplicadoException;
import com.perfulandia.usuarios.model.dto.*;
import com.perfulandia.usuarios.model.entity.Cliente;
import com.perfulandia.usuarios.model.entity.TokenInvalidado;
import com.perfulandia.usuarios.model.entity.TokenRecuperacion;
import com.perfulandia.usuarios.model.entity.Usuario;
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
        if (usuarioRepository.findByCorreo(dto.correo()).isPresent()) {
            throw new RecursoDuplicadoException("El correo ingresado ya se encuentra en uso.");
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(dto.nombre());
        usuario.setCorreo(dto.correo());
        usuario.setContrasena(passwordEncoder.encode(dto.contrasena()));
        usuario.setRol(Rol.CLIENTE);
        usuario.setEstado("ACTIVO");

        Cliente cliente = new Cliente();
        cliente.setDireccion(dto.direccion());
        usuario.setPerfilCliente(cliente);

        Usuario guardado = usuarioRepository.save(usuario);
        return new PerfilResponseDTO(guardado.getIdUsuario(), guardado.getNombre(), guardado.getCorreo(),
                guardado.getRol(), guardado.getPerfilCliente().getDireccion(), "**** **** **** 1234");
    }

    @Transactional(noRollbackFor = CredencialesInvalidasException.class)
    public String iniciarSesion(String correo, String contrasenaPlana) {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new CredencialesInvalidasException("Credenciales incorrectas."));

        if ("INACTIVO".equals(usuario.getEstado())) {
            throw new CredencialesInvalidasException("La cuenta se encuentra bloqueada.");
        }

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
            usuario.setEstado("INACTIVO");
            log.warn("Bloqueo de seguridad activado para el usuario: {}", usuario.getCorreo());
        }
        usuarioRepository.save(usuario);
    }

    @Transactional
    public void actualizarPerfil(String correo, ActualizarPerfilDTO dto) {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        usuario.setNombre(dto.nombre());
        if (usuario.getPerfilCliente() != null) {
            usuario.getPerfilCliente().setDireccion(dto.direccion());
        }
        log.info("Simulando conexion segura con pasarela externa para validar tarjeta...");
    }

    @Transactional
    public String crearEmpleado(CrearEmpleadoDTO dto) {
        if (usuarioRepository.findByCorreo(dto.correo()).isPresent()) {
            throw new RecursoDuplicadoException("El correo ya esta en uso.");
        }
        String passwordTemporal = UUID.randomUUID().toString().substring(0, 8) + "A1!";

        Usuario usuario = new Usuario();
        usuario.setNombre(dto.nombre());
        usuario.setCorreo(dto.correo());
        usuario.setContrasena(passwordEncoder.encode(passwordTemporal));
        usuario.setRol(dto.rol());
        usuario.setEstado("ACTIVO");

        usuarioRepository.save(usuario);
        return passwordTemporal;
    }

    public Page<Usuario> listarUsuarios(Pageable pageable) {
        return usuarioRepository.findAll(pageable);
    }

    @Transactional
    public void actualizarEmpleado(Integer id, ActualizarEmpleadoDTO dto, String correoAdmin) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (usuario.getCorreo().equals(correoAdmin) && dto.rol() != Rol.ADMIN) {
            throw new RuntimeException("Regla de negocio: Un admin no puede quitarse su propio rol.");
        }

        usuario.setNombre(dto.nombre());
        usuario.setCorreo(dto.correo());
        usuario.setRol(dto.rol());
    }

    @Transactional
    public void desactivarCuenta(Integer id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        usuario.setEstado("INACTIVO");
    }

    @Transactional
    public void procesarRecuperacion(String correo) {
        usuarioRepository.findByCorreo(correo).ifPresent(usuario -> {
            TokenRecuperacion tr = new TokenRecuperacion();
            tr.setCorreo(correo);
            tr.setToken(UUID.randomUUID().toString());
            tr.setExpiracion(LocalDateTime.now().plusMinutes(15));
            tokenRecuperacionRepository.save(tr);
            log.info("MS Notificaciones simulado. Correo enviado a {} con token {}", correo, tr.getToken());
        });
    }

    @Transactional
    public void cerrarSesion(String token) {
        TokenInvalidado invalidado = new TokenInvalidado(token, LocalDateTime.now());
        tokenInvalidadoRepository.save(invalidado);
    }
}