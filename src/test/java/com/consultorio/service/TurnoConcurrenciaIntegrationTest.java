package com.consultorio.service;

import com.consultorio.domain.*;
import com.consultorio.dto.TurnoReservaDTO;
import com.consultorio.dto.TurnoResponseDTO;
import com.consultorio.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class TurnoConcurrenciaIntegrationTest {

    @Autowired
    private TurnoService turnoService;

    @Autowired
    private TurnoRepository turnoRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private PacienteRepository pacienteRepository;

    @Autowired
    private EspecialidadRepository especialidadRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private SlotHorarioRepository slotHorarioRepository;

    private Doctor doctor;
    private Especialidad especialidad;
    private LocalDateTime fechaHoraSlot;
    private final List<Paciente> pacientes = new ArrayList<>();
    private final List<Usuario> usuarios = new ArrayList<>();
    private SlotHorario slotHorario;

    @BeforeEach
    void setUp() {
        Usuario usuarioDoctor = Usuario.builder()
                .email("doctor_concurr_" + UUID.randomUUID() + "@test.com")
                .password("password123")
                .rol(Rol.DOCTOR)
                .activo(true)
                .emailVerificado(true)
                .build();

        especialidad = especialidadRepository.save(Especialidad.builder()
                .nombre("Cardiologia Concurrencia " + UUID.randomUUID())
                .descripcion("Especialidad para prueba de concurrencia")
                .build());

        doctor = doctorRepository.save(Doctor.builder()
                .usuario(usuarioDoctor)
                .nombre("Gregory")
                .apellido("House")
                .disponibleParaTurnos(true)
                .especialidades(Set.of(especialidad))
                .build());

        LocalDate fecha = LocalDate.now().plusDays(5);
        LocalTime hora = LocalTime.of(10, 0);
        fechaHoraSlot = LocalDateTime.of(fecha, hora);

        slotHorario = slotHorarioRepository.save(SlotHorario.builder()
                .doctor(doctor)
                .especialidad(especialidad)
                .fecha(fecha)
                .horaInicio(hora)
                .horaFin(hora.plusMinutes(30))
                .duracionMinutos(30)
                .build());

        for (int i = 0; i < 20; i++) {
            Usuario u = Usuario.builder()
                    .email("paciente_concurr_" + i + "_" + UUID.randomUUID() + "@test.com")
                    .password("password123")
                    .rol(Rol.PACIENTE)
                    .activo(true)
                    .emailVerificado(true)
                    .build();

            Paciente p = pacienteRepository.save(Paciente.builder()
                    .usuario(u)
                    .nombre("Paciente" + i)
                    .apellido("Test")
                    .dni(41000000L + (long) (Math.random() * 900000))
                    .build());
            pacientes.add(p);
        }
    }

    @AfterEach
    void tearDown() {
        if (doctor != null) {
            List<Turno> turnos = turnoRepository.findByDoctorIdAndFechaHoraBetweenOrderByFechaHoraAsc(
                    doctor.getId(), fechaHoraSlot.minusDays(1), fechaHoraSlot.plusDays(1));
            turnoRepository.deleteAll(turnos);
        }
        if (slotHorario != null) {
            slotHorarioRepository.deleteById(slotHorario.getId());
        }
        for (Paciente p : pacientes) {
            pacienteRepository.deleteById(p.getId());
        }
        if (doctor != null) {
            doctorRepository.deleteById(doctor.getId());
        }
        if (especialidad != null) {
            especialidadRepository.deleteById(especialidad.getId());
        }
    }

    @Test
    @DisplayName("Doble Booking: 20 pacientes compitiendo simultáneamente por el mismo slot -> exactamente 1 gana, 19 fallan")
    void testReservaConcurrenteMismoSlotUnicoGanador() throws InterruptedException {
        int numHilos = pacientes.size();
        ExecutorService executor = Executors.newFixedThreadPool(numHilos);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(numHilos);

        AtomicInteger reservasExitosas = new AtomicInteger(0);
        AtomicInteger conflictosDetectados = new AtomicInteger(0);

        for (int i = 0; i < numHilos; i++) {
            final Paciente pacienteActual = pacientes.get(i);
            executor.submit(() -> {
                try {
                    startGate.await(); // Esperar disparo simultáneo

                    // Autenticar hilo individual
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            pacienteActual.getUsuario().getEmail(),
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_PACIENTE"))
                    );
                    SecurityContext context = SecurityContextHolder.createEmptyContext();
                    context.setAuthentication(auth);
                    SecurityContextHolder.setContext(context);

                    TurnoReservaDTO dto = new TurnoReservaDTO();
                    dto.setDoctorId(doctor.getId());
                    dto.setEspecialidadId(especialidad.getId());
                    dto.setPacienteId(pacienteActual.getId());
                    dto.setFechaHora(fechaHoraSlot);
                    dto.setTieneObraSocial(false);
                    dto.setMotivoConsulta("Consulta de control");

                    TurnoResponseDTO response = turnoService.reservarTurno(dto);
                    if (response != null && response.getId() != null) {
                        reservasExitosas.incrementAndGet();
                    }
                } catch (IllegalStateException | org.springframework.dao.DataIntegrityViolationException e) {
                    conflictosDetectados.incrementAndGet();
                } catch (Exception e) {
                    if (e.getClass().getSimpleName().contains("Lock") || e.getClass().getSimpleName().contains("Conflict")) {
                        conflictosDetectados.incrementAndGet();
                    } else {
                        conflictosDetectados.incrementAndGet();
                    }
                } finally {
                    SecurityContextHolder.clearContext();
                    endGate.countDown();
                }
            });
        }

        startGate.countDown(); // DISPARO SIMULTÁNEO
        boolean terminaron = endGate.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        assertEquals(true, terminaron, "Todas las peticiones concurrentes deben completarse en menos de 15 segundos.");
        assertEquals(1, reservasExitosas.get(), "Exactamente 1 paciente debe ganar la reserva del slot.");
        assertEquals(numHilos - 1, conflictosDetectados.get(), "Los restantes pacientes deben recibir conflicto por horario ocupado.");

        List<Turno> turnosEnBD = turnoRepository.findByDoctorIdAndFechaHoraBetweenAndEstadoNot(
                doctor.getId(), fechaHoraSlot.minusMinutes(1), fechaHoraSlot.plusMinutes(1), EstadoTurno.CANCELADO);
        assertEquals(1, turnosEnBD.size(), "Solo debe existir 1 turno activo guardado en base de datos para ese médico y fecha/hora.");
    }
}
