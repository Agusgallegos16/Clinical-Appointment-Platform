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
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

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

    @SpyBean
    private EmailService emailService;

    private String emailAdmin;
    private String emailDoctor;
    private LocalDate fechaManana;

    @BeforeEach
    void setUp() {
        emailAdmin = "admin_config_" + UUID.randomUUID() + "@test.com";
        emailDoctor = "doctor_config_" + UUID.randomUUID() + "@test.com";
        fechaManana = LocalDate.now().plusDays(1);

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
    }

    @Test
    @DisplayName("Scheduler omite envío de resumen diario a médicos si el switch diario está deshabilitado")
    void testSchedulerOmiteResumenDiarioCuandoDeshabilitado() {
        // Deshabilitar diario
        configuracionSistemaService.actualizarNotificacionesConfig(
                ActualizarNotificacionesConfigDTO.builder().resumenDiarioMedicosHabilitado(false).build(),
                emailAdmin
        );

        notificacionProgramadaService.enviarResumenDiarioADoctoresParaFecha(fechaManana);

        // emailService no debe haber sido llamado
        verify(emailService, never()).enviarResumenDiarioDoctor(anyString(), anyString(), anyString(), anyList());
    }

    @Test
    @DisplayName("Scheduler despacha resumen diario a médicos cuando el switch diario está habilitado")
    void testSchedulerDespachaResumenDiarioCuandoHabilitado() {
        // Habilitar diario
        configuracionSistemaService.actualizarNotificacionesConfig(
                ActualizarNotificacionesConfigDTO.builder().resumenDiarioMedicosHabilitado(true).build(),
                emailAdmin
        );

        notificacionProgramadaService.enviarResumenDiarioADoctoresParaFecha(fechaManana);

        // emailService debe haber sido invocado para el doctor con turnos
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
        // Deshabilitar semanal
        configuracionSistemaService.actualizarNotificacionesConfig(
                ActualizarNotificacionesConfigDTO.builder().resumenSemanalMedicosHabilitado(false).build(),
                emailAdmin
        );

        LocalDate desde = LocalDate.now();
        LocalDate hasta = LocalDate.now().plusDays(6);

        notificacionProgramadaService.enviarResumenSemanalADoctoresParaRango(desde, hasta);

        // emailService no debe haber sido llamado
        verify(emailService, never()).enviarResumenSemanalDoctor(anyString(), anyString(), anyString(), anyList());
    }
}
