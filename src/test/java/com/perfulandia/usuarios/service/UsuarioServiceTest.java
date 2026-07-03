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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
    private TokenRecuperacionRepository tokenRecuperacionRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

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
            RegistroRequestDTO dto = new RegistroRequestDTO("Juan", "juan@test.com", "Juan12345", "Calle 123", "+56912345678");
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
            assertEquals(EstadoUsuario.ACTIVO, result.estado());
            assertEquals("****", result.metodoPagoOfuscado());
            verify(usuarioRepository, times(1)).save(any(Usuario.class));
        }

        @Test
        @DisplayName("Debe lanzar RecursoDuplicadoException si el correo ya existe")
        void registrarCliente_EmailDuplicado() {
            // Given
            RegistroRequestDTO dto = new RegistroRequestDTO("Juan", "juan@test.com", "Juan12345", "Calle 123", "+56912345678");
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
        @DisplayName("Debe retornar rol en login exitoso")
        void autenticar_Exito() {
            // Given
            LoginRequestDTO dto = new LoginRequestDTO("juan@test.com", "Juan12345");
            when(usuarioRepository.findByEmail(dto.email())).thenReturn(Optional.of(usuarioActivo));
            when(passwordEncoder.matches(dto.password(), usuarioActivo.getPassword())).thenReturn(true);

            // When
            LoginResponseDTO result = usuarioService.autenticar(dto);

            // Then
            assertNotNull(result);
            assertEquals("CLIENTE", result.rol());
            assertEquals(0, usuarioActivo.getIntentosFallidos());
            verify(usuarioRepository, times(1)).save(usuarioActivo);
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
            verify(usuarioRepository, times(1)).save(usuarioActivo);
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
            verify(usuarioRepository, times(1)).save(usuarioActivo);
        }

        @Test
        @DisplayName("Debe rechazar login si cuenta está inactiva")
        void autenticar_CuentaInactiva() {
            // Given
            LoginRequestDTO dto = new LoginRequestDTO("ana@test.com", "password");
            when(usuarioRepository.findByEmail(dto.email())).thenReturn(Optional.of(usuarioInactivo));

            // When / Then
            assertThrows(CredencialesInvalidasException.class, () -> usuarioService.autenticar(dto));
            verify(usuarioRepository, never()).save(any(Usuario.class));
        }

        @Test
        @DisplayName("Debe lanzar excepción si el correo no existe")
        void autenticar_CorreoNoExiste() {
            // Given
            LoginRequestDTO dto = new LoginRequestDTO("noexiste@test.com", "password");
            when(usuarioRepository.findByEmail(dto.email())).thenReturn(Optional.empty());

            // When / Then
            assertThrows(CredencialesInvalidasException.class, () -> usuarioService.autenticar(dto));
            verify(usuarioRepository, never()).save(any(Usuario.class));
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
            assertEquals(Rol.CLIENTE, result.rol());
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

        @Test
        @DisplayName("Debe lanzar RecursoNoEncontradoException si el usuario no existe")
        void actualizarPerfil_NoEncontrado() {
            // Given
            ActualizarPerfilDTO dto = new ActualizarPerfilDTO("Nombre", "Dirección", null);
            when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

            // When / Then
            assertThrows(RecursoNoEncontradoException.class, () -> usuarioService.actualizarPerfil(99L, dto));
            verify(usuarioRepository, never()).save(any(Usuario.class));
        }
    }

    // ==================================================================
    // HU-44a: Recuperar Password
    // ==================================================================
    @Nested
    @DisplayName("recuperarPassword")
    class RecuperarPasswordTests {

        @Test
        @DisplayName("Debe retornar mensaje genérico aunque el correo no exista (no revela info)")
        void recuperarPassword_CorreoNoExiste() {
            // Given
            when(usuarioRepository.findByEmail("noexiste@test.com")).thenReturn(Optional.empty());

            // When
            String result = usuarioService.recuperarPassword("noexiste@test.com");

            // Then — siempre retorna mensaje genérico (no revela si el correo existe)
            assertNotNull(result);
            assertTrue(result.contains("correo"));
            verify(notificacionesWebClient, never()).enviarNotificacion(any(), anyString());
            verify(tokenRecuperacionRepository, never()).save(any(TokenRecuperacion.class));
        }

        @Test
        @DisplayName("Debe generar token y enviar correo si el email existe")
        void recuperarPassword_Exito() {
            // Given
            when(usuarioRepository.findByEmail("juan@test.com")).thenReturn(Optional.of(usuarioActivo));

            // When
            String result = usuarioService.recuperarPassword("juan@test.com");

            // Then — retorna mensaje genérico, el token se envía por correo
            assertNotNull(result);
            assertTrue(result.contains("correo"));
            verify(tokenRecuperacionRepository, times(1)).save(any(TokenRecuperacion.class));
            verify(notificacionesWebClient, times(1)).enviarNotificacion(any(CorreoRequestDTO.class), anyString());
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
            tr.setToken(UUID.randomUUID().toString());
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
            verify(tokenRecuperacionRepository, times(1)).save(tr);
            assertTrue(tr.isUsado());
            assertEquals(0, usuarioActivo.getIntentosFallidos());
            assertEquals(EstadoUsuario.ACTIVO, usuarioActivo.getEstado());
        }

        @Test
        @DisplayName("Debe lanzar excepción si el token expiró")
        void restablecerPassword_TokenExpirado() {
            // Given
            TokenRecuperacion tr = new TokenRecuperacion();
            tr.setToken(UUID.randomUUID().toString());
            tr.setCorreo("juan@test.com");
            tr.setExpiracion(LocalDateTime.now().minusMinutes(1));
            tr.setUsado(false);

            RestablecerPasswordRequestDTO dto = new RestablecerPasswordRequestDTO("token123", "NuevaClave123");
            when(tokenRecuperacionRepository.findByToken("token123")).thenReturn(Optional.of(tr));

            // When / Then
            assertThrows(IllegalArgumentException.class, () -> usuarioService.restablecerPassword(dto));
            verify(usuarioRepository, never()).save(any(Usuario.class));
            assertFalse(tr.isUsado());
        }

        @Test
        @DisplayName("Debe lanzar excepción si el token ya fue usado")
        void restablecerPassword_TokenYaUsado() {
            // Given
            TokenRecuperacion tr = new TokenRecuperacion();
            tr.setToken(UUID.randomUUID().toString());
            tr.setCorreo("juan@test.com");
            tr.setExpiracion(LocalDateTime.now().plusMinutes(5));
            tr.setUsado(true);

            RestablecerPasswordRequestDTO dto = new RestablecerPasswordRequestDTO("token123", "NuevaClave123");
            when(tokenRecuperacionRepository.findByToken("token123")).thenReturn(Optional.of(tr));

            // When / Then
            assertThrows(IllegalArgumentException.class, () -> usuarioService.restablecerPassword(dto));
            verify(usuarioRepository, never()).save(any(Usuario.class));
            verify(tokenRecuperacionRepository, never()).save(any(TokenRecuperacion.class));
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
            CrearEmpleadoDTO dto = new CrearEmpleadoDTO("Empleado", "empleado@test.com", Rol.EMPLEADO, null);
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
            assertEquals(EstadoUsuario.ACTIVO, result.estado());
            assertEquals("****", result.metodoPagoOfuscado());
            verify(usuarioRepository, times(1)).save(any(Usuario.class));
        }

        @Test
        @DisplayName("Debe lanzar RecursoDuplicadoException si email ya existe")
        void crearUsuarioAdmin_EmailDuplicado() {
            // Given
            CrearEmpleadoDTO dto = new CrearEmpleadoDTO("Duplicado", "juan@test.com", Rol.EMPLEADO, null);
            when(usuarioRepository.findByEmail(dto.email())).thenReturn(Optional.of(usuarioActivo));

            // When / Then
            assertThrows(RecursoDuplicadoException.class, () -> usuarioService.crearUsuarioAdmin(dto));
            verify(usuarioRepository, never()).save(any(Usuario.class));
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
            when(usuarioRepository.findAll()).thenReturn(List.of(usuarioActivo, usuarioInactivo));

            // When
            List<PerfilResponseDTO> result = usuarioService.listarUsuarios(null, null);

            // Then
            assertNotNull(result);
            assertEquals(2, result.size());
            verify(usuarioRepository, times(1)).findAll();
        }

        @Test
        @DisplayName("Debe filtrar por rol correctamente")
        void listarUsuarios_PorRol() {
            // Given
            when(usuarioRepository.findByRol(Rol.CLIENTE)).thenReturn(List.of(usuarioActivo));

            // When
            List<PerfilResponseDTO> result = usuarioService.listarUsuarios("CLIENTE", null);

            // Then
            assertEquals(1, result.size());
            verify(usuarioRepository, times(1)).findByRol(Rol.CLIENTE);
        }

        @Test
        @DisplayName("Debe filtrar por estado correctamente")
        void listarUsuarios_PorEstado() {
            // Given
            when(usuarioRepository.findByEstado(EstadoUsuario.ACTIVO)).thenReturn(List.of(usuarioActivo));

            // When
            List<PerfilResponseDTO> result = usuarioService.listarUsuarios(null, "ACTIVO");

            // Then
            assertEquals(1, result.size());
            verify(usuarioRepository, times(1)).findByEstado(EstadoUsuario.ACTIVO);
        }

        @Test
        @DisplayName("Debe filtrar por rol y estado combinados")
        void listarUsuarios_PorRolYEstado() {
            // Given
            when(usuarioRepository.findByRolAndEstado(Rol.CLIENTE, EstadoUsuario.ACTIVO))
                    .thenReturn(List.of(usuarioActivo));

            // When
            List<PerfilResponseDTO> result = usuarioService.listarUsuarios("CLIENTE", "ACTIVO");

            // Then
            assertEquals(1, result.size());
            verify(usuarioRepository, times(1)).findByRolAndEstado(Rol.CLIENTE, EstadoUsuario.ACTIVO);
        }

        @Test
        @DisplayName("Debe lanzar excepción si el valor del rol es inválido")
        void listarUsuarios_RolInvalido() {
            // When / Then
            assertThrows(IllegalArgumentException.class,
                    () -> usuarioService.listarUsuarios("ROL_INEXISTENTE", null));
        }

        @Test
        @DisplayName("Debe lanzar excepción si el valor del estado es inválido")
        void listarUsuarios_EstadoInvalido() {
            // When / Then
            assertThrows(IllegalArgumentException.class,
                    () -> usuarioService.listarUsuarios(null, "ESTADO_INEXISTENTE"));
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
            ActualizarEmpleadoDTO dto = new ActualizarEmpleadoDTO("Nuevo", "nuevo@test.com", Rol.GERENTE, null);
            Usuario target = new Usuario();
            target.setId(5L);
            target.setRol(Rol.EMPLEADO);
            when(usuarioRepository.findById(5L)).thenReturn(Optional.of(target));
            when(usuarioRepository.save(any(Usuario.class))).thenReturn(target);

            // When
            PerfilResponseDTO result = usuarioService.actualizarUsuarioAdmin(5L, dto);

            // Then
            assertNotNull(result);
            assertEquals("nuevo@test.com", result.email());
            assertEquals(Rol.GERENTE, result.rol());
            verify(usuarioRepository, times(1)).save(any(Usuario.class));
        }

        @Test
        @DisplayName("Debe prevenir que admin cambie su propio rol")
        void actualizarUsuarioAdmin_NoCambiarRolAdmin() {
            // Given
            Usuario admin = new Usuario();
            admin.setId(1L);
            admin.setRol(Rol.ADMIN);
            ActualizarEmpleadoDTO dto = new ActualizarEmpleadoDTO("Admin", "admin@test.com", Rol.EMPLEADO, null);
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(admin));

            // When / Then
            assertThrows(IllegalArgumentException.class,
                    () -> usuarioService.actualizarUsuarioAdmin(1L, dto));
            verify(usuarioRepository, never()).save(any(Usuario.class));
        }

        @Test
        @DisplayName("Debe lanzar RecursoNoEncontradoException si el usuario no existe")
        void actualizarUsuarioAdmin_NoEncontrado() {
            // Given
            ActualizarEmpleadoDTO dto = new ActualizarEmpleadoDTO("Nuevo", "nuevo@test.com", Rol.EMPLEADO, null);
            when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

            // When / Then
            assertThrows(RecursoNoEncontradoException.class,
                    () -> usuarioService.actualizarUsuarioAdmin(99L, dto));
            verify(usuarioRepository, never()).save(any(Usuario.class));
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
            verify(usuarioRepository, never()).save(any(Usuario.class));
        }

        @Test
        @DisplayName("Debe lanzar excepción si el usuario ya está inactivo")
        void desactivarUsuario_YaInactivo() {
            // Given
            when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuarioInactivo));

            // When / Then
            assertThrows(IllegalArgumentException.class, () -> usuarioService.desactivarUsuario(2L));
            verify(usuarioRepository, never()).save(any(Usuario.class));
        }

        @Test
        @DisplayName("Debe lanzar RecursoNoEncontradoException si el usuario no existe")
        void desactivarUsuario_NoEncontrado() {
            // Given
            when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

            // When / Then
            assertThrows(RecursoNoEncontradoException.class, () -> usuarioService.desactivarUsuario(99L));
            verify(usuarioRepository, never()).save(any(Usuario.class));
        }
    }
}
