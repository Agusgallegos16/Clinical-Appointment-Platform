package com.consultorio.service;

import com.consultorio.domain.*;
import com.consultorio.dto.ActualizarNotificacionesConfigDTO;
import com.consultorio.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminConfiguracionSchedulerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ConfiguracionSistemaService configuracionSistemaService;

    @Autowired
    private ConfiguracionSistemaRepository configuracionSistemaRepository;

    @Autowired
    private NotificacionProgramadaService notificacionProgramadaService;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private PacienteRepository pacienteRepository;

    @Autowired
    private TurnoRepository turnoRepository;

    @Autowired
    private EspecialidadRepository especialidadRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private Clock clock;

    @SpyBean
    private EmailService emailService;

    private String emailAdmin;
    private String emailDoctor;
    private LocalDate fechaHoy;
    private LocalDate fechaManana;

    @BeforeEach
    void setUp() {
        emailAdmin = "admin_config_" + UUID.randomUUID() + "@test.com";
        emailDoctor = "doctor_config_" + UUID.randomUUID() + "@test.com";
        fechaHoy = LocalDate.now(clock);
        fechaManana = fechaHoy.plusDays(1);

        // Crear Doctor con turno mañana
        Usuario usuarioDoctor = Usuario.builder()
                .email(emailDoctor)
                .password("password123")
                .rol(Rol.DOCTOR)
                .activo(true)
                .emailVerificado(true)
                .build();

        Especialidad especialidad = especialidadRepository.save(Especialidad.builder()
                .nombre("Pediatria Config " + UUID.randomUUID())
                .descripcion("Especialidad prueba")
                .build());

        Doctor doctor = doctorRepository.save(Doctor.builder()
                .usuario(usuarioDoctor)
                .nombre("Claudio")
                .apellido("Borghi")
                .disponibleParaTurnos(true)
                .especialidades(Set.of(especialidad))
                .build());

        Paciente paciente = pacienteRepository.save(Paciente.builder()
                .nombre("Facundo")
                .apellido("Quiroga")
                .dni(10000000L + (long)(Math.random() * 80000000L))
                .telefono("1122334455")
                .fechaNacimiento(LocalDate.of(2000, 1, 1))
                .build());

        turnoRepository.save(Turno.builder()
                .paciente(paciente)
                .doctor(doctor)
                .especialidad(especialidad)
                .fechaHora(fechaManana.atTime(10, 0))
                .estado(EstadoTurno.CONFIRMADO)
                .motivoConsulta("Control pediátrico")
                .build());

        turnoRepository.save(Turno.builder()
                .paciente(paciente)
                .doctor(doctor)
                .especialidad(especialidad)
                .fechaHora(fechaHoy.atTime(11, 0))
                .estado(EstadoTurno.CONFIRMADO)
                .motivoConsulta("Control semanal")
                .build());

        doNothing().when(emailService).enviarResumenDiarioDoctor(anyString(), anyString(), anyString(), anyList());
        doNothing().when(emailService).enviarResumenSemanalDoctor(anyString(), anyString(), anyString(), anyList());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GET /api/admin/configuracion/notificaciones sin autenticación retorna 401 Unauthorized")
    void testGetConfiguracionSinAutenticacion() throws Exception {
        mockMvc.perform(get("/api/admin/configuracion/notificaciones"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/admin/configuracion/notificaciones con rol PACIENTE retorna 403 Forbidden")
    void testGetConfiguracionComoPacienteRetorna403() throws Exception {
        mockMvc.perform(get("/api/admin/configuracion/notificaciones")
                        .with(user("paciente@test.com").roles("PACIENTE")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/admin/configuracion/notificaciones como ADMIN retorna estado 200 OK con configuración")
    void testGetConfiguracionComoAdminRetornaOk() throws Exception {
        mockMvc.perform(get("/api/admin/configuracion/notificaciones")
                        .with(user(emailAdmin).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resumenDiarioMedicosHabilitado", notNullValue()))
                .andExpect(jsonPath("$.resumenSemanalMedicosHabilitado", notNullValue()));
    }

    @Test
    @DisplayName("PATCH /api/admin/configuracion/notificaciones actualiza los switches y persiste en BD")
    void testActualizarConfiguracionSwitches() throws Exception {
        ActualizarNotificacionesConfigDTO updateDTO = ActualizarNotificacionesConfigDTO.builder()
                .resumenDiarioMedicosHabilitado(false)
                .resumenSemanalMedicosHabilitado(false)
                .build();

        mockMvc.perform(patch("/api/admin/configuracion/notificaciones")
                        .with(user(emailAdmin).roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resumenDiarioMedicosHabilitado", is(false)))
                .andExpect(jsonPath("$.resumenSemanalMedicosHabilitado", is(false)))
                .andExpect(jsonPath("$.modificadoPor", is(emailAdmin)));

        assertFalse(configuracionSistemaService.isResumenDiarioMedicosHabilitado());
        assertFalse(configuracionSistemaService.isResumenSemanalMedicosHabilitado());
        assertFalse(auditLogRepository
                .findByEventTypeOrderByOccurredAtDescIdDesc("NOTIFICATION_CONFIG_UPDATED", PageRequest.of(0, 10))
                .isEmpty());
    }

    @Test
    @DisplayName("PATCH /api/admin/configuracion/notificaciones vacío retorna 400 Bad Request")
    void testActualizarConfiguracionVaciaRetorna400() throws Exception {
        mockMvc.perform(patch("/api/admin/configuracion/notificaciones")
                        .with(user(emailAdmin).roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Scheduler omite envío de resumen diario a médicos si el switch diario está deshabilitado")
    void testSchedulerOmiteResumenDiarioCuandoDeshabilitado() {
        configuracionSistemaService.actualizarNotificacionesConfig(
                ActualizarNotificacionesConfigDTO.builder().resumenDiarioMedicosHabilitado(false).build(),
                emailAdmin
        );

        notificacionProgramadaService.enviarResumenDiarioADoctores();

        verify(emailService, never()).enviarResumenDiarioDoctor(anyString(), anyString(), anyString(), anyList());
    }

    @Test
    @DisplayName("Scheduler despacha resumen diario a médicos cuando el switch diario está habilitado")
    void testSchedulerDespachaResumenDiarioCuandoHabilitado() {
        configuracionSistemaService.actualizarNotificacionesConfig(
                ActualizarNotificacionesConfigDTO.builder().resumenDiarioMedicosHabilitado(true).build(),
                emailAdmin
        );

        notificacionProgramadaService.enviarResumenDiarioADoctores();

        verify(emailService, atLeastOnce()).enviarResumenDiarioDoctor(
                eq(emailDoctor),
                contains("Borghi"),
                anyString(),
                anyList()
        );
    }

    @Test
    @DisplayName("Disparo manual de resumen diario funciona aunque el cron automático esté deshabilitado")
    void testEjecucionManualDiariaFuncionaConCronDeshabilitado() throws Exception {
        configuracionSistemaService.actualizarNotificacionesConfig(
                ActualizarNotificacionesConfigDTO.builder().resumenDiarioMedicosHabilitado(false).build(),
                emailAdmin
        );

        mockMvc.perform(post("/api/doctores/ejecutar-resumen-diario")
                        .param("fecha", fechaManana.toString())
                        .with(user(emailAdmin).roles("ADMIN")))
                .andExpect(status().isOk());

        verify(emailService, atLeastOnce()).enviarResumenDiarioDoctor(
                eq(emailDoctor),
                contains("Borghi"),
                anyString(),
                anyList()
        );
    }

    @Test
    @DisplayName("Scheduler omite reporte semanal a médicos si el switch semanal está deshabilitado")
    void testSchedulerOmiteResumenSemanalCuandoDeshabilitado() {
        configuracionSistemaService.actualizarNotificacionesConfig(
                ActualizarNotificacionesConfigDTO.builder().resumenSemanalMedicosHabilitado(false).build(),
                emailAdmin
        );

        notificacionProgramadaService.enviarResumenSemanalADoctores();

        verify(emailService, never()).enviarResumenSemanalDoctor(anyString(), anyString(), anyString(), anyList());
    }

    @Test
    @DisplayName("Scheduler despacha reporte semanal cuando el switch semanal está habilitado")
    void testSchedulerDespachaResumenSemanalCuandoHabilitado() {
        configuracionSistemaService.actualizarNotificacionesConfig(
                ActualizarNotificacionesConfigDTO.builder().resumenSemanalMedicosHabilitado(true).build(),
                emailAdmin
        );

        notificacionProgramadaService.enviarResumenSemanalADoctores();

        verify(emailService, atLeastOnce()).enviarResumenSemanalDoctor(
                eq(emailDoctor),
                contains("Borghi"),
                anyString(),
                anyList()
        );
    }

    @Test
    @DisplayName("Disparo manual de reporte semanal funciona aunque el cron automático esté deshabilitado")
    void testEjecucionManualSemanalFuncionaConCronDeshabilitado() throws Exception {
        configuracionSistemaService.actualizarNotificacionesConfig(
                ActualizarNotificacionesConfigDTO.builder().resumenSemanalMedicosHabilitado(false).build(),
                emailAdmin
        );

        mockMvc.perform(post("/api/doctores/ejecutar-resumen-semanal")
                        .param("desde", fechaHoy.toString())
                        .param("hasta", fechaManana.toString())
                        .with(user(emailAdmin).roles("ADMIN")))
                .andExpect(status().isOk());

        verify(emailService, atLeastOnce()).enviarResumenSemanalDoctor(
                eq(emailDoctor),
                contains("Borghi"),
                anyString(),
                anyList()
        );
    }
}
