package com.consultorio.service;

import com.consultorio.domain.Especialidad;
import com.consultorio.domain.SlotHorario;
import com.consultorio.dto.ProximoTurnoDisponibleDTO;
import com.consultorio.dto.SlotDisponibilidadDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DisponibilidadServiceTest {

    private static final ZoneId CLINIC_ZONE = ZoneId.of("America/Argentina/Buenos_Aires");
    // 2026-12-20 10:00:00 -03:00
    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-12-20T13:00:00Z"),
            CLINIC_ZONE
    );

    @Mock
    private DoctorService doctorService;

    @Mock
    private TurnoService turnoService;

    private DisponibilidadService disponibilidadService;
    private UUID doctorId;
    private Especialidad cardiologia;
    private Especialidad clinicaMedica;

    @BeforeEach
    void setUp() {
        disponibilidadService = new DisponibilidadService(doctorService, turnoService, FIXED_CLOCK);
        doctorId = UUID.randomUUID();
        cardiologia = Especialidad.builder().id(1L).nombre("Cardiología").build();
        clinicaMedica = Especialidad.builder().id(2L).nombre("Clínica Médica").build();
    }

    @Test
    @DisplayName("Debe saltar días completamente ocupados (20/12 al 30/12) y retornar el primer turno libre (31/12 08:30)")
    void obtenerProximoHorarioDisponible_saltaDiasOcupadosHasta31Dic0830() {
        LocalDate hoy = LocalDate.of(2026, 12, 20);
        LocalDate dic30 = LocalDate.of(2026, 12, 30);
        LocalDate dic31 = LocalDate.of(2026, 12, 31);

        List<SlotHorario> slots = List.of(
                slot(10L, hoy, LocalTime.of(11, 0), cardiologia),
                slot(11L, dic30, LocalTime.of(9, 0), cardiologia),
                slot(12L, dic31, LocalTime.of(8, 0), cardiologia),
                slot(13L, dic31, LocalTime.of(8, 30), cardiologia),
                slot(14L, dic31, LocalTime.of(9, 0), cardiologia)
        );

        Set<LocalDateTime> ocupados = Set.of(
                LocalDateTime.of(hoy, LocalTime.of(11, 0)),
                LocalDateTime.of(dic30, LocalTime.of(9, 0)),
                LocalDateTime.of(dic31, LocalTime.of(8, 0))
        );

        when(doctorService.estaDisponibleParaTurnos(doctorId)).thenReturn(true);
        when(doctorService.obtenerSlotsDoctorDesdeOrdenados(doctorId, hoy)).thenReturn(slots);
        when(turnoService.obtenerFechasHorasOcupadasDoctorDesde(doctorId, hoy.atStartOfDay())).thenReturn(ocupados);

        Optional<ProximoTurnoDisponibleDTO> resultado = disponibilidadService.obtenerProximoHorarioDisponible(
                doctorId, cardiologia.getId(), null);

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getSlotId()).isEqualTo(13L);
        assertThat(resultado.get().getFecha()).isEqualTo(dic31);
        assertThat(resultado.get().getHora()).isEqualTo(LocalTime.of(8, 30));
        assertThat(resultado.get().getEspecialidadId()).isEqualTo(1L);
        assertThat(resultado.get().getEspecialidadNombre()).isEqualTo("Cardiología");
    }

    @Test
    @DisplayName("Debe descartar horarios pasados o iguales a la hora actual en el día de hoy")
    void obtenerProximoHorarioDisponible_descartaHorasPasadasDeHoy() {
        LocalDate hoy = LocalDate.of(2026, 12, 20);

        List<SlotHorario> slots = List.of(
                slot(1L, hoy, LocalTime.of(9, 0), null),
                slot(2L, hoy, LocalTime.of(10, 0), null),
                slot(3L, hoy, LocalTime.of(10, 30), null)
        );

        when(doctorService.estaDisponibleParaTurnos(doctorId)).thenReturn(true);
        when(doctorService.obtenerSlotsDoctorDesdeOrdenados(doctorId, hoy)).thenReturn(slots);
        when(turnoService.obtenerFechasHorasOcupadasDoctorDesde(doctorId, hoy.atStartOfDay())).thenReturn(Set.of());

        Optional<ProximoTurnoDisponibleDTO> resultado = disponibilidadService.obtenerProximoHorarioDisponible(
                doctorId, null, null);

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getSlotId()).isEqualTo(3L);
        assertThat(resultado.get().getFecha()).isEqualTo(hoy);
        assertThat(resultado.get().getHora()).isEqualTo(LocalTime.of(10, 30));
        assertThat(resultado.get().getEspecialidadId()).isNull();
        assertThat(resultado.get().getEspecialidadNombre()).isNull();
    }

    @Test
    @DisplayName("Debe respetar filtro de especialidad permitiendo slots de esa especialidad o generales (null)")
    void obtenerProximoHorarioDisponible_filtraPorEspecialidadAceptandoGenerales() {
        LocalDate hoy = LocalDate.of(2026, 12, 20);

        List<SlotHorario> slots = List.of(
                slot(1L, hoy, LocalTime.of(11, 0), clinicaMedica),
                slot(2L, hoy, LocalTime.of(11, 30), null),
                slot(3L, hoy, LocalTime.of(12, 0), cardiologia)
        );

        when(doctorService.estaDisponibleParaTurnos(doctorId)).thenReturn(true);
        when(doctorService.obtenerSlotsDoctorDesdeOrdenados(doctorId, hoy)).thenReturn(slots);
        when(turnoService.obtenerFechasHorasOcupadasDoctorDesde(doctorId, hoy.atStartOfDay())).thenReturn(Set.of());

        Optional<ProximoTurnoDisponibleDTO> resultado = disponibilidadService.obtenerProximoHorarioDisponible(
                doctorId, cardiologia.getId(), null);

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getSlotId()).isEqualTo(2L);
        assertThat(resultado.get().getHora()).isEqualTo(LocalTime.of(11, 30));
    }

    @Test
    @DisplayName("Debe acotar desdeFecha pasada a hoy y respetar desdeFecha futura")
    void obtenerProximoHorarioDisponible_manejaDesdeFechaPasadaYFutura() {
        LocalDate hoy = LocalDate.of(2026, 12, 20);
        LocalDate fechaPasada = LocalDate.of(2026, 12, 1);
        LocalDate fechaFutura = LocalDate.of(2026, 12, 28);

        when(doctorService.estaDisponibleParaTurnos(doctorId)).thenReturn(true);
        when(doctorService.obtenerSlotsDoctorDesdeOrdenados(doctorId, hoy))
                .thenReturn(List.of(slot(1L, hoy, LocalTime.of(15, 0), null)));
        when(doctorService.obtenerSlotsDoctorDesdeOrdenados(doctorId, fechaFutura))
                .thenReturn(List.of(slot(2L, fechaFutura, LocalTime.of(8, 30), null)));
        when(turnoService.obtenerFechasHorasOcupadasDoctorDesde(eq(doctorId), any())).thenReturn(Set.of());

        Optional<ProximoTurnoDisponibleDTO> conPasada = disponibilidadService.obtenerProximoHorarioDisponible(
                doctorId, null, fechaPasada);
        Optional<ProximoTurnoDisponibleDTO> conFutura = disponibilidadService.obtenerProximoHorarioDisponible(
                doctorId, null, fechaFutura);

        assertThat(conPasada).isPresent();
        assertThat(conPasada.get().getFecha()).isEqualTo(hoy);
        assertThat(conFutura).isPresent();
        assertThat(conFutura.get().getFecha()).isEqualTo(fechaFutura);
        verify(doctorService).obtenerSlotsDoctorDesdeOrdenados(doctorId, hoy);
        verify(doctorService).obtenerSlotsDoctorDesdeOrdenados(doctorId, fechaFutura);
    }

    @Test
    @DisplayName("Debe retornar Optional.empty si el médico no está disponible para turnos sin consultar slots")
    void obtenerProximoHorarioDisponible_retornaVacioSiDoctorNoDisponible() {
        when(doctorService.estaDisponibleParaTurnos(doctorId)).thenReturn(false);

        Optional<ProximoTurnoDisponibleDTO> resultado = disponibilidadService.obtenerProximoHorarioDisponible(
                doctorId, null, null);

        assertThat(resultado).isEmpty();
        verify(doctorService, never()).obtenerSlotsDoctorDesdeOrdenados(any(), any());
        verify(turnoService, never()).obtenerFechasHorasOcupadasDoctorDesde(any(), any());
    }

    @Test
    @DisplayName("Debe retornar Optional.empty si no existen slots libres futuros")
    void obtenerProximoHorarioDisponible_retornaVacioSiTodoEstaOcupado() {
        LocalDate hoy = LocalDate.of(2026, 12, 20);
        List<SlotHorario> slots = List.of(slot(1L, hoy, LocalTime.of(14, 0), cardiologia));

        when(doctorService.estaDisponibleParaTurnos(doctorId)).thenReturn(true);
        when(doctorService.obtenerSlotsDoctorDesdeOrdenados(doctorId, hoy)).thenReturn(slots);
        when(turnoService.obtenerFechasHorasOcupadasDoctorDesde(doctorId, hoy.atStartOfDay()))
                .thenReturn(Set.of(LocalDateTime.of(hoy, LocalTime.of(14, 0))));

        Optional<ProximoTurnoDisponibleDTO> resultado = disponibilidadService.obtenerProximoHorarioDisponible(
                doctorId, cardiologia.getId(), null);

        assertThat(resultado).isEmpty();
    }

    @Test
    @DisplayName("Regresión: obtenerHorariosDisponibles mantiene el comportamiento existente de filtrado y ordenamiento")
    void obtenerHorariosDisponibles_mantieneComportamientoExistente() {
        LocalDate fecha = LocalDate.of(2026, 12, 31);
        List<SlotHorario> slots = List.of(
                slot(2L, fecha, LocalTime.of(9, 0), cardiologia),
                slot(1L, fecha, LocalTime.of(8, 30), null),
                slot(3L, fecha, LocalTime.of(8, 0), cardiologia),
                slot(4L, fecha, LocalTime.of(9, 30), clinicaMedica)
        );

        when(doctorService.estaDisponibleParaTurnos(doctorId)).thenReturn(true);
        when(doctorService.obtenerSlotsDoctorPorFecha(doctorId, fecha)).thenReturn(slots);
        when(turnoService.obtenerHorasOcupadasDoctor(doctorId, fecha)).thenReturn(Set.of(LocalTime.of(8, 0)));

        List<SlotDisponibilidadDTO> disponibles = disponibilidadService.obtenerHorariosDisponibles(
                doctorId, fecha, cardiologia.getId());

        assertThat(disponibles)
                .extracting(SlotDisponibilidadDTO::getHora)
                .containsExactly(LocalTime.of(8, 30), LocalTime.of(9, 0));
    }

    private SlotHorario slot(Long id, LocalDate fecha, LocalTime horaInicio, Especialidad especialidad) {
        return SlotHorario.builder()
                .id(id)
                .fecha(fecha)
                .horaInicio(horaInicio)
                .horaFin(horaInicio.plusMinutes(30))
                .duracionMinutos(30)
                .especialidad(especialidad)
                .build();
    }
}
