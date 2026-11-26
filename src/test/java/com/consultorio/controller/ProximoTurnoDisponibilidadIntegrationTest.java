package com.consultorio.controller;

import com.consultorio.domain.AuthProvider;
import com.consultorio.domain.Doctor;
import com.consultorio.domain.Especialidad;
import com.consultorio.domain.EstadoTurno;
import com.consultorio.domain.Paciente;
import com.consultorio.domain.Rol;
import com.consultorio.domain.SlotHorario;
import com.consultorio.domain.Turno;
import com.consultorio.domain.Usuario;
import com.consultorio.repository.DoctorRepository;
import com.consultorio.repository.EspecialidadRepository;
import com.consultorio.repository.PacienteRepository;
import com.consultorio.repository.SlotHorarioRepository;
import com.consultorio.repository.TurnoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Set;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "app.jwt.secret=dGVzdC1qd3Qtc2VjcmV0LWtleS1mb3ItaW50ZWdyYXRpb24tdGVzdHMtMzItYnl0ZXM=",
        "app.mail.console-mode=true",
        "app.dev.seed-test-users=false"
})
@Transactional
class ProximoTurnoDisponibilidadIntegrationTest {

    @TestConfiguration
    static class FixedClockTestConfig {
        @Bean
        @Primary
        Clock testClinicClock() {
            // 2026-12-20 10:00:00 -03:00
            return Clock.fixed(
                    Instant.parse("2026-12-20T13:00:00Z"),
                    ZoneId.of("America/Argentina/Buenos_Aires")
            );
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EspecialidadRepository especialidadRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private PacienteRepository pacienteRepository;

    @Autowired
    private SlotHorarioRepository slotHorarioRepository;

    @Autowired
    private TurnoRepository turnoRepository;

    private Especialidad cardiologia;
    private Doctor doctor;
    private Paciente paciente;

    @BeforeEach
    void setUp() {
        cardiologia = especialidadRepository.save(Especialidad.builder()
                .nombre("Cardiología Test")
                .descripcion("Especialidad de prueba")
                .build());

        Usuario usuarioDoctor = Usuario.builder()
                .email("doc.proximo@test.com")
                .password("encoded-pass")
                .rol(Rol.DOCTOR)
                .activo(true)
                .emailVerificado(true)
                .authProvider(AuthProvider.LOCAL)
                .build();

        doctor = doctorRepository.save(Doctor.builder()
                .usuario(usuarioDoctor)
                .nombre("Laura")
                .apellido("Gómez")
                .especialidades(Set.of(cardiologia))
                .disponibleParaTurnos(true)
                .build());

        paciente = pacienteRepository.save(Paciente.builder()
                .nombre("Juan")
                .apellido("Pérez")
                .dni(30111222L)
                .telefono("1122334455")
                .email("juan.perez@test.com")
                .fechaNacimiento(LocalDate.of(1990, 5, 10))
                .build());
    }

    @Test
    @DisplayName("GET /disponibilidad/proximo retorna 200 OK con el 31/12 08:30 ignorando horas pasadas y turnos cancelados")
    void obtenerProximoTurnoDisponible_retorna200ConPrimerSlotLibre() throws Exception {
        LocalDate dic20 = LocalDate.of(2026, 12, 20);
        LocalDate dic30 = LocalDate.of(2026, 12, 30);
        LocalDate dic31 = LocalDate.of(2026, 12, 31);

        // Slot pasado el mismo 20/12 (09:00 < 10:00)
        crearSlot(dic20, LocalTime.of(9, 0));
        // Slots ocupados del 20/12 al 31/12 08:00
        crearSlot(dic20, LocalTime.of(11, 0));
        crearSlot(dic30, LocalTime.of(9, 0));
        crearSlot(dic31, LocalTime.of(8, 0));
        // Slot objetivo libre (incluso con un turno CANCELADO en esa fecha/hora)
        SlotHorario slotObjetivo = crearSlot(dic31, LocalTime.of(8, 30));

        crearTurno(LocalDateTime.of(dic20, LocalTime.of(11, 0)), EstadoTurno.CONFIRMADO);
        crearTurno(LocalDateTime.of(dic30, LocalTime.of(9, 0)), EstadoTurno.PENDIENTE);
        crearTurno(LocalDateTime.of(dic31, LocalTime.of(8, 0)), EstadoTurno.CONFIRMADO);
        crearTurno(LocalDateTime.of(dic31, LocalTime.of(8, 30)), EstadoTurno.CANCELADO);

        mockMvc.perform(get("/api/doctores/{id}/disponibilidad/proximo", doctor.getId())
                        .param("especialidadId", cardiologia.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slotId").value(slotObjetivo.getId()))
                .andExpect(jsonPath("$.fecha").value("2026-12-31"))
                .andExpect(jsonPath("$.hora").value("08:30:00"))
                .andExpect(jsonPath("$.especialidadId").value(cardiologia.getId()))
                .andExpect(jsonPath("$.especialidadNombre").value("Cardiología Test"));
    }

    @Test
    @DisplayName("GET /disponibilidad/proximo retorna 204 No Content cuando no hay turnos disponibles")
    void obtenerProximoTurnoDisponible_retorna204CuandoNoHayDisponibilidad() throws Exception {
        mockMvc.perform(get("/api/doctores/{id}/disponibilidad/proximo", doctor.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Regresión: GET /disponibilidad por fecha sigue siendo público y retornando slots disponibles")
    void obtenerDisponibilidadPorFecha_sigueFuncionando() throws Exception {
        LocalDate dic31 = LocalDate.of(2026, 12, 31);
        crearSlot(dic31, LocalTime.of(8, 30));

        mockMvc.perform(get("/api/doctores/{id}/disponibilidad", doctor.getId())
                        .param("fecha", "2026-12-31")
                        .param("especialidadId", cardiologia.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].hora").value("08:30:00"));
    }

    @Test
    @DisplayName("Seguridad: GET /agenda sigue requiriendo autenticación tras habilitar /disponibilidad/proximo")
    void obtenerAgendaPrivada_sigueRequiriendoAutenticacion() throws Exception {
        mockMvc.perform(get("/api/doctores/{id}/agenda", doctor.getId())
                        .param("fecha", "2026-12-31"))
                .andExpect(status().isUnauthorized());
    }

    private SlotHorario crearSlot(LocalDate fecha, LocalTime horaInicio) {
        return slotHorarioRepository.save(SlotHorario.builder()
                .doctor(doctor)
                .especialidad(cardiologia)
                .fecha(fecha)
                .horaInicio(horaInicio)
                .horaFin(horaInicio.plusMinutes(30))
                .duracionMinutos(30)
                .build());
    }

    private void crearTurno(LocalDateTime fechaHora, EstadoTurno estado) {
        turnoRepository.save(Turno.builder()
                .doctor(doctor)
                .paciente(paciente)
                .especialidad(cardiologia)
                .fechaHora(fechaHora)
                .estado(estado)
                .build());
    }
}
