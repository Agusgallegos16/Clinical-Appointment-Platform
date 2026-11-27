package com.consultorio.security;

import com.consultorio.domain.Paciente;
import com.consultorio.domain.Rol;
import com.consultorio.domain.Usuario;
import com.consultorio.repository.PacienteRepository;
import com.consultorio.repository.UsuarioRepository;
import com.consultorio.security.oauth2.OAuth2AuthenticationSuccessHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class JwtLifecycleAndAuthHardeningSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PacienteRepository pacienteRepository;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private OAuth2AuthenticationSuccessHandler oauth2SuccessHandler;

    @Test
    @DisplayName("S8: /api/auth/me does not re-mint JWT and password reset revokes previously issued tokens via tokenVersion")
    void meDoesNotRemintAndPasswordResetRevokesOldJwt() throws Exception {
        String email = "s8_tv_" + UUID.randomUUID() + "@test.com";
        Usuario usuario = usuarioRepository.saveAndFlush(Usuario.builder()
                .email(email)
                .password(passwordEncoder.encode("InitialValidPass12!"))
                .rol(Rol.PACIENTE)
                .activo(true)
                .emailVerificado(true)
                .bloqueado(false)
                .tokenRestablecimientoPassword("valid-reset-token-s8")
                .tokenRestablecimientoExpiracion(LocalDateTime.now().plusHours(1))
                .build());

        String oldJwt = jwtUtils.generarToken(email, Rol.PACIENTE.name());

        // /api/auth/me must return user profile WITHOUT minting a new token
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + oldJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.token").doesNotExist());

        // Reset password -> must increment tokenVersion and revoke oldJwt
        mockMvc.perform(post("/api/auth/confirmar-restablecimiento-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "token": "valid-reset-token-s8",
                                  "nuevaPassword": "UpdatedStrongPass99!",
                                  "confirmarNuevaPassword": "UpdatedStrongPass99!"
                                }
                                """))
                .andExpect(status().isOk());

        // Old JWT must now be rejected with 401
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + oldJwt))
                .andExpect(status().isUnauthorized());

        // Explicit logout must also revoke a newly issued JWT
        String freshJwt = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"UpdatedStrongPass99!"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String extractedToken = com.fasterxml.jackson.databind.json.JsonMapper.builder().build()
                .readTree(freshJwt).get("token").asText();

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + extractedToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + extractedToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("S8: OAuth2 success handler issues opaque single-use code (no JWT in URL) and rejects unverified emails")
    void oauth2UsesOneTimeCodeAndChecksEmailVerified() throws Exception {
        String verifiedEmail = "s8_oauth_ok_" + UUID.randomUUID() + "@gmail.com";
        DefaultOAuth2User verifiedUser = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                Map.of(
                        "email", verifiedEmail,
                        "email_verified", true,
                        "sub", "google-sub-ok-1",
                        "name", "Ana OAuth",
                        "given_name", "Ana",
                        "family_name", "OAuth"
                ),
                "email"
        );
        OAuth2AuthenticationToken authOk = new OAuth2AuthenticationToken(verifiedUser, verifiedUser.getAuthorities(), "google");
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();

        oauth2SuccessHandler.onAuthenticationSuccess(req, res, authOk);
        String redirectedUrl = res.getRedirectedUrl();

        assertThat(redirectedUrl).isNotNull();
        assertThat(redirectedUrl).doesNotContain("token=");
        assertThat(redirectedUrl).contains("code=");

        String code = redirectedUrl.substring(redirectedUrl.indexOf("code=") + 5);
        if (code.contains("&")) {
            code = code.substring(0, code.indexOf("&"));
        }

        // Redeem code once -> 200 with JWT
        mockMvc.perform(post("/api/auth/oauth2/exchange")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"" + code + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.email").value(verifiedEmail));

        // Replay same code -> 400/401 rejected
        mockMvc.perform(post("/api/auth/oauth2/exchange")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"" + code + "\"}"))
                .andExpect(status().isBadRequest());

        // Unverified email must NOT link or login
        String unverifiedEmail = "s8_oauth_unverified_" + UUID.randomUUID() + "@gmail.com";
        DefaultOAuth2User unverifiedUser = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                Map.of(
                        "email", unverifiedEmail,
                        "email_verified", false,
                        "sub", "google-sub-bad-2",
                        "name", "Evil Unverified"
                ),
                "email"
        );
        OAuth2AuthenticationToken authBad = new OAuth2AuthenticationToken(unverifiedUser, unverifiedUser.getAuthorities(), "google");
        MockHttpServletResponse resBad = new MockHttpServletResponse();

        oauth2SuccessHandler.onAuthenticationSuccess(new MockHttpServletRequest(), resBad, authBad);
        assertThat(resBad.getRedirectedUrl()).contains("/login?error=email_not_verified");
        assertThat(usuarioRepository.findByEmailIgnoreCase(unverifiedEmail)).isEmpty();
    }

    @Test
    @DisplayName("S8: Credential updates require valid currentPassword; >=12-char password policy enforced across auth endpoints")
    void credentialUpdatesRequireCurrentPasswordAnd12CharPolicy() throws Exception {
        String email = "s8_cred_" + UUID.randomUUID() + "@test.com";
        Usuario usuario = usuarioRepository.saveAndFlush(Usuario.builder()
                .email(email)
                .password(passwordEncoder.encode("CurrentStrong12!"))
                .rol(Rol.PACIENTE)
                .activo(true)
                .emailVerificado(true)
                .bloqueado(false)
                .build());
        pacienteRepository.saveAndFlush(Paciente.builder()
                .usuario(usuario)
                .nombre("Luis")
                .apellido("Prueba")
                .dni(44112233L + (long) (Math.random() * 10000))
                .telefono("1122334455")
                .build());

        String jwt = jwtUtils.generarToken(email, Rol.PACIENTE.name());

        // Changing non-credential profile data without currentPassword still works
        mockMvc.perform(put("/api/pacientes/mi-perfil")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "Luis Alberto",
                                  "apellido": "Prueba",
                                  "telefono": "1199887766",
                                  "email": "%s"
                                }
                                """.formatted(email)))
                .andExpect(status().isOk());

        // Changing email WITHOUT currentPassword -> 400
        String newEmail = "s8_new_" + UUID.randomUUID() + "@test.com";
        mockMvc.perform(put("/api/pacientes/mi-perfil")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "Luis Alberto",
                                  "apellido": "Prueba",
                                  "telefono": "1199887766",
                                  "email": "%s"
                                }
                                """.formatted(newEmail)))
                .andExpect(status().isBadRequest());

        // Changing password with WRONG currentPassword -> 400
        mockMvc.perform(put("/api/pacientes/mi-perfil")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "Luis Alberto",
                                  "apellido": "Prueba",
                                  "telefono": "1199887766",
                                  "email": "%s",
                                  "password": "AnotherStrong12!",
                                  "currentPassword": "WrongPassword99!"
                                }
                                """.formatted(email)))
                .andExpect(status().isBadRequest());

        // Changing email + password WITH valid currentPassword -> 200
        mockMvc.perform(put("/api/pacientes/mi-perfil")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "Luis Alberto",
                                  "apellido": "Prueba",
                                  "telefono": "1199887766",
                                  "email": "%s",
                                  "password": "BrandNewStrong12!",
                                  "currentPassword": "CurrentStrong12!"
                                }
                                """.formatted(newEmail)))
                .andExpect(status().isOk());

        // Password shorter than 12 chars rejected on registration, reset confirm, and doctor setup
        mockMvc.perform(post("/api/auth/registro-paciente")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "short_pw_%s@test.com",
                                  "password": "Short11Chr!",
                                  "confirmarPassword": "Short11Chr!",
                                  "nombre": "Short",
                                  "apellido": "Pass",
                                  "dni": 49123456,
                                  "telefono": "11223344",
                                  "fechaNacimiento": "1990-01-01"
                                }
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/auth/confirmar-restablecimiento-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "token": "any-token",
                                  "nuevaPassword": "Short11Chr!",
                                  "confirmarNuevaPassword": "Short11Chr!"
                                }
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/auth/establecer-password-doctor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "token": "any-token",
                                  "password": "Short11Chr!",
                                  "confirmarPassword": "Short11Chr!"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }
}
