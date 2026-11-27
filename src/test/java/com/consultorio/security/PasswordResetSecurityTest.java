package com.consultorio.security;

import com.consultorio.domain.Rol;
import com.consultorio.domain.Usuario;
import com.consultorio.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PasswordResetSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = usuarioRepository.saveAndFlush(Usuario.builder()
                .email("reset_user_" + UUID.randomUUID() + "@test.com")
                .password(passwordEncoder.encode("OldPassword123!"))
                .rol(Rol.PACIENTE)
                .activo(true)
                .emailVerificado(true)
                .build());
    }

    @Test
    @DisplayName("Step 1 accepts email only without staging a pending password; GET confirm is rejected (405); Step 2 POST sets new password")
    void twoStepPostPasswordResetFlow() throws Exception {
        // Step 1: request reset with email only
        String requestPayload = """
                { "email": "%s" }
                """.formatted(usuario.getEmail());

        mockMvc.perform(post("/api/auth/solicitar-restablecimiento-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestPayload))
                .andExpect(status().isOk());

        Usuario afterStep1 = usuarioRepository.findById(usuario.getId()).orElseThrow();
        String token = afterStep1.getTokenRestablecimientoPassword();
        assertThat(token).isNotBlank();
        assertThat(afterStep1.getNuevaPasswordPendiente()).isNull();
        assertThat(passwordEncoder.matches("OldPassword123!", afterStep1.getPassword())).isTrue();

        // Legacy state-mutating GET endpoint must no longer be allowed
        mockMvc.perform(get("/api/auth/confirmar-restablecimiento-password")
                        .param("token", token))
                .andExpect(status().isMethodNotAllowed());

        // Step 2: confirm via POST with token + new password
        String confirmPayload = """
                {
                  "token": "%s",
                  "nuevaPassword": "NewSecurePassword456!",
                  "confirmarNuevaPassword": "NewSecurePassword456!"
                }
                """.formatted(token);

        mockMvc.perform(post("/api/auth/confirmar-restablecimiento-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmPayload))
                .andExpect(status().isOk());

        Usuario afterStep2 = usuarioRepository.findById(usuario.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("NewSecurePassword456!", afterStep2.getPassword())).isTrue();
        assertThat(afterStep2.getTokenRestablecimientoPassword()).isNull();
        assertThat(afterStep2.getTokenRestablecimientoExpiracion()).isNull();

        // Reusing the same token must fail
        mockMvc.perform(post("/api/auth/confirmar-restablecimiento-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmPayload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Unknown email returns neutral 200 OK (no account enumeration) and expired token is rejected")
    void unknownEmailNeutralAndExpiredTokenRejected() throws Exception {
        mockMvc.perform(post("/api/auth/solicitar-restablecimiento-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"does-not-exist@test.com\"}"))
                .andExpect(status().isOk());

        usuario.setTokenRestablecimientoPassword("expired-token-123");
        usuario.setTokenRestablecimientoExpiracion(LocalDateTime.now().minusMinutes(5));
        usuarioRepository.saveAndFlush(usuario);

        String expiredPayload = """
                {
                  "token": "expired-token-123",
                  "nuevaPassword": "NewSecurePassword456!",
                  "confirmarNuevaPassword": "NewSecurePassword456!"
                }
                """;

        mockMvc.perform(post("/api/auth/confirmar-restablecimiento-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(expiredPayload))
                .andExpect(status().isBadRequest());
    }
}
