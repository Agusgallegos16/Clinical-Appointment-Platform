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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TurnoCancelacionTardiaTest {

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
    private String emailSecretaria;
    private String emailAdmin;

    private Paciente paciente;
    private Doctor doctor;
    private Especialidad especialidad;
    private Turno turnoMenos24hs;
    private Turno turnoMas24hs;

    @BeforeEach
    void setUp() {
        emailPaciente = "paciente_tardia_" + UUID.randomUUID() + "@test.com";
        emailDoctor = "doctor_tardia_" + UUID.randomUUID() + "@test.com";
        emailSecretaria = "sec_tardia_" + UUID.randomUUID() + "@test.com";
        emailAdmin = "admin_tardia_" + UUID.randomUUID() + "@test.com";

        Usuario usuarioPaciente = Usuario.builder()
                .email(emailPaciente)
                .password("password123")
                .rol(Rol.PACIENTE)
                .activo(true)
                .emailVerificado(true)
                .build();

        paciente = pacienteRepository.save(Paciente.builder()
                .usuario(usuarioPaciente)
                .nombre("Lucas")
                .apellido("Gomez")
                .dni(10000000L + (long)(Math.random() * 80000000L))
                .telefono("1133445566")
                .fechaNacimiento(LocalDate.of(1992, 3, 20))
                .build());

        Usuario usuarioDoctor = Usuario.builder()
                .email(emailDoctor)
                .password("password123")
                .rol(Rol.DOCTOR)
                .activo(true)
                .emailVerificado(true)
                .build();

        especialidad = especialidadRepository.save(Especialidad.builder()
                .nombre("Traumatologia Tardia " + UUID.randomUUID())
                .descripcion("Especialidad de prueba")
                .build());

        doctor = doctorRepository.save(Doctor.builder()
                .usuario(usuarioDoctor)
                .nombre("Jorge")
                .apellido("Bermudez")
                .disponibleParaTurnos(true)
                .especialidades(Set.of(especialidad))
                .build());

        // Turno con 8 horas de anticipación (menos de 24 horas)
        turnoMenos24hs = turnoRepository.save(Turno.builder()
                .paciente(paciente)
                .doctor(doctor)
                .especialidad(especialidad)
                .fechaHora(LocalDateTime.now().plusHours(8))
                .estado(EstadoTurno.CONFIRMADO)
                .motivoConsulta("Dolor de rodilla")
                .build());

        // Turno con 48 horas de anticipación (más de 24 horas)
        turnoMas24hs = turnoRepository.save(Turno.builder()
                .paciente(paciente)
                .doctor(doctor)
                .especialidad(especialidad)
                .fechaHora(LocalDateTime.now().plusHours(48))
                .estado(EstadoTurno.CONFIRMADO)
                .motivoConsulta("Control postoperatorio")
                .build());
    }

    @Test
    @DisplayName("Paciente intentando cancelar turno con menos de 24 hs debe recibir HTTP 409 Conflict con detalle y teléfono")
    void testPacienteCancelarTurnoMenos24hsRechazado() throws Exception {
        CancelarTurnoDTO dto = new CancelarTurnoDTO("Surgió un imprevisto");

        mockMvc.perform(put("/api/turnos/{id}/cancelar", turnoMenos24hs.getId())
                        .with(user(emailPaciente).roles("PACIENTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.code", is("TURNO_CANCELACION_ANTICIPACION_INSUFICIENTE")))
                .andExpect(jsonPath("$.telefonoContacto", notNullValue()))
                .andExpect(jsonPath("$.detail", containsString("24 horas")));

        // Verificar que el estado del turno no cambió
        Turno turnoActual = turnoRepository.findById(turnoMenos24hs.getId()).orElseThrow();
        assertEquals(EstadoTurno.CONFIRMADO, turnoActual.getEstado());
    }

    @Test
    @DisplayName("Paciente puede cancelar turno con más de 24 hs de anticipación exitosamente (HTTP 200 OK)")
    void testPacienteCancelarTurnoMas24hsPermitido() throws Exception {
        CancelarTurnoDTO dto = new CancelarTurnoDTO("Viaje con antelación");

        mockMvc.perform(put("/api/turnos/{id}/cancelar", turnoMas24hs.getId())
                        .with(user(emailPaciente).roles("PACIENTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado", is("CANCELADO")))
                .andExpect(jsonPath("$.motivoCancelacion", is("Viaje con antelación")));

        Turno turnoActual = turnoRepository.findById(turnoMas24hs.getId()).orElseThrow();
        assertEquals(EstadoTurno.CANCELADO, turnoActual.getEstado());
    }

    @Test
    @DisplayName("Doctor asignado puede cancelar turno con menos de 24 hs (Staff exento de la regla de 24 hs)")
    void testDoctorCancelaTurnoMenos24hsExento() throws Exception {
        CancelarTurnoDTO dto = new CancelarTurnoDTO("Urgencia médica en quirófano");

        mockMvc.perform(put("/api/turnos/{id}/cancelar", turnoMenos24hs.getId())
                        .with(user(emailDoctor).roles("DOCTOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado", is("CANCELADO")))
                .andExpect(jsonPath("$.motivoCancelacion", is("Urgencia médica en quirófano")));

        Turno turnoActual = turnoRepository.findById(turnoMenos24hs.getId()).orElseThrow();
        assertEquals(EstadoTurno.CANCELADO, turnoActual.getEstado());
    }

    @Test
    @DisplayName("Secretaría puede cancelar turno con menos de 24 hs (Staff exento de la regla de 24 hs)")
    void testSecretariaCancelaTurnoMenos24hsExento() throws Exception {
        CancelarTurnoDTO dto = new CancelarTurnoDTO("Aviso telefónico del paciente");

        mockMvc.perform(put("/api/turnos/{id}/cancelar", turnoMenos24hs.getId())
                        .with(user(emailSecretaria).roles("SECRETARIA"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado", is("CANCELADO")));

        Turno turnoActual = turnoRepository.findById(turnoMenos24hs.getId()).orElseThrow();
        assertEquals(EstadoTurno.CANCELADO, turnoActual.getEstado());
    }

    @Test
    @DisplayName("Admin puede cancelar turno con menos de 24 hs (Staff exento de la regla de 24 hs)")
    void testAdminCancelaTurnoMenos24hsExento() throws Exception {
        CancelarTurnoDTO dto = new CancelarTurnoDTO("Mantenimiento edilicio de consultorios");

        mockMvc.perform(put("/api/turnos/{id}/cancelar", turnoMenos24hs.getId())
                        .with(user(emailAdmin).roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado", is("CANCELADO")));

        Turno turnoActual = turnoRepository.findById(turnoMenos24hs.getId()).orElseThrow();
        assertEquals(EstadoTurno.CANCELADO, turnoActual.getEstado());
    }
}
