package com.consultorio.service;

import com.consultorio.adapter.CalendarioAdapter;
import com.consultorio.domain.*;
import com.consultorio.dto.TurnoReservaSecretariaDTO;
import com.consultorio.dto.TurnoResponseDTO;
import com.consultorio.repository.*;
import com.consultorio.security.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TurnoServiceSecretariaTest {

    @Mock
    private TurnoRepository turnoRepository;
    @Mock
    private PacienteRepository pacienteRepository;
    @Mock
    private DoctorRepository doctorRepository;
    @Mock
    private EspecialidadRepository especialidadRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private SecurityUtils securityUtils;
    @Mock
    private CalendarioAdapter calendarioAdapter;
    @Mock
    private SlotHorarioRepository slotHorarioRepository;

    @InjectMocks
    private TurnoService turnoService;

    private Doctor doctorMock;
    private Especialidad especialidadMock;
    private UUID doctorId;
    private Long especialidadId;

    @BeforeEach
    void setUp() {
        doctorId = UUID.randomUUID();
        especialidadId = 1L;

        especialidadMock = Especialidad.builder()
                .id(especialidadId)
                .nombre("Cardiología")
                .build();

        Usuario usuarioDoctor = Usuario.builder()
                .id(100L)
                .email("doctor@consultorio.com")
                .rol(Rol.DOCTOR)
                .build();

        doctorMock = Doctor.builder()
                .id(doctorId)
                .nombre("Gregory")
                .apellido("House")
                .usuario(usuarioDoctor)
                .especialidades(Collections.singletonList(especialidadMock))
                .disponibleParaTurnos(true)
                .build();

        lenient().when(slotHorarioRepository.existsByDoctorIdAndFechaAndHoraInicio(any(), any(), any())).thenReturn(true);
    }

    @Test
    @DisplayName("Escenario 1: Agendar turno por secretaria para paciente NO registrado crea nuevo Paciente sin usuario")
    void testReservarTurnoSecretaria_PacienteNoRegistrado_CreaNuevoPaciente() {
        Long dni = 12345678L;
        LocalDateTime fechaFutura = LocalDateTime.now().plusDays(2);

        TurnoReservaSecretariaDTO dto = TurnoReservaSecretariaDTO.builder()
                .doctorId(doctorId)
                .especialidadId(especialidadId)
                .fechaHora(fechaFutura)
                .nombre("Juan")
                .apellido("Pérez")
                .dni(dni)
                .email("juan.perez@example.com")
                .telefono("1122334455")
                .fechaNacimiento(LocalDate.of(1990, 5, 15))
                .tieneObraSocial(true)
                .obraSocial("OSDE")
                .motivoConsulta("Consulta general")
                .build();

        when(doctorRepository.findById(doctorId)).thenReturn(Optional.of(doctorMock));
        when(especialidadRepository.findById(especialidadId)).thenReturn(Optional.of(especialidadMock));
        when(pacienteRepository.findByDni(dni)).thenReturn(Optional.empty());
        when(turnoRepository.existsByDoctorIdAndFechaHoraAndEstadoNot(eq(doctorId), eq(fechaFutura), any())).thenReturn(false);

        Paciente pacienteGuardado = Paciente.builder()
                .id(UUID.randomUUID())
                .nombre(dto.getNombre())
                .apellido(dto.getApellido())
                .dni(dto.getDni())
                .telefono(dto.getTelefono())
                .fechaNacimiento(dto.getFechaNacimiento())
                .usuario(null)
                .build();

        when(pacienteRepository.save(any(Paciente.class))).thenReturn(pacienteGuardado);

        Turno turnoGuardado = Turno.builder()
                .id(UUID.randomUUID())
                .paciente(pacienteGuardado)
                .doctor(doctorMock)
                .especialidad(especialidadMock)
                .fechaHora(fechaFutura)
                .estado(EstadoTurno.CONFIRMADO)
                .tieneObraSocial(true)
                .obraSocial("OSDE")
                .build();

        when(turnoRepository.save(any(Turno.class))).thenReturn(turnoGuardado);

        TurnoResponseDTO response = turnoService.reservarTurnoSecretaria(dto);

        assertNotNull(response);
        assertEquals(pacienteGuardado.getId(), response.getPacienteId());
        assertEquals("Juan Pérez", response.getPacienteNombre());
        assertEquals(EstadoTurno.CONFIRMADO, response.getEstado());

        ArgumentCaptor<Paciente> pacienteCaptor = ArgumentCaptor.forClass(Paciente.class);
        verify(pacienteRepository).save(pacienteCaptor.capture());
        Paciente pacienteCreado = pacienteCaptor.getValue();
        assertNull(pacienteCreado.getUsuario());
        assertEquals(dni, pacienteCreado.getDni());
        assertEquals("Juan", pacienteCreado.getNombre());
    }

    @Test
    @DisplayName("Escenario 2: Agendar turno por secretaria para paciente YA registrado asocia el Paciente existente")
    void testReservarTurnoSecretaria_PacienteRegistrado_AsociaPacienteExistente() {
        Long dni = 87654321L;
        LocalDateTime fechaFutura = LocalDateTime.now().plusDays(3);

        TurnoReservaSecretariaDTO dto = TurnoReservaSecretariaDTO.builder()
                .doctorId(doctorId)
                .especialidadId(especialidadId)
                .fechaHora(fechaFutura)
                .nombre("Maria")
                .apellido("Gomez")
                .dni(dni)
                .email("maria.gomez@example.com")
                .telefono("99887766")
                .tieneObraSocial(false)
                .build();

        Usuario usuarioExistente = Usuario.builder()
                .id(50L)
                .email(dto.getEmail())
                .rol(Rol.PACIENTE)
                .build();

        Paciente pacienteExistente = Paciente.builder()
                .id(UUID.randomUUID())
                .usuario(usuarioExistente)
                .nombre("Maria")
                .apellido("Gomez")
                .dni(dni)
                .telefono("99887766")
                .build();

        when(doctorRepository.findById(doctorId)).thenReturn(Optional.of(doctorMock));
        when(especialidadRepository.findById(especialidadId)).thenReturn(Optional.of(especialidadMock));
        when(pacienteRepository.findByDni(dni)).thenReturn(Optional.of(pacienteExistente));
        when(turnoRepository.existsByDoctorIdAndFechaHoraAndEstadoNot(eq(doctorId), eq(fechaFutura), any())).thenReturn(false);

        Turno turnoGuardado = Turno.builder()
                .id(UUID.randomUUID())
                .paciente(pacienteExistente)
                .doctor(doctorMock)
                .especialidad(especialidadMock)
                .fechaHora(fechaFutura)
                .estado(EstadoTurno.CONFIRMADO)
                .tieneObraSocial(false)
                .obraSocial("Particular / Sin Obra Social")
                .build();

        when(turnoRepository.save(any(Turno.class))).thenReturn(turnoGuardado);

        TurnoResponseDTO response = turnoService.reservarTurnoSecretaria(dto);

        assertNotNull(response);
        assertEquals(pacienteExistente.getId(), response.getPacienteId());

        verify(pacienteRepository, never()).save(any(Paciente.class));
        verify(turnoRepository, times(2)).save(any(Turno.class));
    }
}
