package com.consultorio.security;

import com.consultorio.domain.Rol;
import com.consultorio.domain.Usuario;
import com.consultorio.repository.AuditLogRepository;
import com.consultorio.repository.UsuarioRepository;
import com.consultorio.util.LogSanitizer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuditAndLogHygieneSecurityTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("S10: Login events are persisted to append-only audit_log and readable only by ADMIN via /api/admin/auditoria")
    void loginEventsPersistedToAuditLogAndAdminOnlyReadable() throws Exception {
        String email = "s10_audit_" + UUID.randomUUID() + "@test.com";
        Usuario u = usuarioRepository.saveAndFlush(Usuario.builder()
                .email(email)
                .password(passwordEncoder.encode("AuditStrongPass12!"))
                .rol(Rol.PACIENTE)
                .activo(true)
                .emailVerificado(true)
                .bloqueado(false)
                .build());

        try {
            // Trigger a successful login
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email":"%s","password":"AuditStrongPass12!"}
                                    """.formatted(email)))
                    .andExpect(status().isOk());

            // Non-admin cannot read audit log
            mockMvc.perform(get("/api/admin/auditoria")
                            .with(user(email).roles("PACIENTE")))
                    .andExpect(status().isForbidden());

            // ADMIN reads persisted audit log containing AUTH_LOGIN_SUCCESS
            mockMvc.perform(get("/api/admin/auditoria")
                            .with(user("admin@test.com").roles("ADMIN")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].eventType").value("AUTH_LOGIN_SUCCESS"))
                    .andExpect(jsonPath("$.content[0].outcome").value("SUCCESS"));
            // Failed login must also persist AUTH_LOGIN_FAILURE even though exception is thrown
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email":"%s","password":"WrongPassword123!"}
                                    """.formatted(email)))
                    .andExpect(status().isUnauthorized());

            mockMvc.perform(get("/api/admin/auditoria")
                            .param("eventType", "AUTH_LOGIN_FAILURE")
                            .with(user("admin@test.com").roles("ADMIN")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].eventType").value("AUTH_LOGIN_FAILURE"))
                    .andExpect(jsonPath("$.content[0].outcome").value("FAILURE"));
        } finally {
            usuarioRepository.deleteById(u.getId());
        }
    }

    @Test
    @DisplayName("S10: AuditLogRepository is structurally append-only and LogSanitizer masks emails and query tokens")
    void auditRepositoryAppendOnlyAndLogSanitizerWorks() {
        boolean hasDeleteMethod = Arrays.stream(AuditLogRepository.class.getMethods())
                .map(Method::getName)
                .anyMatch(name -> name.toLowerCase().contains("delete"));
        assertThat(hasDeleteMethod).isFalse();

        assertThat(LogSanitizer.maskEmail("paciente@gmail.com")).isEqualTo("p***@gmail.com");
        assertThat(LogSanitizer.maskEmail("a@b.com")).isEqualTo("*@b.com");
        assertThat(LogSanitizer.redactTokens("https://app.com/reset?token=secret123&code=abc999"))
                .doesNotContain("secret123")
                .doesNotContain("abc999")
                .contains("token=[REDACTED]")
                .contains("code=[REDACTED]");
    }

    @Test
    @DisplayName("S10: Dockerfile runs as non-root user and CI SCA workflow exists with least-privilege permissions")
    void dockerfileNonRootAndScaWorkflowConfigured() throws Exception {
        String dockerfile = Files.readString(Path.of("Dockerfile"));
        assertThat(dockerfile).contains("USER ");
        assertThat(dockerfile).doesNotContain("USER root");

        Path scaWorkflow = Path.of(".github/workflows/security-sca.yml");
        assertThat(Files.exists(scaWorkflow)).isTrue();
        String workflowContent = Files.readString(scaWorkflow);
        assertThat(workflowContent).contains("permissions:");
        assertThat(workflowContent).contains("contents: read");
        assertThat(workflowContent).contains("trivy-action");
    }
}
