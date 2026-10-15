package com.consultorio.service;

import com.consultorio.domain.AuthProvider;
import com.consultorio.domain.Paciente;
import com.consultorio.domain.Rol;
import com.consultorio.domain.Usuario;
import com.consultorio.repository.PacienteRepository;
import com.consultorio.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuth2ProvisioningServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PacienteRepository pacienteRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private OAuth2ProvisioningService oAuth2ProvisioningService;

    @Test
    @DisplayName("Usuario nuevo: Debe crear Usuario PACIENTE con AuthProvider GOOGLE y entidad Paciente asociada (JIT)")
    void testProcesarUsuarioOAuth2_NuevoUsuario_CreaUsuarioYPaciente() {
        String email = "juan.perez@gmail.com";
        String googleId = "google_123456";
        String name = "Juan Perez";
        String givenName = "Juan";
        String familyName = "Perez";

        when(usuarioRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encodedSecurePassword");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario u = invocation.getArgument(0);
            u.setId(100L);
            return u;
        });

        Usuario resultado = oAuth2ProvisioningService.procesarUsuarioOAuth2(email, googleId, name, givenName, familyName);

        assertNotNull(resultado);
        assertEquals(email, resultado.getEmail());
        assertEquals(Rol.PACIENTE, resultado.getRol());
        assertEquals(AuthProvider.GOOGLE, resultado.getAuthProvider());
        assertEquals(googleId, resultado.getGoogleId());
        assertTrue(resultado.isActivo());
        assertTrue(resultado.isEmailVerificado());

        ArgumentCaptor<Paciente> pacienteCaptor = ArgumentCaptor.forClass(Paciente.class);
        verify(pacienteRepository).save(pacienteCaptor.capture());
        Paciente pacienteGuardado = pacienteCaptor.getValue();

        assertNotNull(pacienteGuardado);
        assertEquals("Juan", pacienteGuardado.getNombre());
        assertEquals("Perez", pacienteGuardado.getApellido());
        assertEquals(email, pacienteGuardado.getEmail());
        assertNull(pacienteGuardado.getDni());
        assertEquals(100L, pacienteGuardado.getUsuario().getId());
    }

    @Test
    @DisplayName("Usuario existente local: Debe vincular googleId, activar cuenta y preservar su rol original")
    void testProcesarUsuarioOAuth2_UsuarioExistente_VinculaYActiva() {
        String email = "maria.doctora@consultorio.com";
        String googleId = "google_789101";

        Usuario usuarioExistente = Usuario.builder()
                .id(50L)
                .email(email)
                .password("hashLocal")
                .rol(Rol.DOCTOR)
                .activo(false)
                .emailVerificado(false)
                .authProvider(AuthProvider.LOCAL)
                .bloqueado(false)
                .build();

        when(usuarioRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(usuarioExistente));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Usuario resultado = oAuth2ProvisioningService.procesarUsuarioOAuth2(email, googleId, "Maria Doctora", "Maria", "Doctora");

        assertEquals(50L, resultado.getId());
        assertEquals(googleId, resultado.getGoogleId());
        assertEquals(Rol.DOCTOR, resultado.getRol());
        assertTrue(resultado.isActivo());
        assertTrue(resultado.isEmailVerificado());
        verify(pacienteRepository, never()).save(any(Paciente.class));
    }

    @Test
    @DisplayName("Usuario existente bloqueado: Debe lanzar excepción USER_BLOCKED")
    void testProcesarUsuarioOAuth2_UsuarioBloqueado_LanzaExcepcion() {
        String email = "bloqueado@consultorio.com";

        Usuario usuarioBloqueado = Usuario.builder()
                .id(99L)
                .email(email)
                .rol(Rol.PACIENTE)
                .bloqueado(true)
                .build();

        when(usuarioRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(usuarioBloqueado));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                oAuth2ProvisioningService.procesarUsuarioOAuth2(email, "google_999", "Bloqueado", "Test", "User"));

        assertEquals("USER_BLOCKED", ex.getMessage());
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }
}
