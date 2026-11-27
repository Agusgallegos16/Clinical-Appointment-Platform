package com.consultorio.security;

import com.consultorio.domain.*;
import com.consultorio.dto.TurnoResponseDTO;
import com.consultorio.event.TurnoCanceladoEvent;
import com.consultorio.factory.EmailTemplateFactory;
import com.consultorio.repository.*;
import com.consultorio.security.oauth2.OAuth2AuthenticationSuccessHandler;
import com.consultorio.security.oauth2.OAuth2OneTimeCodeService;
import com.consultorio.service.AuditLogService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.persistence.Column;
import org.hibernate.annotations.Immutable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class Phase3IndependentSecurityProbesTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private DoctorRepository doctorRepository;
    @Autowired private PacienteRepository pacienteRepository;
    @Autowired private EspecialidadRepository especialidadRepository;
    @Autowired private TurnoRepository turnoRepository;
    @Autowired private JwtUtils jwtUtils;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private OAuth2AuthenticationSuccessHandler oauth2SuccessHandler;
    @Autowired private OAuth2OneTimeCodeService oauth2OneTimeCodeService;
    @Autowired private EmailTemplateFactory emailTemplateFactory;
    @Autowired private AuditLogService auditLogService;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Test
    @DisplayName("S8-Probe: JWT without tv claim, non-numeric tv claim, and pre-lock JWT after unlock are all rejected (401)")
    @Transactional
    void s8JwtTokenVersionEdgeCases() throws Exception {
        String email = "probe_s8_tv_" + UUID.randomUUID() + "@test.com";
        Usuario u = usuarioRepository.saveAndFlush(Usuario.builder()
                .email(email)
                .password(passwordEncoder.encode("StrongPassword12!"))
                .rol(Rol.PACIENTE)
                .activo(true)
                .emailVerificado(true)
                .bloqueado(false)
                .build());

        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.trim().getBytes(StandardCharsets.UTF_8));
        Date now = new Date();
        Date exp = new Date(now.getTime() + 60_000L);

        // 1. Legacy JWT without tv claim -> 401
        String legacyNoTvJwt = Jwts.builder()
                .subject(email)
                .claim("rol", "PACIENTE")
                .issuedAt(now)
                .expiration(exp)
                .signWith(key)
                .compact();

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + legacyNoTvJwt))
                .andExpect(status().isUnauthorized());

        // 2. JWT with string tv claim -> 401
        String stringTvJwt = Jwts.builder()
                .subject(email)
                .claim("rol", "PACIENTE")
                .claim("tv", "0")
                .issuedAt(now)
                .expiration(exp)
                .signWith(key)
                .compact();

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + stringTvJwt))
                .andExpect(status().isUnauthorized());

        // 3. Valid JWT before admin lock -> after lock (403) and subsequent unlock -> still rejected (401)!
        String preLockJwt = jwtUtils.generarToken(email, "PACIENTE", u.getTokenVersionOrZero());
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + preLockJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").doesNotExist());

        // Admin locks user -> increments tokenVersion
        mockMvc.perform(patch("/api/admin/usuarios/" + u.getId() + "/bloquear")
                        .param("bloquear", "true")
                        .with(user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + preLockJwt))
                .andExpect(status().isForbidden());

        // Admin unlocks user -> pre-lock JWT must STILL be rejected due to bumped tokenVersion!
        mockMvc.perform(patch("/api/admin/usuarios/" + u.getId() + "/bloquear")
                        .param("bloquear", "false")
                        .with(user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + preLockJwt))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("S8-Probe: OAuth2 rejects missing/null/false email_verified without linking existing local user, and rejects blocked user at exchange")
    @Transactional
    void s8Oauth2EmailVerifiedAndBlockedExchangeEdgeCases() throws Exception {
        String existingEmail = "probe_s8_local_" + UUID.randomUUID() + "@test.com";
        Usuario existing = usuarioRepository.saveAndFlush(Usuario.builder()
                .email(existingEmail)
                .password(passwordEncoder.encode("LocalStrongPass12!"))
                .rol(Rol.PACIENTE)
                .activo(false)
                .emailVerificado(false)
                .bloqueado(false)
                .build());

        // 1. Missing email_verified attribute (null) must NOT link or activate existing user
        Map<String, Object> attrsNull = new HashMap<>();
        attrsNull.put("email", existingEmail);
        attrsNull.put("sub", "attacker-sub-1");
        attrsNull.put("name", "Attacker");
        DefaultOAuth2User userNull = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")), attrsNull, "email");
        MockHttpServletResponse resNull = new MockHttpServletResponse();
        oauth2SuccessHandler.onAuthenticationSuccess(new MockHttpServletRequest(), resNull,
                new OAuth2AuthenticationToken(userNull, userNull.getAuthorities(), "google"));

        assertThat(resNull.getRedirectedUrl()).contains("error=email_not_verified");
        assertThat(resNull.getRedirectedUrl()).doesNotContain("code=");
        Usuario reloaded = usuarioRepository.findById(existing.getId()).orElseThrow();
        assertThat(reloaded.getGoogleId()).isNull();
        assertThat(reloaded.isEmailVerificado()).isFalse();

        // 2. String "false" email_verified must also be rejected
        Map<String, Object> attrsStrFalse = new HashMap<>();
        attrsStrFalse.put("email", existingEmail);
        attrsStrFalse.put("email_verified", "false");
        attrsStrFalse.put("sub", "attacker-sub-2");
        DefaultOAuth2User userStrFalse = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")), attrsStrFalse, "email");
        MockHttpServletResponse resFalse = new MockHttpServletResponse();
        oauth2SuccessHandler.onAuthenticationSuccess(new MockHttpServletRequest(), resFalse,
                new OAuth2AuthenticationToken(userStrFalse, userStrFalse.getAuthorities(), "google"));
        assertThat(resFalse.getRedirectedUrl()).contains("error=email_not_verified");

        // 3. Blocked user between code issuance and exchange -> rejected
        String blockedEmail = "probe_s8_blk_" + UUID.randomUUID() + "@test.com";
        Usuario blkUser = usuarioRepository.saveAndFlush(Usuario.builder()
                .email(blockedEmail)
                .password(passwordEncoder.encode("BlockedPass123!"))
                .rol(Rol.PACIENTE)
                .activo(true)
                .emailVerificado(true)
                .bloqueado(false)
                .build());
        String code = oauth2OneTimeCodeService.issueCode(blkUser.getId());
        blkUser.setBloqueado(true);
        usuarioRepository.saveAndFlush(blkUser);

        mockMvc.perform(post("/api/auth/oauth2/exchange")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"" + code + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("S8-Probe: Doctor /api/doctores/mi-perfil enforces currentPassword on credential changes, >=12-char password, and revokes old JWT")
    @Transactional
    void s8DoctorProfileCurrentPasswordAndRevocation() throws Exception {
        String email = "probe_s8_doc_" + UUID.randomUUID() + "@test.com";
        Usuario uDoc = usuarioRepository.saveAndFlush(Usuario.builder()
                .email(email)
                .password(passwordEncoder.encode("DocCurrentPass12!"))
                .rol(Rol.DOCTOR)
                .activo(true)
                .emailVerificado(true)
                .bloqueado(false)
                .build());
        doctorRepository.saveAndFlush(Doctor.builder()
                .usuario(uDoc)
                .nombre("Carlos")
                .apellido("Medico")
                .disponibleParaTurnos(true)
                .build());

        String jwt = jwtUtils.generarToken(email, "DOCTOR", uDoc.getTokenVersionOrZero());

        // Non-credential edit without currentPassword -> 200 OK
        mockMvc.perform(put("/api/doctores/mi-perfil")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Carlos A.","apellido":"Medico","email":"%s"}
                                """.formatted(email)))
                .andExpect(status().isOk());

        // Email change without currentPassword -> 400
        String newEmail = "probe_s8_doc_new_" + UUID.randomUUID() + "@test.com";
        mockMvc.perform(put("/api/doctores/mi-perfil")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Carlos A.","apellido":"Medico","email":"%s"}
                                """.formatted(newEmail)))
                .andExpect(status().isBadRequest());

        // Password change with valid currentPassword but 11-char new password -> 400
        mockMvc.perform(put("/api/doctores/mi-perfil")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Carlos A.","apellido":"Medico","email":"%s","password":"Short11Chr!","currentPassword":"DocCurrentPass12!"}
                                """.formatted(email)))
                .andExpect(status().isBadRequest());

        // Password change with valid currentPassword and 12-char new password -> 200 and old JWT revoked (401)
        mockMvc.perform(put("/api/doctores/mi-perfil")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Carlos A.","apellido":"Medico","email":"%s","password":"ValidNewDoc12!","currentPassword":"DocCurrentPass12!"}
                                """.formatted(email)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isUnauthorized());

        // Logout without auth -> 401
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("S9-Probe: Doctor DTO redaction on list & detail across all roles; ADMIN/SECRETARIA patient access preserved")
    @Transactional
    void s9DoctorRedactionAllRolesAndPatientAccessMatrix() throws Exception {
        String docEmail = "probe_s9_doc_" + UUID.randomUUID() + "@clinic.com";
        Usuario uDoc = usuarioRepository.saveAndFlush(Usuario.builder()
                .email(docEmail)
                .password(passwordEncoder.encode("StrongPassword12!"))
                .rol(Rol.DOCTOR)
                .activo(true)
                .emailVerificado(true)
                .build());
        Doctor doc = doctorRepository.saveAndFlush(Doctor.builder()
                .usuario(uDoc)
                .nombre("Elena")
                .apellido("Cardio")
                .disponibleParaTurnos(true)
                .build());

        Paciente pac = pacienteRepository.saveAndFlush(Paciente.builder()
                .nombre("Juan")
                .apellido("Solo")
                .dni(77100000L + (long) (Math.random() * 90000))
                .build());

        // List & detail redaction for PACIENTE and DOCTOR
        for (var auth : List.of(
                user("pac@test.com").roles("PACIENTE"),
                user(docEmail).roles("DOCTOR")
        )) {
            mockMvc.perform(get("/api/doctores/" + doc.getId()).with(auth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").doesNotExist())
                    .andExpect(jsonPath("$.usuario").doesNotExist());

            String listBody = mockMvc.perform(get("/api/doctores").with(auth))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            assertThat(listBody).doesNotContain(docEmail);
        }

        // SECRETARIA and ADMIN keep email/usuario on both endpoints
        for (var auth : List.of(
                user("sec@test.com").roles("SECRETARIA"),
                user("admin@test.com").roles("ADMIN")
        )) {
            mockMvc.perform(get("/api/doctores/" + doc.getId()).with(auth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value(docEmail))
                    .andExpect(jsonPath("$.usuario.email").value(docEmail));

            mockMvc.perform(get("/api/pacientes/" + pac.getId()).with(auth))
                    .andExpect(status().isOk());
            mockMvc.perform(get("/api/pacientes/" + pac.getId() + "/estadisticas").with(auth))
                    .andExpect(status().isOk());
            mockMvc.perform(get("/api/turnos/paciente/" + pac.getId()).with(auth))
                    .andExpect(status().isOk());
            mockMvc.perform(get("/api/pacientes/buscar-por-dni/" + pac.getDni()).with(auth))
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("S9-Probe: RateLimitingFilter multi-endpoint limit, trusted proxy behavior, and bounded eviction")
    void s9RateLimitingTrustedProxyAndBoundedEviction() throws Exception {
        RateLimitingFilter filter = new RateLimitingFilter();
        ReflectionTestUtils.setField(filter, "trustedProxies", List.of("10.0.0.1"));

        // 1. Multi-endpoint limit: /api/auth/solicitar-restablecimiento-password allows 5/min
        for (int i = 0; i < 5; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/auth/solicitar-restablecimiento-password");
            req.setRemoteAddr("192.0.2.55");
            MockHttpServletResponse res = new MockHttpServletResponse();
            filter.doFilter(req, res, new MockFilterChain());
            assertThat(res.getStatus()).isEqualTo(200);
        }
        MockHttpServletRequest sixth = new MockHttpServletRequest("POST", "/api/auth/solicitar-restablecimiento-password");
        sixth.setRemoteAddr("192.0.2.55");
        MockHttpServletResponse sixthRes = new MockHttpServletResponse();
        filter.doFilter(sixth, sixthRes, new MockFilterChain());
        assertThat(sixthRes.getStatus()).isEqualTo(429);

        // 2. Trusted proxy (10.0.0.1) honors rightmost non-trusted XFF IP
        for (int i = 0; i < 12; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/auth/login");
            req.setRemoteAddr("10.0.0.1");
            req.addHeader("X-Forwarded-For", "198.51.100." + i);
            MockHttpServletResponse res = new MockHttpServletResponse();
            filter.doFilter(req, res, new MockFilterChain());
            assertThat(res.getStatus()).isEqualTo(200);
        }

        // 3. Bounded eviction: >10,000 keys must never grow unboundedly
        for (int i = 0; i < 10_250; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/auth/login");
            req.setRemoteAddr("10.1." + (i / 256) + "." + (i % 256));
            MockHttpServletResponse res = new MockHttpServletResponse();
            filter.doFilter(req, res, new MockFilterChain());
        }
        Map<?, ?> counts = (Map<?, ?>) ReflectionTestUtils.getField(filter, "requestCounts");
        assertThat(counts).isNotNull();
        assertThat(counts.size()).isLessThanOrEqualTo(10_000);
    }

    @Test
    @DisplayName("S9-Probe: ALL EmailTemplateFactory templates HTML-escape malicious payloads")
    void s9AllEmailTemplatesEscapeHtml() {
        String xss = "<script>alert(1)</script><img src=x onerror=alert(2)>";
        TurnoResponseDTO dto = TurnoResponseDTO.builder()
                .pacienteNombre(xss)
                .doctorNombre(xss)
                .especialidadNombre(xss)
                .obraSocial(xss)
                .motivoConsulta(xss)
                .fechaHora(LocalDateTime.of(2026, 12, 10, 14, 30))
                .estado(EstadoTurno.CONFIRMADO)
                .build();
        TurnoCanceladoEvent event = new TurnoCanceladoEvent(
                UUID.randomUUID(), xss, "PACIENTE", xss,
                "p@test.com", xss, "d@test.com", xss, xss,
                LocalDateTime.of(2026, 12, 10, 14, 30), null, null);

        List<String> htmls = List.of(
                emailTemplateFactory.crearEmailVerificacion(xss, "https://app.test/verify?t=" + xss),
                emailTemplateFactory.crearEmailActivacionDoctor(xss, "https://app.test/doc?t=" + xss),
                emailTemplateFactory.crearEmailRestablecerPassword("https://app.test/reset?t=" + xss),
                emailTemplateFactory.crearEmailBienvenida(xss),
                emailTemplateFactory.crearEmailConfirmacionTurno(dto),
                emailTemplateFactory.crearEmailRecordatorioTurno48hs(dto),
                emailTemplateFactory.crearEmailCancelacionDoctor(dto, xss),
                emailTemplateFactory.crearEmailCancelacionPacienteParaDoctor(event),
                emailTemplateFactory.crearEmailConfirmacionCancelacionPaciente(event),
                emailTemplateFactory.crearEmailConfirmacionCancelacionDoctor(event),
                emailTemplateFactory.crearEmailResumenDiarioDoctor(xss, xss, List.of(dto)),
                emailTemplateFactory.crearEmailResumenSemanalDoctor(xss, xss, List.of(dto))
        );

        for (String html : htmls) {
            assertThat(html).doesNotContain("<script>");
            assertThat(html).doesNotContain("<img src=x");
            assertThat(html).contains("&lt;script&gt;");
        }
    }

    @Test
    @DisplayName("S10-Probe: AuditLog is @Immutable with updatable=false columns, RBAC enforced, summary sanitized & truncated")
    void s10AuditLogImmutabilityRbacAndSanitization() throws Exception {
        assertThat(AuditLog.class.isAnnotationPresent(Immutable.class)).isTrue();
        for (Field f : AuditLog.class.getDeclaredFields()) {
            if (!"id".equals(f.getName())) {
                Column col = f.getAnnotation(Column.class);
                assertThat(col).as("Column annotation on %s", f.getName()).isNotNull();
                assertThat(col.updatable()).as("updatable=false on %s", f.getName()).isFalse();
            }
        }

        // Sanitization & truncation >255 chars
        String rawSummary = "Callback https://x.test?token=SUPERSECRET999&code=CODE888&password=PW777 " + "A".repeat(300);
        AuditLog saved = auditLogService.record("PROBE_EVENT", "SUCCESS", 1L, "ADMIN", "TEST", "1", rawSummary);
        assertThat(saved.getSummary()).doesNotContain("SUPERSECRET999");
        assertThat(saved.getSummary()).doesNotContain("CODE888");
        assertThat(saved.getSummary()).doesNotContain("PW777");
        assertThat(saved.getSummary().length()).isLessThanOrEqualTo(255);

        // RBAC on /api/admin/auditoria: unauthenticated -> 401; PACIENTE, DOCTOR, SECRETARIA -> 403
        mockMvc.perform(get("/api/admin/auditoria")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/auditoria").with(user("p@test.com").roles("PACIENTE"))).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/auditoria").with(user("d@test.com").roles("DOCTOR"))).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/auditoria").with(user("s@test.com").roles("SECRETARIA"))).andExpect(status().isForbidden());

        // ADMIN -> 200 with page/size clamping
        mockMvc.perform(get("/api/admin/auditoria")
                        .param("eventType", "PROBE_EVENT")
                        .param("page", "-5")
                        .param("size", "500")
                        .with(user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.content[0].eventType").value("PROBE_EVENT"));
    }
}
