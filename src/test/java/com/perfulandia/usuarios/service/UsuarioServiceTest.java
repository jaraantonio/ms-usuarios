package com.perfulandia.usuarios.service;

import com.perfulandia.usuarios.client.NotificacionesWebClient;
import com.perfulandia.usuarios.exception.CredencialesInvalidasException;
import com.perfulandia.usuarios.exception.RecursoDuplicadoException;
import com.perfulandia.usuarios.exception.RecursoNoEncontradoException;
import com.perfulandia.usuarios.model.dto.*;
import com.perfulandia.usuarios.model.entity.TokenRecuperacion;
import com.perfulandia.usuarios.model.entity.Usuario;
import com.perfulandia.usuarios.model.enums.EstadoUsuario;
import com.perfulandia.usuarios.model.enums.Rol;
import com.perfulandia.usuarios.repository.TokenInvalidadoRepository;
import com.perfulandia.usuarios.repository.TokenRecuperacionRepository;
import com.perfulandia.usuarios.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UsuarioService - Pruebas unitarias")
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private TokenInvalidadoRepository tokenInvalidadoRepository;

    @Mock
    private TokenRecuperacionRepository tokenRecuperacionRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private NotificacionesWebClient notificacionesWebClient;

    @InjectMocks
    private UsuarioService usuarioService;

    private Usuario usuarioActivo;
    private Usuario usuarioInactivo;

    @BeforeEach
    void setUp() {
        usuarioActivo = new Usuario();
        usuarioActivo.setId(1L);
        usuarioActivo.setNombre("Juan Pérez");
        usuarioActivo.setEmail("juan@test.com");
        usuarioActivo.setPassword("encodedPass");
        usuarioActivo.setRol(Rol.CLIENTE);
        usuarioActivo.setEstado(EstadoUsuario.ACTIVO);
        usuarioActivo.setIntentosFallidos(0);
        usuarioActivo.setDireccion("Calle 123");
        usuarioActivo.setMetodoPagoOfuscado("****");

        usuarioInactivo = new Usuario();
        usuarioInactivo.setId(2L);
        usuarioInactivo.setNombre("Ana Inactiva");
        usuarioInactivo.setEmail("ana@test.com");
        usuarioInactivo.setPassword("encodedPass");
        usuarioInactivo.setRol(Rol.CLIENTE);
        usuarioInactivo.setEstado(EstadoUsuario.INACTIVO);
        usuarioInactivo.setIntentosFallidos(3);
    }

    // ==================================================================
    // HU-01: Registro de Cliente
    // ==================================================================
    @Nested
    @DisplayName("registrarCliente")
    class RegistrarClienteTests {

        @Test
        @DisplayName("Debe registrar cliente exitosamente con datos válidos")
        void registrarCliente_Exito() {
            // Given
            RegistroRequestDTO dto = new RegistroRequestDTO("Juan", "juan@test.com", "Juan12345", "Calle 123");
            when(usuarioRepository.findByEmail(dto.email())).thenReturn(Optional.empty());
            when(passwordEncoder.encode(dto.password())).thenReturn("encodedPass");
            when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuarioActivo);

            // When
            PerfilResponseDTO result = usuarioService.registrarCliente(dto);

            // Then
            assertNotNull(result);
            assertEquals("Juan Pérez", result.nombre());
            assertEquals("juan@test.com", result.email());
            assertEquals(Rol.CLIENTE, result.rol());
            verify(usuarioRepository, times(1)).save(any(Usuario.class));
        }

        @Test
        @DisplayName("Debe lanzar RecursoDuplicadoException si el correo ya existe")
        void registrarCliente_EmailDuplicado() {
            // Given
            RegistroRequestDTO dto = new RegistroRequestDTO("Juan", "juan@test.com", "Juan12345", "Calle 123");
            when(usuarioRepository.findByEmail(dto.email())).thenReturn(Optional.of(usuarioActivo));

            // When / Then
            assertThrows(RecursoDuplicadoException.class, () -> usuarioService.registrarCliente(dto));
            verify(usuarioRepository, never()).save(any(Usuario.class));
        }
    }

    // ==================================================================
    // HU-02: Autenticación / Login
    // ==================================================================
    @Nested
    @DisplayName("autenticar")
    class AutenticarTests {

        @Test
        @DisplayName("Debe retornar token JWT en login exitoso")
        void autenticar_Exito() {
            // Given
            LoginRequestDTO dto = new LoginRequestDTO("juan@test.com", "Juan12345");
            when(usuarioRepository.findByEmail(dto.email())).thenReturn(Optional.of(usuarioActivo));
            when(passwordEncoder.matches(dto.password(), usuarioActivo.getPassword())).thenReturn(true);
            when(jwtService.generarToken(1L, "juan@test.com", "CLIENTE")).thenReturn("jwt-token");

            // When
            LoginResponseDTO result = usuarioService.autenticar(dto);

            // Then
            assertNotNull(result);
            assertEquals("jwt-token", result.token());
            assertEquals("CLIENTE", result.rol());
            assertEquals(0, usuarioActivo.getIntentosFallidos());
        }

        @Test
        @DisplayName("Debe lanzar CredencialesInvalidasException si contraseña incorrecta")
        void autenticar_PasswordIncorrecta() {
            // Given
            LoginRequestDTO dto = new LoginRequestDTO("juan@test.com", "WrongPass");
            when(usuarioRepository.findByEmail(dto.email())).thenReturn(Optional.of(usuarioActivo));
            when(passwordEncoder.matches(dto.password(), usuarioActivo.getPassword())).thenReturn(false);

            // When / Then
            assertThrows(CredencialesInvalidasException.class, () -> usuarioService.autenticar(dto));
            assertEquals(1, usuarioActivo.getIntentosFallidos());
        }

        @Test
        @DisplayName("Debe bloquear cuenta tras 3 intentos fallidos")
        void autenticar_BloqueoTras3Intentos() {
            // Given
            usuarioActivo.setIntentosFallidos(2);
            LoginRequestDTO dto = new LoginRequestDTO("juan@test.com", "WrongPass");
            when(usuarioRepository.findByEmail(dto.email())).thenReturn(Optional.of(usuarioActivo));
            when(passwordEncoder.matches(dto.password(), usuarioActivo.getPassword())).thenReturn(false);

            // When / Then
            assertThrows(CredencialesInvalidasException.class, () -> usuarioService.autenticar(dto));
            assertEquals(EstadoUsuario.INACTIVO, usuarioActivo.getEstado());
            assertEquals(3, usuarioActivo.getIntentosFallidos());
        }

        @Test
        @DisplayName("Debe rechazar login si cuenta está inactiva")
        void autenticar_CuentaInactiva() {
            // Given
            LoginRequestDTO dto = new LoginRequestDTO("ana@test.com", "password");
            when(usuarioRepository.findByEmail(dto.email())).thenReturn(Optional.of(usuarioInactivo));

            // When / Then
            assertThrows(CredencialesInvalidasException.class, () -> usuarioService.autenticar(dto));
        }

        @Test
        @DisplayName("Debe lanzar excepción si el correo no existe")
        void autenticar_CorreoNoExiste() {
            // Given
            LoginRequestDTO dto = new LoginRequestDTO("noexiste@test.com", "password");
            when(usuarioRepository.findByEmail(dto.email())).thenReturn(Optional.empty());

            // When / Then
            assertThrows(CredencialesInvalidasException.class, () -> usuarioService.autenticar(dto));
        }
    }

    // ==================================================================
    // HU-03: Obtener Perfil
    // ==================================================================
    @Nested
    @DisplayName("obtenerPerfil")
    class ObtenerPerfilTests {

        @Test
        @DisplayName("Debe retornar perfil si el usuario existe")
        void obtenerPerfil_Exito() {
            // Given
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioActivo));

            // When
            PerfilResponseDTO result = usuarioService.obtenerPerfil(1L);

            // Then
            assertNotNull(result);
            assertEquals(1L, result.id());
            assertEquals("juan@test.com", result.email());
        }

        @Test
        @DisplayName("Debe lanzar RecursoNoEncontradoException si no existe")
        void obtenerPerfil_NoEncontrado() {
            // Given
            when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

            // When / Then
            assertThrows(RecursoNoEncontradoException.class, () -> usuarioService.obtenerPerfil(99L));
        }
    }

    // ==================================================================
    // HU-04: Actualizar Perfil
    // ==================================================================
    @Nested
    @DisplayName("actualizarPerfil")
    class ActualizarPerfilTests {

        @Test
        @DisplayName("Debe actualizar nombre y dirección exitosamente")
        void actualizarPerfil_Exito() {
            // Given
            ActualizarPerfilDTO dto = new ActualizarPerfilDTO("Nuevo Nombre", "Nueva Dirección", null);
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioActivo));
            when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuarioActivo);

            // When
            PerfilResponseDTO result = usuarioService.actualizarPerfil(1L, dto);

            // Then
            assertNotNull(result);
            verify(usuarioRepository, times(1)).save(any(Usuario.class));
        }

        @Test
        @DisplayName("Debe ofuscar método de pago al actualizar")
        void actualizarPerfil_OfuscaMetodoPago() {
            // Given
            ActualizarPerfilDTO dto = new ActualizarPerfilDTO("Nombre", "Dirección", "1234567890123456");
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioActivo));
            when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuarioActivo);

            // When
            usuarioService.actualizarPerfil(1L, dto);

            // Then
            ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
            verify(usuarioRepository).save(captor.capture());
            assertTrue(captor.getValue().getMetodoPagoOfuscado().contains("****"));
            assertTrue(captor.getValue().getMetodoPagoOfuscado().contains("3456"));
        }
    }

    // ==================================================================
    // HU-44a: Recuperar Password
    // ==================================================================
    @Nested
    @DisplayName("recuperarPassword")
    class RecuperarPasswordTests {

        @Test
        @DisplayName("Debe retornar mensaje aunque el correo no exista (no revela info)")
        void recuperarPassword_CorreoNoExiste() {
            // Given
            when(usuarioRepository.findByEmail("noexiste@test.com")).thenReturn(Optional.empty());
            when(tokenRecuperacionRepository.findAll()).thenReturn(List.of());

            // When
            String result = usuarioService.recuperarPassword("noexiste@test.com");

            // Then
            assertNotNull(result);
            assertTrue(result.contains("simulado"));
            verify(notificacionesWebClient, never()).enviarCorreo(any());
        }

        @Test
        @DisplayName("Debe generar token si el correo existe")
        void recuperarPassword_Exito() {
            // Given
            when(usuarioRepository.findByEmail("juan@test.com")).thenReturn(Optional.of(usuarioActivo));
            TokenRecuperacion tr = new TokenRecuperacion();
            tr.setToken("token123");
            tr.setCorreo("juan@test.com");
            tr.setExpiracion(LocalDateTime.now().plusMinutes(15));
            tr.setUsado(false);
            when(tokenRecuperacionRepository.findAll()).thenReturn(List.of(tr));

            // When
            String result = usuarioService.recuperarPassword("juan@test.com");

            // Then
            assertNotNull(result);
            assertTrue(result.contains("token123"));
            verify(tokenRecuperacionRepository, times(1)).save(any(TokenRecuperacion.class));
            verify(notificacionesWebClient, times(1)).enviarCorreo(any(CorreoRequestDTO.class));
        }
    }

    // ==================================================================
    // HU-44b: Restablecer Password
    // ==================================================================
    @Nested
    @DisplayName("restablecerPassword")
    class RestablecerPasswordTests {

        @Test
        @DisplayName("Debe restablecer contraseña con token válido")
        void restablecerPassword_Exito() {
            // Given
            TokenRecuperacion tr = new TokenRecuperacion();
            tr.setToken("token123");
            tr.setCorreo("juan@test.com");
            tr.setExpiracion(LocalDateTime.now().plusMinutes(5));
            tr.setUsado(false);

            RestablecerPasswordRequestDTO dto = new RestablecerPasswordRequestDTO("token123", "NuevaClave123");

            when(tokenRecuperacionRepository.findByToken("token123")).thenReturn(Optional.of(tr));
            when(usuarioRepository.findByEmail("juan@test.com")).thenReturn(Optional.of(usuarioActivo));
            when(passwordEncoder.encode("NuevaClave123")).thenReturn("encodedNewPass");

            // When
            assertDoesNotThrow(() -> usuarioService.restablecerPassword(dto));

            // Then
            verify(usuarioRepository, times(1)).save(any(Usuario.class));
            assertTrue(tr.isUsado());
        }

        @Test
        @DisplayName("Debe lanzar excepción si el token expiró")
        void restablecerPassword_TokenExpirado() {
            // Given
            TokenRecuperacion tr = new TokenRecuperacion();
            tr.setToken("token123");
            tr.setCorreo("juan@test.com");
            tr.setExpiracion(LocalDateTime.now().minusMinutes(1));
            tr.setUsado(false);

            RestablecerPasswordRequestDTO dto = new RestablecerPasswordRequestDTO("token123", "NuevaClave123");
            when(tokenRecuperacionRepository.findByToken("token123")).thenReturn(Optional.of(tr));

            // When / Then
            assertThrows(IllegalArgumentException.class, () -> usuarioService.restablecerPassword(dto));
        }
    }

    // ==================================================================
    // HU-45: Cerrar Sesión
    // ==================================================================
    @Nested
    @DisplayName("cerrarSesion")
    class CerrarSesionTests {

        @Test
        @DisplayName("Debe invalidar el token exitosamente")
        void cerrarSesion_Exito() {
            // Given / When
            assertDoesNotThrow(() -> usuarioService.cerrarSesion("token-a-invalidar"));

            // Then
            verify(tokenInvalidadoRepository, times(1)).save(any());
        }
    }

    // ==================================================================
    // HU-05: Crear Usuario Admin
    // ==================================================================
    @Nested
    @DisplayName("crearUsuarioAdmin")
    class CrearUsuarioAdminTests {

        @Test
        @DisplayName("Debe crear usuario con rol asignado por admin")
        void crearUsuarioAdmin_Exito() {
            // Given
            CrearEmpleadoDTO dto = new CrearEmpleadoDTO("Empleado", "empleado@test.com", Rol.EMPLEADO);
            when(usuarioRepository.findByEmail(dto.email())).thenReturn(Optional.empty());
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPwd");
            when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
                Usuario u = inv.getArgument(0);
                u.setId(10L);
                return u;
            });

            // When
            PerfilResponseDTO result = usuarioService.crearUsuarioAdmin(dto);

            // Then
            assertNotNull(result);
            assertEquals("empleado@test.com", result.email());
            assertEquals(Rol.EMPLEADO, result.rol());
            verify(usuarioRepository, times(1)).save(any(Usuario.class));
        }

        @Test
        @DisplayName("Debe lanzar RecursoDuplicadoException si email ya existe")
        void crearUsuarioAdmin_EmailDuplicado() {
            // Given
            CrearEmpleadoDTO dto = new CrearEmpleadoDTO("Duplicado", "juan@test.com", Rol.EMPLEADO);
            when(usuarioRepository.findByEmail(dto.email())).thenReturn(Optional.of(usuarioActivo));

            // When / Then
            assertThrows(RecursoDuplicadoException.class, () -> usuarioService.crearUsuarioAdmin(dto));
        }
    }

    // ==================================================================
    // HU-06: Listar Usuarios
    // ==================================================================
    @Nested
    @DisplayName("listarUsuarios")
    class ListarUsuariosTests {

        @Test
        @DisplayName("Debe listar todos los usuarios sin filtros")
        void listarUsuarios_SinFiltros() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Usuario> page = new PageImpl<>(List.of(usuarioActivo, usuarioInactivo));
            when(usuarioRepository.findAll(pageable)).thenReturn(page);

            // When
            Page<PerfilResponseDTO> result = usuarioService.listarUsuarios(pageable, null, null);

            // Then
            assertNotNull(result);
            assertEquals(2, result.getTotalElements());
        }

        @Test
        @DisplayName("Debe filtrar por rol correctamente")
        void listarUsuarios_PorRol() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Usuario> page = new PageImpl<>(List.of(usuarioActivo));
            when(usuarioRepository.findByRol(Rol.CLIENTE, pageable)).thenReturn(page);

            // When
            Page<PerfilResponseDTO> result = usuarioService.listarUsuarios(pageable, "CLIENTE", null);

            // Then
            assertEquals(1, result.getTotalElements());
        }

        @Test
        @DisplayName("Debe filtrar por rol y estado combinados")
        void listarUsuarios_PorRolYEstado() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Usuario> page = new PageImpl<>(List.of(usuarioActivo));
            when(usuarioRepository.findByRolAndEstado(Rol.CLIENTE, EstadoUsuario.ACTIVO, pageable))
                    .thenReturn(page);

            // When
            Page<PerfilResponseDTO> result = usuarioService.listarUsuarios(pageable, "CLIENTE", "ACTIVO");

            // Then
            assertEquals(1, result.getTotalElements());
        }
    }

    // ==================================================================
    // HU-07: Actualizar Usuario Admin
    // ==================================================================
    @Nested
    @DisplayName("actualizarUsuarioAdmin")
    class ActualizarUsuarioAdminTests {

        @Test
        @DisplayName("Debe actualizar datos de usuario correctamente")
        void actualizarUsuarioAdmin_Exito() {
            // Given
            ActualizarEmpleadoDTO dto = new ActualizarEmpleadoDTO("Nuevo", "nuevo@test.com", Rol.GERENTE);
            Usuario target = new Usuario();
            target.setId(5L);
            target.setRol(Rol.EMPLEADO);
            when(usuarioRepository.findById(5L)).thenReturn(Optional.of(target));
            when(usuarioRepository.save(any(Usuario.class))).thenReturn(target);

            // When
            PerfilResponseDTO result = usuarioService.actualizarUsuarioAdmin(5L, dto);

            // Then
            assertNotNull(result);
            verify(usuarioRepository, times(1)).save(any(Usuario.class));
        }

        @Test
        @DisplayName("Debe prevenir que admin cambie su propio rol")
        void actualizarUsuarioAdmin_NoCambiarRolAdmin() {
            // Given
            Usuario admin = new Usuario();
            admin.setId(1L);
            admin.setRol(Rol.ADMIN);
            ActualizarEmpleadoDTO dto = new ActualizarEmpleadoDTO("Admin", "admin@test.com", Rol.EMPLEADO);
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(admin));

            // When / Then
            assertThrows(IllegalArgumentException.class,
                    () -> usuarioService.actualizarUsuarioAdmin(1L, dto));
        }
    }

    // ==================================================================
    // HU-08: Desactivar Usuario
    // ==================================================================
    @Nested
    @DisplayName("desactivarUsuario")
    class DesactivarUsuarioTests {

        @Test
        @DisplayName("Debe desactivar usuario correctamente")
        void desactivarUsuario_Exito() {
            // Given
            Usuario target = new Usuario();
            target.setId(5L);
            target.setEstado(EstadoUsuario.ACTIVO);
            target.setRol(Rol.EMPLEADO);
            when(usuarioRepository.findById(5L)).thenReturn(Optional.of(target));

            // When
            assertDoesNotThrow(() -> usuarioService.desactivarUsuario(5L));

            // Then
            assertEquals(EstadoUsuario.INACTIVO, target.getEstado());
            verify(usuarioRepository, times(1)).save(target);
        }

        @Test
        @DisplayName("Debe prevenir desactivar a un ADMIN")
        void desactivarUsuario_NoDesactivarAdmin() {
            // Given
            Usuario admin = new Usuario();
            admin.setId(1L);
            admin.setRol(Rol.ADMIN);
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(admin));

            // When / Then
            assertThrows(IllegalArgumentException.class, () -> usuarioService.desactivarUsuario(1L));
        }
    }
}