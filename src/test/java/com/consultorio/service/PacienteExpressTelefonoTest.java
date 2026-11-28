package com.consultorio.service;

import com.consultorio.domain.Paciente;
import com.consultorio.dto.TurnoReservaSecretariaDTO;
import com.consultorio.repository.PacienteRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PacienteExpressTelefonoTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @Mock
    private PacienteRepository pacienteRepository;

    @Mock
    private UsuarioService usuarioService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private PacienteService pacienteService;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void tearDownValidator() {
        validatorFactory.close();
    }

    private TurnoReservaSecretariaDTO buildValidDto(String telefono) {
        return TurnoReservaSecretariaDTO.builder()
                .doctorId(UUID.randomUUID())
                .especialidadId(1L)
                .fechaHora(LocalDateTime.now().plusDays(1))
                .nombre("Juan")
                .apellido("Perez")
                .dni(35123456L)
                .email("juan@example.com")
                .telefono(telefono)
                .fechaNacimiento(LocalDate.of(1990, 5, 15))
                .build();
    }

    @Test
    void dtoRejectsNullOrBlankTelefono() {
        Set<ConstraintViolation<TurnoReservaSecretariaDTO>> nullViolations = validator.validate(buildValidDto(null));
        Set<ConstraintViolation<TurnoReservaSecretariaDTO>> blankViolations = validator.validate(buildValidDto("   "));

        assertTrue(nullViolations.stream().anyMatch(v -> "telefono".equals(v.getPropertyPath().toString())));
        assertTrue(blankViolations.stream().anyMatch(v -> "telefono".equals(v.getPropertyPath().toString())));
    }

    @Test
    void dtoRejectsTelefonoExceedingMaxLength50() {
        String tooLong = "1".repeat(51);
        Set<ConstraintViolation<TurnoReservaSecretariaDTO>> violations = validator.validate(buildValidDto(tooLong));

        assertTrue(violations.stream().anyMatch(v -> "telefono".equals(v.getPropertyPath().toString())));
    }

    @Test
    void dtoAcceptsValidTelefono() {
        Set<ConstraintViolation<TurnoReservaSecretariaDTO>> violations = validator.validate(buildValidDto("11 2233-4455"));

        assertTrue(violations.isEmpty());
    }

    @Test
    void expressRegistrationSavesTrimmedTelefonoForNewPaciente() {
        when(pacienteRepository.findByDni(35123456L)).thenReturn(Optional.empty());
        when(pacienteRepository.save(any(Paciente.class))).thenAnswer(inv -> inv.getArgument(0));

        Paciente saved = pacienteService.obtenerORegistrarPacienteExpress(
                "Juan", "Perez", 35123456L, "  11 2233-4455  ", "juan@example.com", LocalDate.of(1990, 5, 15));

        assertEquals("11 2233-4455", saved.getTelefono());
        verify(pacienteRepository).save(any(Paciente.class));
    }

    @Test
    void expressRegistrationUpdatesTelefonoWhenExistingPacienteHasNone() {
        Paciente existente = Paciente.builder()
                .id(UUID.randomUUID())
                .nombre("Maria")
                .apellido("Gomez")
                .dni(30111222L)
                .telefono(null)
                .build();
        when(pacienteRepository.findByDni(30111222L)).thenReturn(Optional.of(existente));
        when(pacienteRepository.save(any(Paciente.class))).thenAnswer(inv -> inv.getArgument(0));

        Paciente result = pacienteService.obtenerORegistrarPacienteExpress(
                "Maria", "Gomez", 30111222L, " 11 4455-6677 ", null, null);

        assertEquals("11 4455-6677", result.getTelefono());
        verify(pacienteRepository).save(existente);
    }

    @Test
    void expressRegistrationUpdatesTelefonoWhenSecretaryModifiesExistingPacientePhone() {
        Paciente existente = Paciente.builder()
                .id(UUID.randomUUID())
                .nombre("Maria")
                .apellido("Gomez")
                .dni(30111222L)
                .telefono("11 0000-0000")
                .build();
        when(pacienteRepository.findByDni(30111222L)).thenReturn(Optional.of(existente));
        when(pacienteRepository.save(any(Paciente.class))).thenAnswer(inv -> inv.getArgument(0));

        Paciente result = pacienteService.obtenerORegistrarPacienteExpress(
                "Maria", "Gomez", 30111222L, "11 9999-8888", null, null);

        assertEquals("11 9999-8888", result.getTelefono());
        verify(pacienteRepository).save(existente);
    }

    @Test
    void expressRegistrationDoesNotSaveWhenExistingPacienteTelefonoIsUnchanged() {
        Paciente existente = Paciente.builder()
                .id(UUID.randomUUID())
                .nombre("Maria")
                .apellido("Gomez")
                .dni(30111222L)
                .telefono("11 2233-4455")
                .build();
        when(pacienteRepository.findByDni(30111222L)).thenReturn(Optional.of(existente));

        Paciente result = pacienteService.obtenerORegistrarPacienteExpress(
                "Maria", "Gomez", 30111222L, "  11 2233-4455  ", null, null);

        assertEquals("11 2233-4455", result.getTelefono());
        verify(pacienteRepository, never()).save(any());
    }
}
