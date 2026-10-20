package com.consultorio.listener;

import com.consultorio.domain.*;
import com.consultorio.dto.CancelarTurnoDTO;
import com.consultorio.dto.TurnoResponseDTO;
import com.consultorio.event.TurnoCanceladoEvent;
import com.consultorio.repository.*;
import com.consultorio.security.SecurityUtils;
import com.consultorio.service.EmailService;
import com.consultorio.service.TurnoService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@SpringBootTest
@TestPropertySource(properties = {
        "app.email.console-mode=true",
        "app.dev.seed-test-users=false"
})
class TurnoCancelacionNotificacionesIntegrationTest {

    @Autowired
    private TurnoService turnoService;

    @Autowired
    private TurnoRepository turnoRepository;

    @Autowired
    private PacienteRepository pacienteRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private EspecialidadRepository especialidadRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @SpyBean
    private EmailService emailService;

    private String emailPaciente;
    private String emailDoctor;
    private Turno turno;

    @BeforeEach
    void setUp() {
        emailPaciente = "paciente_notif_" + UUID.randomUUID() + "@test.com";
        emailDoctor = "doctor_notif_" + UUID.randomUUID() + "@test.com";

        Usuario usuarioPaciente = Usuario.builder()
                .email(emailPaciente)
                .password("password123")
                .rol(Rol.PACIENTE)
                .activo(true)
                .emailVerificado(true)
                .build();

        Paciente paciente = pacienteRepository.save(Paciente.builder()
                .usuario(usuarioPaciente)
                .nombre("Lucia")
                .apellido("Gomez")
                .dni(10000000L + (long)(Math.random() * 80000000L))
                .telefono("1144556677")
                .fechaNacimiento(LocalDate.of(1995, 3, 20))
                .build());

        Usuario usuarioDoctor = Usuario.builder()
                .email(emailDoctor)
                .password("password123")
                .rol(Rol.DOCTOR)
                .activo(true)
                .emailVerificado(true)
                .build();

        Especialidad especialidad = especialidadRepository.save(Especialidad.builder()
                .nombre("Dermatologia Notif Test " + UUID.randomUUID())
                .descripcion("Especialidad para prueba de notificaciones")
                .build());

        Doctor doctor = doctorRepository.save(Doctor.builder()
                .usuario(usuarioDoctor)
                .nombre("Alberto")
                .apellido("Cormillot")
                .disponibleParaTurnos(true)
                .especialidades(Set.of(especialidad))
                .build());

        turno = turnoRepository.save(Turno.builder()
                .paciente(paciente)
                .doctor(doctor)
                .especialidad(especialidad)
                .fechaHora(LocalDateTime.now().plusDays(4))
                .estado(EstadoTurno.CONFIRMADO)
                .motivoConsulta("Consulta de lunares")
                .build());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void autenticarComo(String email, String rol) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                email,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + rol))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("Cancelación médica: Despacha email al Paciente con el motivo visible y constancia al Doctor")
    void testCancelacionPorDoctorDespachaEmailsCorrectos() {
        autenticarComo(emailDoctor, "DOCTOR");

        String motivoMedico = "Aislamiento preventivo por enfermedad del profesional";
        turnoService.cancelarTurno(turno.getId(), new CancelarTurnoDTO(motivoMedico));

        // Verificar que el paciente recibe el email de aviso con el motivo visible
        ArgumentCaptor<TurnoResponseDTO> dtoCaptor = ArgumentCaptor.forClass(TurnoResponseDTO.class);
        ArgumentCaptor<String> motivoCaptor = ArgumentCaptor.forClass(String.class);

        verify(emailService, timeout(5000)).enviarEmailCancelacionDoctor(
                eq(emailPaciente),
                dtoCaptor.capture(),
                motivoCaptor.capture()
        );

        assertEquals(motivoMedico, motivoCaptor.getValue(), "El motivo despachado debe coincidir exactamente con el ingresado");
        assertEquals("Alberto Cormillot", dtoCaptor.getValue().getDoctorNombre());
        assertEquals("Lucia Gomez", dtoCaptor.getValue().getPacienteNombre());

        // Verificar constancia enviada al médico
        ArgumentCaptor<TurnoCanceladoEvent> eventCaptor = ArgumentCaptor.forClass(TurnoCanceladoEvent.class);
        verify(emailService, timeout(5000)).enviarConfirmacionCancelacionDoctor(
                eq(emailDoctor),
                eventCaptor.capture()
        );

        TurnoCanceladoEvent event = eventCaptor.getValue();
        assertEquals(turno.getId(), event.turnoId());
        assertTrue(event.canceladoPorDoctor());
        assertEquals(motivoMedico, event.motivo());
    }

    @Test
    @DisplayName("Cancelación por paciente: Notifica al Doctor de la liberación con motivo y confirma al Paciente")
    void testCancelacionPorPacienteDespachaEmailsCorrectos() {
        autenticarComo(emailPaciente, "PACIENTE");

        String motivoPaciente = "Superposición de horario laboral";
        turnoService.cancelarTurno(turno.getId(), new CancelarTurnoDTO(motivoPaciente));

        // 1. Verificar notificación al médico informando que el turno fue liberado
        ArgumentCaptor<TurnoCanceladoEvent> eventDoctorCaptor = ArgumentCaptor.forClass(TurnoCanceladoEvent.class);
        verify(emailService, timeout(5000)).enviarEmailCancelacionPorPaciente(
                eq(emailDoctor),
                eventDoctorCaptor.capture()
        );

        TurnoCanceladoEvent eventAlDoctor = eventDoctorCaptor.getValue();
        assertEquals(motivoPaciente, eventAlDoctor.motivo(), "El médico debe recibir el motivo manifestado por el paciente");
        assertEquals("Lucia Gomez", eventAlDoctor.pacienteNombre());
        assertFalse(eventAlDoctor.canceladoPorDoctor(), "Debe reflejar que fue cancelado por el paciente");

        // 2. Verificar confirmación formal de cancelación enviada al paciente
        ArgumentCaptor<TurnoCanceladoEvent> eventPacienteCaptor = ArgumentCaptor.forClass(TurnoCanceladoEvent.class);
        verify(emailService, timeout(5000)).enviarConfirmacionCancelacionPaciente(
                eq(emailPaciente),
                eventPacienteCaptor.capture()
        );

        TurnoCanceladoEvent eventAlPaciente = eventPacienteCaptor.getValue();
        assertEquals(emailPaciente, eventAlPaciente.pacienteEmail());
        assertEquals(motivoPaciente, eventAlPaciente.motivo());
    }
}
