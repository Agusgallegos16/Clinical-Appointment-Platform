package com.consultorio.service;

import com.consultorio.domain.Doctor;
import com.consultorio.domain.Especialidad;
import com.consultorio.domain.Paciente;
import com.consultorio.domain.Usuario;
import com.consultorio.dto.TurnoReservaDTO;
import com.consultorio.exception.PerfilIncompletoException;
import com.consultorio.repository.TurnoRepository;
import com.consultorio.security.SecurityUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TurnoPerfilIncompletoTest {

    @Mock
    private TurnoRepository turnoRepository;

    @Mock
    private PacienteService pacienteService;

    @Mock
    private DoctorService doctorService;

    @Mock
    private EspecialidadService especialidadService;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private TurnoService turnoService;

    @Test
    @DisplayName("Reservar turno con perfil de paciente incompleto (sin DNI ni fecha de nacimiento) debe lanzar PerfilIncompletoException")
    void testReservarTurno_PerfilIncompleto_LanzaPerfilIncompletoException() {
        UUID pacienteId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        Long especialidadId = 2L;
        String email = "google_user@test.com";

        Usuario usuario = Usuario.builder()
                .id(10L)
                .email(email)
                .build();

        // Paciente creado vía Google OAuth (sin DNI, sin teléfono, sin fecha de nacimiento)
        Paciente pacienteIncompleto = Paciente.builder()
                .id(pacienteId)
                .usuario(usuario)
                .nombre("Google")
                .apellido("User")
                .email(email)
                .dni(null)
                .telefono(null)
                .fechaNacimiento(null)
                .build();

        Doctor doctor = Doctor.builder().id(doctorId).nombre("Gregory").apellido("House").build();
        Especialidad especialidad = Especialidad.builder().id(especialidadId).nombre("Clínica").build();

        TurnoReservaDTO dto = new TurnoReservaDTO();
        dto.setPacienteId(pacienteId);
        dto.setDoctorId(doctorId);
        dto.setEspecialidadId(especialidadId);
        dto.setFechaHora(LocalDateTime.now().plusDays(2));
        dto.setTieneObraSocial(false);
        dto.setObraSocial("Particular");
        dto.setMotivoConsulta("Chequeo general");

        when(pacienteService.obtenerPorId(pacienteId)).thenReturn(pacienteIncompleto);
        when(doctorService.obtenerPorIdConBloqueo(doctorId)).thenReturn(doctor);
        when(especialidadService.obtenerPorId(especialidadId)).thenReturn(especialidad);
        when(securityUtils.obtenerEmailUsuarioAutenticado()).thenReturn(email);

        PerfilIncompletoException ex = assertThrows(PerfilIncompletoException.class, () ->
                turnoService.reservarTurno(dto));

        assertNotNull(ex.getCamposFaltantes());
        assertTrue(ex.getCamposFaltantes().contains("dni"));
        assertTrue(ex.getCamposFaltantes().contains("telefono"));
        assertTrue(ex.getCamposFaltantes().contains("fechaNacimiento"));

        verify(turnoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Paciente con perfil completo (DNI, teléfono y fecha de nacimiento) pasa la validación de perfil")
    void testPaciente_PerfilCompleto_PasaValidacion() {
        Paciente pacienteCompleto = Paciente.builder()
                .id(UUID.randomUUID())
                .nombre("Carlos")
                .apellido("Perez")
                .dni(35123456L)
                .telefono("+54 11 1234-5678")
                .fechaNacimiento(LocalDate.of(1990, 1, 1))
                .build();

        assertTrue(pacienteCompleto.isPerfilCompleto());
        assertTrue(pacienteCompleto.getCamposFaltantes().isEmpty());
    }
}
