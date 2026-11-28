package com.consultorio.service;

import com.consultorio.domain.Doctor;
import com.consultorio.domain.Especialidad;
import com.consultorio.domain.EstadoTurno;
import com.consultorio.domain.SlotHorario;
import com.consultorio.dto.CrearSlotIndividualDTO;
import com.consultorio.repository.DoctorRepository;
import com.consultorio.repository.HorarioAtencionRepository;
import com.consultorio.repository.SlotHorarioRepository;
import com.consultorio.repository.TurnoRepository;
import com.consultorio.security.SecurityUtils;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DoctorAgendaServiceSlotIndividualTest {

    private static final ZoneId CLINIC_ZONE = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final Clock FIXED_CLOCK = Clock.fixed(
            LocalDateTime.of(2026, 11, 20, 10, 0).atZone(CLINIC_ZONE).toInstant(),
            CLINIC_ZONE
    );

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @Mock
    private HorarioAtencionRepository horarioAtencionRepository;
    @Mock
    private SlotHorarioRepository slotHorarioRepository;
    @Mock
    private DoctorRepository doctorRepository;
    @Mock
    private TurnoRepository turnoRepository;
    @Mock
    private EspecialidadService especialidadService;
    @Mock
    private SecurityUtils securityUtils;
    @Mock
    private AuditLogService auditLogService;

    private DoctorAgendaService doctorAgendaService;

    private UUID doctorId;
    private Especialidad cardiologia;
    private Doctor doctor;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void tearDownValidator() {
        validatorFactory.close();
    }

    @BeforeEach
    void setUp() {
        doctorAgendaService = new DoctorAgendaService(
                horarioAtencionRepository,
                slotHorarioRepository,
                doctorRepository,
                turnoRepository,
                especialidadService,
                securityUtils,
                auditLogService,
                FIXED_CLOCK
        );

        doctorId = UUID.randomUUID();
        cardiologia = Especialidad.builder().id(1L).nombre("Cardiologia").build();
        doctor = Doctor.builder()
                .id(doctorId)
                .nombre("Gregory")
                .apellido("House")
                .disponibleParaTurnos(true)
                .especialidades(Set.of(cardiologia))
                .build();
    }

    private void stubDoctorFound() {
        when(doctorRepository.findById(doctorId)).thenReturn(Optional.of(doctor));
        when(doctorRepository.findByIdWithLock(doctorId)).thenReturn(Optional.of(doctor));
    }

    @Test
    void createsPunctualSlotWithDefaultDurationChecksAuthBeforeLockAndAudits() {
        CrearSlotIndividualDTO dto = CrearSlotIndividualDTO.builder()
                .fecha(LocalDate.of(2026, 11, 20))
                .horaInicio(LocalTime.of(14, 0))
                .build();

        stubDoctorFound();
        when(slotHorarioRepository.findByDoctorIdAndFecha(doctorId, dto.getFecha())).thenReturn(Collections.emptyList());
        when(turnoRepository.existsByDoctorIdAndFechaHoraGreaterThanEqualAndFechaHoraLessThanAndEstadoNot(
                doctorId,
                LocalDateTime.of(2026, 11, 20, 14, 0),
                LocalDateTime.of(2026, 11, 20, 14, 30),
                EstadoTurno.CANCELADO))
                .thenReturn(false);
        when(slotHorarioRepository.save(any(SlotHorario.class))).thenAnswer(inv -> {
            SlotHorario s = inv.getArgument(0);
            s.setId(77L);
            return s;
        });
        when(securityUtils.obtenerIdUsuarioAutenticado()).thenReturn(9L);
        when(securityUtils.esSecretaria()).thenReturn(true);

        SlotHorario created = doctorAgendaService.crearSlotIndividual(doctorId, dto);

        assertEquals(77L, created.getId());
        assertTrue(created.isEsPuntual());
        assertEquals(30, created.getDuracionMinutos());
        assertEquals(LocalTime.of(14, 0), created.getHoraInicio());
        assertEquals(LocalTime.of(14, 30), created.getHoraFin());
        assertNull(created.getEspecialidad());

        InOrder inOrder = inOrder(securityUtils, doctorRepository);
        inOrder.verify(securityUtils).verificarAccesoDoctor(doctor, true);
        inOrder.verify(doctorRepository).findByIdWithLock(doctorId);
        verify(auditLogService).record(eq("SLOT_CREATED"), eq("SUCCESS"), eq(9L), eq("SECRETARIA"), eq("SLOT"), eq("77"), contains("fecha=2026-11-20"));
    }

    @Test
    void createsPunctualSlotWithDoctorOwnedSpecialty() {
        CrearSlotIndividualDTO dto = CrearSlotIndividualDTO.builder()
                .fecha(LocalDate.of(2026, 11, 21))
                .horaInicio(LocalTime.of(9, 0))
                .duracionMinutos(45)
                .especialidadId(1L)
                .build();

        stubDoctorFound();
        when(especialidadService.buscarPorId(1L)).thenReturn(Optional.of(cardiologia));
        when(slotHorarioRepository.findByDoctorIdAndFecha(doctorId, dto.getFecha())).thenReturn(Collections.emptyList());
        when(turnoRepository.existsByDoctorIdAndFechaHoraGreaterThanEqualAndFechaHoraLessThanAndEstadoNot(
                eq(doctorId), any(), any(), eq(EstadoTurno.CANCELADO)))
                .thenReturn(false);
        when(slotHorarioRepository.save(any(SlotHorario.class))).thenAnswer(inv -> inv.getArgument(0));

        SlotHorario created = doctorAgendaService.crearSlotIndividual(doctorId, dto);

        assertEquals(cardiologia, created.getEspecialidad());
        assertEquals(LocalTime.of(9, 45), created.getHoraFin());
        assertEquals(45, created.getDuracionMinutos());
    }

    @Test
    void rejectsNonZeroSecondsInHoraInicio() {
        CrearSlotIndividualDTO dto = CrearSlotIndividualDTO.builder()
                .fecha(LocalDate.of(2026, 11, 21))
                .horaInicio(LocalTime.of(15, 0, 30))
                .duracionMinutos(30)
                .build();

        stubDoctorFound();

        assertThrows(IllegalArgumentException.class, () -> doctorAgendaService.crearSlotIndividual(doctorId, dto));
        verify(slotHorarioRepository, never()).save(any());
    }

    @Test
    void rejectsPastDateOrTimeBasedOnClinicClock() {
        CrearSlotIndividualDTO pastSameDay = CrearSlotIndividualDTO.builder()
                .fecha(LocalDate.of(2026, 11, 20))
                .horaInicio(LocalTime.of(9, 30))
                .duracionMinutos(30)
                .build();

        stubDoctorFound();

        assertThrows(IllegalArgumentException.class, () -> doctorAgendaService.crearSlotIndividual(doctorId, pastSameDay));
        verify(slotHorarioRepository, never()).save(any());
    }

    @Test
    void rejectsHiddenDoctorUnavailableForAppointments() {
        doctor.setDisponibleParaTurnos(false);
        CrearSlotIndividualDTO dto = CrearSlotIndividualDTO.builder()
                .fecha(LocalDate.of(2026, 11, 21))
                .horaInicio(LocalTime.of(10, 0))
                .duracionMinutos(30)
                .build();

        stubDoctorFound();

        assertThrows(IllegalStateException.class, () -> doctorAgendaService.crearSlotIndividual(doctorId, dto));
        verify(slotHorarioRepository, never()).save(any());
    }

    @Test
    void rejectsMidnightWrap() {
        CrearSlotIndividualDTO dto = CrearSlotIndividualDTO.builder()
                .fecha(LocalDate.of(2026, 11, 21))
                .horaInicio(LocalTime.of(23, 45))
                .duracionMinutos(30)
                .build();

        stubDoctorFound();

        assertThrows(IllegalArgumentException.class, () -> doctorAgendaService.crearSlotIndividual(doctorId, dto));
        verify(slotHorarioRepository, never()).save(any());
    }

    @Test
    void rejectsSpecialtyNotOwnedByDoctor() {
        Especialidad dermatologia = Especialidad.builder().id(2L).nombre("Dermatologia").build();
        CrearSlotIndividualDTO dto = CrearSlotIndividualDTO.builder()
                .fecha(LocalDate.of(2026, 11, 21))
                .horaInicio(LocalTime.of(11, 0))
                .duracionMinutos(30)
                .especialidadId(2L)
                .build();

        stubDoctorFound();
        when(especialidadService.buscarPorId(2L)).thenReturn(Optional.of(dermatologia));

        assertThrows(IllegalArgumentException.class, () -> doctorAgendaService.crearSlotIndividual(doctorId, dto));
        verify(slotHorarioRepository, never()).save(any());
    }

    @Test
    void rejectsPartialOverlapWithExistingSlot() {
        SlotHorario existente = SlotHorario.builder()
                .id(10L)
                .doctor(doctor)
                .fecha(LocalDate.of(2026, 11, 21))
                .horaInicio(LocalTime.of(10, 0))
                .horaFin(LocalTime.of(10, 30))
                .duracionMinutos(30)
                .build();

        CrearSlotIndividualDTO dto = CrearSlotIndividualDTO.builder()
                .fecha(LocalDate.of(2026, 11, 21))
                .horaInicio(LocalTime.of(10, 15))
                .duracionMinutos(30)
                .build();

        stubDoctorFound();
        when(slotHorarioRepository.findByDoctorIdAndFecha(doctorId, dto.getFecha())).thenReturn(List.of(existente));

        assertThrows(IllegalStateException.class, () -> doctorAgendaService.crearSlotIndividual(doctorId, dto));
        verify(slotHorarioRepository, never()).save(any());
    }

    @Test
    void rejectsRangeOverlapWithOrphanedActiveTurnoInsideSlotWindow() {
        CrearSlotIndividualDTO dto = CrearSlotIndividualDTO.builder()
                .fecha(LocalDate.of(2026, 11, 21))
                .horaInicio(LocalTime.of(9, 45))
                .duracionMinutos(30)
                .build();

        stubDoctorFound();
        when(slotHorarioRepository.findByDoctorIdAndFecha(doctorId, dto.getFecha())).thenReturn(Collections.emptyList());
        when(turnoRepository.existsByDoctorIdAndFechaHoraGreaterThanEqualAndFechaHoraLessThanAndEstadoNot(
                doctorId,
                LocalDateTime.of(2026, 11, 21, 9, 45),
                LocalDateTime.of(2026, 11, 21, 10, 15),
                EstadoTurno.CANCELADO))
                .thenReturn(true);

        assertThrows(IllegalStateException.class, () -> doctorAgendaService.crearSlotIndividual(doctorId, dto));
        verify(slotHorarioRepository, never()).save(any());
    }

    @Test
    void dtoValidationRejectsMissingFieldsAndOutOfBoundsDuration() {
        CrearSlotIndividualDTO invalid = CrearSlotIndividualDTO.builder()
                .fecha(null)
                .horaInicio(null)
                .duracionMinutos(2)
                .build();

        Set<ConstraintViolation<CrearSlotIndividualDTO>> violations = validator.validate(invalid);
        assertTrue(violations.stream().anyMatch(v -> "fecha".equals(v.getPropertyPath().toString())));
        assertTrue(violations.stream().anyMatch(v -> "horaInicio".equals(v.getPropertyPath().toString())));
        assertTrue(violations.stream().anyMatch(v -> "duracionMinutos".equals(v.getPropertyPath().toString())));
    }
}
