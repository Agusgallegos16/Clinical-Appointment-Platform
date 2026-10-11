package com.consultorio.security;

import com.consultorio.domain.Paciente;
import com.consultorio.domain.Rol;
import com.consultorio.domain.Usuario;
import com.consultorio.repository.PacienteRepository;
import com.consultorio.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PacienteIdorSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PacienteRepository pacienteRepository;

    private Paciente pacienteA;
    private Paciente pacienteB;
    private Paciente menorDeA;
    private Usuario usuarioA;
    private Usuario usuarioB;

    @BeforeEach
    void setUp() {
        usuarioA = usuarioRepository.save(Usuario.builder()
                .email("idor_pacienteA_" + UUID.randomUUID() + "@test.com")
                .password("password123")
                .rol(Rol.PACIENTE)
                .activo(true)
                .emailVerificado(true)
                .build());

        pacienteA = pacienteRepository.save(Paciente.builder()
                .usuario(usuarioA)
                .nombre("Juan")
                .apellido("Perez")
                .dni(30111000L + (long) (Math.random() * 90000))
                .build());

        usuarioB = usuarioRepository.save(Usuario.builder()
                .email("idor_pacienteB_" + UUID.randomUUID() + "@test.com")
                .password("password123")
                .rol(Rol.PACIENTE)
                .activo(true)
                .emailVerificado(true)
                .build());

        pacienteB = pacienteRepository.save(Paciente.builder()
                .usuario(usuarioB)
                .nombre("Maria")
                .apellido("Gomez")
                .dni(30222000L + (long) (Math.random() * 90000))
                .build());

        menorDeA = pacienteRepository.save(Paciente.builder()
                .tutor(pacienteA)
                .nombre("Pedrito")
                .apellido("Perez")
                .dni(55111000L + (long) (Math.random() * 90000))
                .build());
    }

    @Test
    @DisplayName("BOLA/IDOR: Paciente A intentando ver paciente B debe recibir HTTP 403 Forbidden")
    void testPacienteNoPuedeVerOtroPaciente() throws Exception {
        mockMvc.perform(get("/api/pacientes/" + pacienteB.getId())
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(usuarioA.getEmail()).roles("PACIENTE"))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Paciente A consultando su propio perfil por ID debe recibir HTTP 200 OK")
    void testPacientePuedeVerSuPropioPerfil() throws Exception {
        mockMvc.perform(get("/api/pacientes/" + pacienteA.getId())
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(usuarioA.getEmail()).roles("PACIENTE"))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Juan"))
                .andExpect(jsonPath("$.apellido").value("Perez"));
    }

    @Test
    @DisplayName("Paciente A (tutor) consultando el perfil de su menor a cargo debe recibir HTTP 200 OK")
    void testTutorPuedeVerMenorACargo() throws Exception {
        mockMvc.perform(get("/api/pacientes/" + menorDeA.getId())
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(usuarioA.getEmail()).roles("PACIENTE"))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Pedrito"));
    }

    @Test
    @DisplayName("BOLA/IDOR: Paciente A intentando consultar estadísticas de paciente B debe recibir HTTP 403 Forbidden")
    void testPacienteNoPuedeVerEstadisticasDeOtroPaciente() throws Exception {
        mockMvc.perform(get("/api/pacientes/" + pacienteB.getId() + "/estadisticas")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(usuarioA.getEmail()).roles("PACIENTE"))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "medico@test.com", roles = {"DOCTOR"})
    @DisplayName("Doctor consultando perfil de cualquier paciente debe recibir HTTP 200 OK")
    void testDoctorPuedeVerPerfilDePaciente() throws Exception {
        mockMvc.perform(get("/api/pacientes/" + pacienteB.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Maria"));
    }

    @Test
    @WithMockUser(username = "secretaria@test.com", roles = {"SECRETARIA"})
    @DisplayName("Secretaría consultando perfil de cualquier paciente debe recibir HTTP 200 OK")
    void testSecretariaPuedeVerPerfilDePaciente() throws Exception {
        mockMvc.perform(get("/api/pacientes/" + pacienteB.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Maria"));
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = {"ADMIN"})
    @DisplayName("Administrador consultando perfil de cualquier paciente debe recibir HTTP 200 OK")
    void testAdminPuedeVerPerfilDePaciente() throws Exception {
        mockMvc.perform(get("/api/pacientes/" + pacienteA.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Juan"));
    }
}
