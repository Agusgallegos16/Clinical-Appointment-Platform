package com.consultorio.security;

import com.consultorio.domain.*;
import com.consultorio.repository.*;
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
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DoctorIdorSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private PacienteRepository pacienteRepository;

    @Autowired
    private EspecialidadRepository especialidadRepository;

    @Autowired
    private HorarioAtencionRepository horarioAtencionRepository;

    @Autowired
    private SlotHorarioRepository slotHorarioRepository;

    @Autowired
    private PlantillaAgendaRepository plantillaAgendaRepository;

    @Autowired
    private TurnoRepository turnoRepository;

    private Usuario usuarioDoctorA;
    private Usuario usuarioDoctorB;
    private Doctor doctorA;
    private Doctor doctorB;
    private Especialidad especialidad;
    private HorarioAtencion horarioB;
    private SlotHorario slotA;
    private SlotHorario slotB;
    private PlantillaAgenda plantillaA;
    private PlantillaAgenda plantillaB;
    private Turno turnoA;
    private Turno turnoB;
    private LocalDate fechaFutura;

    @BeforeEach
    void setUp() {
        fechaFutura = LocalDate.now().plusDays(10);

        especialidad = especialidadRepository.save(Especialidad.builder()
                .nombre("Cardiologia_" + UUID.randomUUID().toString().substring(0, 8))
                .build());

        usuarioDoctorA = usuarioRepository.save(Usuario.builder()
                .email("idor_docA_" + UUID.randomUUID() + "@test.com")
                .password("encoded-password")
                .rol(Rol.DOCTOR)
                .activo(true)
                .emailVerificado(true)
                .build());

        doctorA = doctorRepository.save(Doctor.builder()
                .usuario(usuarioDoctorA)
                .nombre("Ana")
                .apellido("Alpha")
                .disponibleParaTurnos(true)
                .especialidades(new HashSet<>(Set.of(especialidad)))
                .build());

        usuarioDoctorB = usuarioRepository.save(Usuario.builder()
                .email("idor_docB_" + UUID.randomUUID() + "@test.com")
                .password("encoded-password")
                .rol(Rol.DOCTOR)
                .activo(true)
                .emailVerificado(true)
                .build());

        doctorB = doctorRepository.save(Doctor.builder()
                .usuario(usuarioDoctorB)
                .nombre("Bruno")
                .apellido("Beta")
                .disponibleParaTurnos(true)
                .especialidades(new HashSet<>(Set.of(especialidad)))
                .build());

        horarioB = horarioAtencionRepository.save(HorarioAtencion.builder()
                .doctor(doctorB)
                .especialidad(especialidad)
                .fecha(fechaFutura)
                .horaInicio(LocalTime.of(9, 0))
                .horaFin(LocalTime.of(12, 0))
                .duracionTurnoMinutos(30)
                .build());

        slotA = slotHorarioRepository.save(SlotHorario.builder()
                .doctor(doctorA)
                .especialidad(especialidad)
                .fecha(fechaFutura)
                .horaInicio(LocalTime.of(10, 0))
                .horaFin(LocalTime.of(10, 30))
                .duracionMinutos(30)
                .esPuntual(true)
                .build());

        slotB = slotHorarioRepository.save(SlotHorario.builder()
                .doctor(doctorB)
                .especialidad(especialidad)
                .fecha(fechaFutura)
                .horaInicio(LocalTime.of(9, 0))
                .horaFin(LocalTime.of(9, 30))
                .duracionMinutos(30)
                .esPuntual(true)
                .build());

        plantillaA = PlantillaAgenda.builder()
                .doctor(doctorA)
                .nombre("Plantilla A")
                .descripcion("Agenda A")
                .build();
        plantillaA.setDetalles(new ArrayList<>(List.of(DetallePlantilla.builder()
                .plantilla(plantillaA)
                .especialidad(especialidad)
                .horaInicio(LocalTime.of(14, 0))
                .horaFin(LocalTime.of(15, 0))
                .duracionTurnoMinutos(30)
                .build())));
        plantillaA = plantillaAgendaRepository.save(plantillaA);

        plantillaB = PlantillaAgenda.builder()
                .doctor(doctorB)
                .nombre("Plantilla B")
                .descripcion("Agenda B")
                .build();
        plantillaB.setDetalles(new ArrayList<>(List.of(DetallePlantilla.builder()
                .plantilla(plantillaB)
                .especialidad(especialidad)
                .horaInicio(LocalTime.of(15, 0))
                .horaFin(LocalTime.of(16, 0))
                .duracionTurnoMinutos(30)
                .build())));
        plantillaB = plantillaAgendaRepository.save(plantillaB);

        Usuario usuarioPaciente = usuarioRepository.save(Usuario.builder()
                .email("idor_pac_" + UUID.randomUUID() + "@test.com")
                .password("encoded-password")
                .rol(Rol.PACIENTE)
                .activo(true)
                .emailVerificado(true)
                .build());

        Paciente paciente = pacienteRepository.save(Paciente.builder()
                .usuario(usuarioPaciente)
                .nombre("Carlos")
                .apellido("Paciente")
                .dni(40000000L + (long) (Math.random() * 90000))
                .build());

        LocalDateTime fechaTurnoPasado = LocalDateTime.now().minusHours(2);

        turnoA = turnoRepository.save(Turno.builder()
                .doctor(doctorA)
                .paciente(paciente)
                .especialidad(especialidad)
                .fechaHora(fechaTurnoPasado)
                .estado(EstadoTurno.CONFIRMADO)
                .build());

        turnoB = turnoRepository.save(Turno.builder()
                .doctor(doctorB)
                .paciente(paciente)
                .especialidad(especialidad)
                .fechaHora(fechaTurnoPasado.minusHours(1))
                .estado(EstadoTurno.CONFIRMADO)
                .build());
    }

    @Test
    @DisplayName("BOLA/IDOR: Doctor A cannot mutate Doctor B settings, schedules, slots, templates, or appointment status")
    void doctorACannotAccessOrMutateDoctorBResources() throws Exception {
        String horarioBody = """
                {
                  "fecha": "%s",
                  "horaInicio": "16:00",
                  "horaFin": "17:00",
                  "duracionTurnoMinutos": 30,
                  "especialidadId": %d
                }
                """.formatted(fechaFutura.plusDays(1), especialidad.getId());

        String plantillaBody = """
                {
                  "nombre": "Hijacked Template",
                  "descripcion": "Injected",
                  "detalles": [
                    {
                      "horaInicio": "16:00",
                      "horaFin": "17:00",
                      "duracionTurnoMinutos": 30,
                      "especialidadId": %d
                    }
                  ]
                }
                """.formatted(especialidad.getId());

        String aplicarPlantillaBBody = """
                {
                  "plantillaId": %d,
                  "fecha": "%s"
                }
                """.formatted(plantillaB.getId(), fechaFutura.plusDays(2));

        // 1-3: Doctor profile availability and warning flags
        mockMvc.perform(patch("/api/doctores/" + doctorB.getId() + "/disponibilidad-turnos")
                        .param("disponible", "false")
                        .with(user(usuarioDoctorA.getEmail()).roles("DOCTOR")))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/doctores/" + doctorB.getId() + "/advertencia-bloqueante")
                        .param("activa", "true")
                        .param("mensaje", "Blocked by attacker")
                        .with(user(usuarioDoctorA.getEmail()).roles("DOCTOR")))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/doctores/" + doctorB.getId() + "/advertencia-informativa")
                        .param("activa", "true")
                        .param("mensaje", "Info by attacker")
                        .with(user(usuarioDoctorA.getEmail()).roles("DOCTOR")))
                .andExpect(status().isForbidden());

        // 4-8: Schedules (horarios)
        mockMvc.perform(post("/api/doctores/" + doctorB.getId() + "/horarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(horarioBody)
                        .with(user(usuarioDoctorA.getEmail()).roles("DOCTOR")))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/doctores/horarios/" + horarioB.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(horarioBody)
                        .with(user(usuarioDoctorA.getEmail()).roles("DOCTOR")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/doctores/" + doctorB.getId() + "/horarios")
                        .with(user(usuarioDoctorA.getEmail()).roles("DOCTOR")))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/doctores/horarios/" + horarioB.getId())
                        .with(user(usuarioDoctorA.getEmail()).roles("DOCTOR")))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/doctores/" + doctorB.getId() + "/horarios/semana")
                        .param("desde", fechaFutura.toString())
                        .param("hasta", fechaFutura.plusDays(7).toString())
                        .with(user(usuarioDoctorA.getEmail()).roles("DOCTOR")))
                .andExpect(status().isForbidden());

        // 9-10: Slots
        mockMvc.perform(get("/api/doctores/" + doctorB.getId() + "/slots")
                        .with(user(usuarioDoctorA.getEmail()).roles("DOCTOR")))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/doctores/slots/" + slotB.getId())
                        .with(user(usuarioDoctorA.getEmail()).roles("DOCTOR")))
                .andExpect(status().isForbidden());

        // 11-15: Templates (plantillas) + cross-doctor templateId injection
        mockMvc.perform(post("/api/doctores/" + doctorB.getId() + "/plantillas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(plantillaBody)
                        .with(user(usuarioDoctorA.getEmail()).roles("DOCTOR")))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/doctores/plantillas/" + plantillaB.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(plantillaBody)
                        .with(user(usuarioDoctorA.getEmail()).roles("DOCTOR")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/doctores/" + doctorB.getId() + "/plantillas")
                        .with(user(usuarioDoctorA.getEmail()).roles("DOCTOR")))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/doctores/plantillas/" + plantillaB.getId())
                        .with(user(usuarioDoctorA.getEmail()).roles("DOCTOR")))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/doctores/" + doctorB.getId() + "/aplicar-plantilla")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(aplicarPlantillaBBody)
                        .with(user(usuarioDoctorA.getEmail()).roles("DOCTOR")))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/doctores/" + doctorA.getId() + "/aplicar-plantilla")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(aplicarPlantillaBBody)
                        .with(user(usuarioDoctorA.getEmail()).roles("DOCTOR")))
                .andExpect(status().isForbidden());

        // 16: Appointment status change (TurnoService.cambiarEstadoTurno)
        mockMvc.perform(put("/api/turnos/" + turnoB.getId() + "/estado")
                        .param("nuevoEstado", "COMPLETADO")
                        .with(user(usuarioDoctorA.getEmail()).roles("DOCTOR")))
                .andExpect(status().isForbidden());

        // Assert Doctor B resources remained intact
        assertThat(doctorRepository.findById(doctorB.getId()).orElseThrow().isDisponibleParaTurnos()).isTrue();
        assertThat(horarioAtencionRepository.existsById(horarioB.getId())).isTrue();
        assertThat(slotHorarioRepository.existsById(slotB.getId())).isTrue();
        assertThat(plantillaAgendaRepository.existsById(plantillaB.getId())).isTrue();
        assertThat(turnoRepository.findById(turnoB.getId()).orElseThrow().getEstado()).isEqualTo(EstadoTurno.CONFIRMADO);
    }

    @Test
    @DisplayName("Owner Doctor A, ADMIN, and SECRETARIA (slots) retain legitimate access")
    void legitimateRolesRetainAccess() throws Exception {
        String aplicarPlantillaABody = """
                {
                  "plantillaId": %d,
                  "fecha": "%s"
                }
                """.formatted(plantillaA.getId(), fechaFutura.plusDays(3));

        // Doctor A owns doctorA resources
        mockMvc.perform(patch("/api/doctores/" + doctorA.getId() + "/disponibilidad-turnos")
                        .param("disponible", "false")
                        .with(user(usuarioDoctorA.getEmail()).roles("DOCTOR")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/doctores/" + doctorA.getId() + "/horarios")
                        .with(user(usuarioDoctorA.getEmail()).roles("DOCTOR")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/doctores/" + doctorA.getId() + "/aplicar-plantilla")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(aplicarPlantillaABody)
                        .with(user(usuarioDoctorA.getEmail()).roles("DOCTOR")))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/turnos/" + turnoA.getId() + "/estado")
                        .param("nuevoEstado", "COMPLETADO")
                        .with(user(usuarioDoctorA.getEmail()).roles("DOCTOR")))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/doctores/slots/" + slotA.getId())
                        .with(user(usuarioDoctorA.getEmail()).roles("DOCTOR")))
                .andExpect(status().isNoContent());

        // ADMIN can manage Doctor B's settings and appointment status
        mockMvc.perform(patch("/api/doctores/" + doctorB.getId() + "/disponibilidad-turnos")
                        .param("disponible", "false")
                        .with(user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/turnos/" + turnoB.getId() + "/estado")
                        .param("nuevoEstado", "AUSENTE")
                        .with(user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isOk());

        // SECRETARIA retains DELETE /api/doctores/slots/{slotId} (used by SecretariaAgenda.jsx)
        mockMvc.perform(delete("/api/doctores/slots/" + slotB.getId())
                        .with(user("secretaria@test.com").roles("SECRETARIA")))
                .andExpect(status().isNoContent());
        assertThat(slotHorarioRepository.existsById(slotB.getId())).isFalse();
    }

    @Test
    void postIndividualSlotAllowsSecretariaAdminAndOwnerDoctorButBlocksIdorAndPaciente() throws Exception {
        LocalDate futureDate = LocalDate.now().plusDays(12);
        String secretariaBody = """
                {
                  "fecha": "%s",
                  "horaInicio": "15:00",
                  "duracionMinutos": 30
                }
                """.formatted(futureDate);
        String adminBody = """
                {
                  "fecha": "%s",
                  "horaInicio": "16:00",
                  "duracionMinutos": 30
                }
                """.formatted(futureDate);
        String ownerDoctorBody = """
                {
                  "fecha": "%s",
                  "horaInicio": "17:00",
                  "duracionMinutos": 30
                }
                """.formatted(futureDate);

        // Anonymous -> 401
        mockMvc.perform(post("/api/doctores/" + doctorB.getId() + "/slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(secretariaBody))
                .andExpect(status().isUnauthorized());

        // PACIENTE -> 403
        mockMvc.perform(post("/api/doctores/" + doctorB.getId() + "/slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(secretariaBody)
                        .with(user("paciente@test.com").roles("PACIENTE")))
                .andExpect(status().isForbidden());

        // Doctor A attempting to create a slot for Doctor B -> 403 (IDOR blocked)
        mockMvc.perform(post("/api/doctores/" + doctorB.getId() + "/slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(secretariaBody)
                        .with(user(usuarioDoctorA.getEmail()).roles("DOCTOR")))
                .andExpect(status().isForbidden());

        // Doctor A attempting to create a slot for a nonexistent doctor -> 403 (fail-closed)
        mockMvc.perform(post("/api/doctores/" + UUID.randomUUID() + "/slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(secretariaBody)
                        .with(user(usuarioDoctorA.getEmail()).roles("DOCTOR")))
                .andExpect(status().isForbidden());

        // SECRETARIA can create a punctual slot for Doctor B -> 201
        mockMvc.perform(post("/api/doctores/" + doctorB.getId() + "/slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(secretariaBody)
                        .with(user("secretaria@test.com").roles("SECRETARIA")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fecha").value(futureDate.toString()))
                .andExpect(jsonPath("$.horaInicio").value("15:00:00"))
                .andExpect(jsonPath("$.horaFin").value("15:30:00"))
                .andExpect(jsonPath("$.esPuntual").value(true));

        // Duplicate / colliding slot at the same time -> 409 Conflict
        mockMvc.perform(post("/api/doctores/" + doctorB.getId() + "/slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(secretariaBody)
                        .with(user("secretaria@test.com").roles("SECRETARIA")))
                .andExpect(status().isConflict());

        // ADMIN can create a punctual slot for Doctor B -> 201
        mockMvc.perform(post("/api/doctores/" + doctorB.getId() + "/slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(adminBody)
                        .with(user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isCreated());

        // Owner Doctor A can create a punctual slot for Doctor A -> 201
        mockMvc.perform(post("/api/doctores/" + doctorA.getId() + "/slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ownerDoctorBody)
                        .with(user(usuarioDoctorA.getEmail()).roles("DOCTOR")))
                .andExpect(status().isCreated());

        // Real DB half-open interval check against orphaned active Turno at 10:00:
        // - Back-to-back 09:30-10:00 MUST succeed (201) despite TIMESTAMP(6) precision
        // - Overlapping 09:45-10:15 MUST be rejected (409)
        LocalDate turnoDate = LocalDate.now().plusDays(15);
        turnoRepository.save(Turno.builder()
                .doctor(doctorB)
                .paciente(turnoB.getPaciente())
                .especialidad(especialidad)
                .fechaHora(LocalDateTime.of(turnoDate, LocalTime.of(10, 0)))
                .estado(EstadoTurno.CONFIRMADO)
                .build());

        String overlappingTurnoBody = """
                {
                  "fecha": "%s",
                  "horaInicio": "09:45",
                  "duracionMinutos": 30
                }
                """.formatted(turnoDate);
        String adjacentBackToBackBody = """
                {
                  "fecha": "%s",
                  "horaInicio": "09:30",
                  "duracionMinutos": 30
                }
                """.formatted(turnoDate);

        mockMvc.perform(post("/api/doctores/" + doctorB.getId() + "/slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(overlappingTurnoBody)
                        .with(user("secretaria@test.com").roles("SECRETARIA")))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/doctores/" + doctorB.getId() + "/slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(adjacentBackToBackBody)
                        .with(user("secretaria@test.com").roles("SECRETARIA")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.horaInicio").value("09:30:00"))
                .andExpect(jsonPath("$.horaFin").value("10:00:00"));
    }
}
