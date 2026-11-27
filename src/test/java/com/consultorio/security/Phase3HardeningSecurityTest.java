package com.consultorio.security;

import com.consultorio.domain.*;
import com.consultorio.dto.TurnoResponseDTO;
import com.consultorio.factory.EmailTemplateFactory;
import com.consultorio.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class Phase3HardeningSecurityTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private DoctorRepository doctorRepository;
    @Autowired private PacienteRepository pacienteRepository;
    @Autowired private EspecialidadRepository especialidadRepository;
    @Autowired private TurnoRepository turnoRepository;
    @Autowired private EmailTemplateFactory emailTemplateFactory;

    @Test
    @DisplayName("S9: Doctor can access patient ONLY with a treatment relationship; DNI lookup forbidden for DOCTOR")
    void doctorScopedToTreatmentRelationship() throws Exception {
        Especialidad esp = especialidadRepository.save(Especialidad.builder()
                .nombre("S9Esp_" + UUID.randomUUID().toString().substring(0, 8)).build());

        Usuario uDoc = usuarioRepository.save(Usuario.builder()
                .email("s9_doc_" + UUID.randomUUID() + "@test.com")
                .password("encoded-password-12").rol(Rol.DOCTOR).activo(true).emailVerificado(true).build());
        Doctor doctor = doctorRepository.save(Doctor.builder()
                .usuario(uDoc).nombre("Dra").apellido("Scope").disponibleParaTurnos(true)
                .especialidades(new HashSet<>(Set.of(esp))).build());

        Paciente treatedPatient = pacienteRepository.save(Paciente.builder()
                .nombre("Paciente").apellido("Atendido")
                .dni(61000000L + (long) (Math.random() * 90000)).build());
        Paciente unrelatedPatient = pacienteRepository.save(Paciente.builder()
                .nombre("Paciente").apellido("Ajeno")
                .dni(62000000L + (long) (Math.random() * 90000)).build());

        turnoRepository.save(Turno.builder()
                .doctor(doctor).paciente(treatedPatient).especialidad(esp)
                .fechaHora(LocalDateTime.now().plusDays(1)).estado(EstadoTurno.CONFIRMADO).build());

        // Unrelated patient -> 403 on profile, stats, and appointments
        mockMvc.perform(get("/api/pacientes/" + unrelatedPatient.getId())
                        .with(user(uDoc.getEmail()).roles("DOCTOR")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/pacientes/" + unrelatedPatient.getId() + "/estadisticas")
                        .with(user(uDoc.getEmail()).roles("DOCTOR")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/turnos/paciente/" + unrelatedPatient.getId())
                        .with(user(uDoc.getEmail()).roles("DOCTOR")))
                .andExpect(status().isForbidden());

        // DNI lookup -> forbidden for DOCTOR, allowed for SECRETARIA
        mockMvc.perform(get("/api/pacientes/buscar-por-dni/" + treatedPatient.getDni())
                        .with(user(uDoc.getEmail()).roles("DOCTOR")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/pacientes/buscar-por-dni/" + treatedPatient.getDni())
                        .with(user("sec@test.com").roles("SECRETARIA")))
                .andExpect(status().isOk());

        // Treated patient -> 200 OK for doctor
        mockMvc.perform(get("/api/pacientes/" + treatedPatient.getId())
                        .with(user(uDoc.getEmail()).roles("DOCTOR")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/pacientes/" + treatedPatient.getId() + "/estadisticas")
                        .with(user(uDoc.getEmail()).roles("DOCTOR")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/turnos/paciente/" + treatedPatient.getId())
                        .with(user(uDoc.getEmail()).roles("DOCTOR")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("S9: Public Doctor DTO redacts email/usuario while ADMIN retains them")
    void publicDoctorDtoRedacted() throws Exception {
        String privateEmail = "private_doc_" + UUID.randomUUID() + "@clinic.com";
        Usuario uDoc = usuarioRepository.save(Usuario.builder()
                .email(privateEmail).password("encoded-password-12").rol(Rol.DOCTOR).activo(true).emailVerificado(true).build());
        Doctor doctor = doctorRepository.save(Doctor.builder()
                .usuario(uDoc).nombre("Publico").apellido("Doctor").disponibleParaTurnos(true).build());

        // Public unauthenticated -> email & usuario redacted
        mockMvc.perform(get("/api/doctores/" + doctor.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Publico"))
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.usuario").doesNotExist());

        // ADMIN -> retains email & usuario
        mockMvc.perform(get("/api/doctores/" + doctor.getId())
                        .with(user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(privateEmail))
                .andExpect(jsonPath("$.usuario.email").value(privateEmail));
    }

    @Test
    @DisplayName("S9: HTTP security headers are emitted on API responses")
    void httpSecurityHeadersPresent() throws Exception {
        mockMvc.perform(get("/api/public/ping"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().exists("Content-Security-Policy"))
                .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
                .andExpect(header().exists("Permissions-Policy"));
    }

    @Test
    @DisplayName("S9: EmailTemplateFactory HTML-escapes user-controlled values against HTML/XSS injection")
    void emailTemplatesEscapeHtml() {
        String xssPayload = "<script>alert('xss')</script><img src=x onerror=alert(1)>";
        TurnoResponseDTO dto = TurnoResponseDTO.builder()
                .pacienteNombre(xssPayload)
                .doctorNombre("Dr. <Evil>")
                .especialidadNombre("Cardio & <b>Test</b>")
                .obraSocial("<a href='http://evil'>Click</a>")
                .motivoConsulta("<script>steal()</script>")
                .fechaHora(LocalDateTime.of(2026, 12, 1, 10, 30))
                .build();

        String html = emailTemplateFactory.crearEmailConfirmacionTurno(dto);
        assertThat(html).doesNotContain("<script>");
        assertThat(html).doesNotContain("<img src=x");
        assertThat(html).contains("&lt;script&gt;");

        String welcomeHtml = emailTemplateFactory.crearEmailBienvenida(xssPayload);
        assertThat(welcomeHtml).doesNotContain("<script>");
        assertThat(welcomeHtml).contains("&lt;script&gt;");
    }

    @Test
    @DisplayName("S9: RateLimitingFilter ignores spoofed X-Forwarded-For from untrusted proxy and enforces limits")
    void rateLimitingIgnoresSpoofedXff() throws Exception {
        RateLimitingFilter filter = new RateLimitingFilter();
        for (int i = 0; i < 10; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/auth/login");
            req.setRemoteAddr("198.51.100.10");
            req.addHeader("X-Forwarded-For", "203.0.113." + i); // attacker spoofs a new IP every request
            MockHttpServletResponse res = new MockHttpServletResponse();
            filter.doFilter(req, res, new MockFilterChain());
            assertThat(res.getStatus()).isEqualTo(200);
        }

        // 11th request from same remoteAddr with yet another spoofed XFF must be rate-limited (429)
        MockHttpServletRequest blockedReq = new MockHttpServletRequest("POST", "/api/auth/login");
        blockedReq.setRemoteAddr("198.51.100.10");
        blockedReq.addHeader("X-Forwarded-For", "203.0.113.99");
        MockHttpServletResponse blockedRes = new MockHttpServletResponse();
        filter.doFilter(blockedReq, blockedRes, new MockFilterChain());
        assertThat(blockedRes.getStatus()).isEqualTo(429);
    }
}
