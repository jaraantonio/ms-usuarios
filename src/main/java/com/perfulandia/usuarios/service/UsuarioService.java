package com.perfulandia.usuarios.service;

import com.perfulandia.usuarios.exception.CredencialesInvalidasException;
import com.perfulandia.usuarios.exception.RecursoDuplicadoException;
import com.perfulandia.usuarios.model.dto.PerfilResponseDTO;
import com.perfulandia.usuarios.model.dto.RegistroRequestDTO;
import com.perfulandia.usuarios.model.entity.Cliente;
import com.perfulandia.usuarios.model.entity.Usuario;
import com.perfulandia.usuarios.model.enums.Rol;
import com.perfulandia.usuarios.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UsuarioService {
    
    private static final Logger log = LoggerFactory.getLogger(UsuarioService.class);
    
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
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
        log.info("Nuevo cliente registrado con ID: {}", guardado.getIdUsuario());

        return new PerfilResponseDTO(
            guardado.getIdUsuario(), 
            guardado.getNombre(), 
            guardado.getCorreo(), 
            guardado.getRol(),
            guardado.getPerfilCliente().getDireccion(),
            "**** **** **** 1234" // falta de datos de medio de pago
        );
    }

    @Transactional
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
        
        return UUID.randomUUID().toString();
    }

    private void manejarIntentoFallido(Usuario usuario) {
        usuario.setIntentosFallidos(usuario.getIntentosFallidos() + 1);
        if (usuario.getIntentosFallidos() >= 3) {
            usuario.setEstado("INACTIVO");
            log.warn("Bloqueo de seguridad activado para el usuario: {}", usuario.getCorreo());
        }
        usuarioRepository.save(usuario);
    }
}