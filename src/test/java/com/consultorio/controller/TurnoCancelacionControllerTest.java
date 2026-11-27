package com.consultorio.controller;

import com.consultorio.domain.*;
import com.consultorio.dto.CancelarTurnoDTO;
import com.consultorio.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TurnoCancelacionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TurnoRepository turnoRepository;

    @Autowired
    private PacienteRepository pacienteRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private EspecialidadRepository especialidadRepository;

    private String emailPaciente;
    private String emailDoctor;
    private Turno turno;

    @BeforeEach
    void setUp() {
        emailPaciente = "paciente_cancel_" + UUID.randomUUID() + "@test.com";
        emailDoctor = "doctor_cancel_" + UUID.randomUUID() + "@test.com";

        Usuario usuarioPaciente = Usuario.builder()
                .email(emailPaciente)
                .password("password123")
                .rol(Rol.PACIENTE)
                .activo(true)
                .emailVerificado(true)
                .build();

        Paciente paciente = pacienteRepository.save(Paciente.builder()
                .usuario(usuarioPaciente)
                .nombre("Juan")
                .apellido("Perez")
                .dni(10000000L + (long)(Math.random() * 80000000L))
                .telefono("1122334455")
                .fechaNacimiento(LocalDate.of(1990, 5, 15))
                .build());

        Usuario usuarioDoctor = Usuario.builder()
                .email(emailDoctor)
                .password("password123")
                .rol(Rol.DOCTOR)
                .activo(true)
                .emailVerificado(true)
                .build();

        Especialidad especialidad = especialidadRepository.save(Especialidad.builder()
                .nombre("Cardiologia Cancel Test " + UUID.randomUUID())
                .descripcion("Especialidad de prueba")
                .build());

        Doctor doctor = doctorRepository.save(Doctor.builder()
                .usuario(usuarioDoctor)
                .nombre("Martin")
                .apellido("Palermo")
                .disponibleParaTurnos(true)
                .especialidades(Set.of(especialidad))
                .build());

        turno = turnoRepository.save(Turno.builder()
                .paciente(paciente)
                .doctor(doctor)
                .especialidad(especialidad)
                .fechaHora(LocalDateTime.now().plusDays(3))
                .estado(EstadoTurno.CONFIRMADO)
                .motivoConsulta("Chequeo anual")
                .build());
    }

    @Test
    @DisplayName("Cancelar turno como Paciente sin motivo en body debe retornar HTTP 200 OK y estado CANCELADO")
    void testCancelarTurnoComoPacienteSinMotivo() throws Exception {
        mockMvc.perform(put("/api/turnos/{id}/cancelar", turno.getId())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(emailPaciente).roles("PACIENTE"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(turno.getId().toString())))
                .andExpect(jsonPath("$.estado", is("CANCELADO")));
    }

    @Test
    @DisplayName("Cancelar turno como Paciente con motivo opcional debe retornar HTTP 200 OK y persistir el motivo")
    void testCancelarTurnoComoPacienteConMotivo() throws Exception {
        CancelarTurnoDTO dto = new CancelarTurnoDTO("Viaje imprevisto por trabajo");

        mockMvc.perform(put("/api/turnos/{id}/cancelar", turno.getId())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(emailPaciente).roles("PACIENTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado", is("CANCELADO")))
                .andExpect(jsonPath("$.motivoCancelacion", is("Viaje imprevisto por trabajo")));
    }

    @Test
    @DisplayName("Cancelar turno con motivo mayor a 500 caracteres debe ser rechazado con HTTP 400 Bad Request")
    void testCancelarTurnoConMotivoExcesivo() throws Exception {
        String motivoLargo = "A".repeat(501);
        CancelarTurnoDTO dto = new CancelarTurnoDTO(motivoLargo);

        mockMvc.perform(put("/api/turnos/{id}/cancelar", turno.getId())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(emailPaciente).roles("PACIENTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Cancelar turno como Doctor con justificación obligatoria debe retornar HTTP 200 OK")
    void testCancelarTurnoComoDoctorConMotivo() throws Exception {
        CancelarTurnoDTO dto = new CancelarTurnoDTO("Urgencia en sala de emergencias");

        mockMvc.perform(put("/api/turnos/{id}/cancelar", turno.getId())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(emailDoctor).roles("DOCTOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado", is("CANCELADO")))
                .andExpect(jsonPath("$.motivoCancelacion", is("Urgencia en sala de emergencias")));
    }

    @Test
    @DisplayName("Cancelar turno como Doctor sin justificación debe retornar HTTP 400 Bad Request")
    void testCancelarTurnoComoDoctorSinMotivoRechaza() throws Exception {
        CancelarTurnoDTO dto = new CancelarTurnoDTO("   ");

        mockMvc.perform(put("/api/turnos/{id}/cancelar", turno.getId())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(emailDoctor).roles("DOCTOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Cancelar turno mediante endpoint legado /cancelar-doctor con query param debe retornar HTTP 200 OK")
    void testCancelarTurnoEndpointLegadoDoctor() throws Exception {
        mockMvc.perform(put("/api/turnos/{id}/cancelar-doctor", turno.getId())
                        .param("motivo", "Cirugia no programada")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(emailDoctor).roles("DOCTOR"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado", is("CANCELADO")))
                .andExpect(jsonPath("$.motivoCancelacion", is("Cirugia no programada")));
    }

    @Test
    @DisplayName("Usuario ajeno al turno intenta cancelarlo y debe recibir HTTP 403 Forbidden")
    void testCancelarTurnoUsuarioNoAutorizadoIdor() throws Exception {
        mockMvc.perform(put("/api/turnos/{id}/cancelar", turno.getId())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("hacker@test.com").roles("PACIENTE"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }
}
