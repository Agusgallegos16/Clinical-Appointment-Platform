package com.consultorio.service;

import com.consultorio.domain.SlotHorario;
import com.consultorio.dto.ProximoTurnoDisponibleDTO;
import com.consultorio.dto.SlotDisponibilidadDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DisponibilidadService {

    private static final ZoneId DEFAULT_CLINIC_ZONE = ZoneId.of("America/Argentina/Buenos_Aires");

    private final DoctorService doctorService;
    private final TurnoService turnoService;
    private final Clock clock;

    public DisponibilidadService(DoctorService doctorService, TurnoService turnoService) {
        this(doctorService, turnoService, Clock.system(DEFAULT_CLINIC_ZONE));
    }

    @Autowired
    public DisponibilidadService(DoctorService doctorService, TurnoService turnoService, Clock clock) {
        this.doctorService = doctorService;
        this.turnoService = turnoService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<SlotDisponibilidadDTO> obtenerHorariosDisponibles(UUID doctorId, LocalDate fecha) {
        return obtenerHorariosDisponibles(doctorId, fecha, null);
    }

    @Transactional(readOnly = true)
    public List<SlotDisponibilidadDTO> obtenerHorariosDisponibles(UUID doctorId, LocalDate fecha, Long especialidadId) {
        if (!doctorService.estaDisponibleParaTurnos(doctorId)) {
            return Collections.emptyList();
        }

        Set<LocalTime> horasOcupadas = turnoService.obtenerHorasOcupadasDoctor(doctorId, fecha);

        List<SlotHorario> slotsDoctor = doctorService.obtenerSlotsDoctorPorFecha(doctorId, fecha);

        List<SlotHorario> slotsFiltrados = slotsDoctor.stream()
                .filter(s -> esSlotCompatibleConEspecialidad(s, especialidadId))
                .collect(Collectors.toList());

        List<SlotDisponibilidadDTO> resultado = new ArrayList<>();
        for (SlotHorario s : slotsFiltrados) {
            boolean disponible = !horasOcupadas.contains(s.getHoraInicio());
            if (disponible) {
                resultado.add(SlotDisponibilidadDTO.builder()
                        .id(s.getId())
                        .hora(s.getHoraInicio())
                        .disponible(true)
                        .especialidadId(s.getEspecialidad() != null ? s.getEspecialidad().getId() : null)
                        .especialidadNombre(s.getEspecialidad() != null ? s.getEspecialidad().getNombre() : null)
                        .build());
            }
        }

        resultado.sort(Comparator.comparing(SlotDisponibilidadDTO::getHora));
        return resultado;
    }

    @Transactional(readOnly = true)
    public Optional<ProximoTurnoDisponibleDTO> obtenerProximoHorarioDisponible(UUID doctorId, Long especialidadId) {
        return obtenerProximoHorarioDisponible(doctorId, especialidadId, null);
    }

    @Transactional(readOnly = true)
    public Optional<ProximoTurnoDisponibleDTO> obtenerProximoHorarioDisponible(
            UUID doctorId, Long especialidadId, LocalDate desdeFechaOpcional) {
        if (!doctorService.estaDisponibleParaTurnos(doctorId)) {
            return Optional.empty();
        }

        LocalDateTime ahora = LocalDateTime.now(clock);
        LocalDate hoy = ahora.toLocalDate();
        LocalDate fechaInicioBusqueda = (desdeFechaOpcional != null && desdeFechaOpcional.isAfter(hoy))
                ? desdeFechaOpcional
                : hoy;

        List<SlotHorario> slotsCandidatos = doctorService.obtenerSlotsDoctorDesdeOrdenados(doctorId, fechaInicioBusqueda);
        if (slotsCandidatos.isEmpty()) {
            return Optional.empty();
        }

        LocalDateTime inicioRangoOcupados = fechaInicioBusqueda.atStartOfDay();
        Set<LocalDateTime> fechasHorasOcupadas = turnoService.obtenerFechasHorasOcupadasDoctorDesde(
                doctorId, inicioRangoOcupados);

        return slotsCandidatos.stream()
                .filter(slot -> esSlotCompatibleConEspecialidad(slot, especialidadId))
                .filter(slot -> {
                    LocalDateTime fechaHoraSlot = LocalDateTime.of(slot.getFecha(), slot.getHoraInicio());
                    return fechaHoraSlot.isAfter(ahora) && !fechasHorasOcupadas.contains(fechaHoraSlot);
                })
                .findFirst()
                .map(this::mapearProximoTurnoDTO);
    }

    private boolean esSlotCompatibleConEspecialidad(SlotHorario slot, Long especialidadId) {
        return especialidadId == null
                || slot.getEspecialidad() == null
                || slot.getEspecialidad().getId().equals(especialidadId);
    }

    private ProximoTurnoDisponibleDTO mapearProximoTurnoDTO(SlotHorario slot) {
        return ProximoTurnoDisponibleDTO.builder()
                .slotId(slot.getId())
                .fecha(slot.getFecha())
                .hora(slot.getHoraInicio())
                .especialidadId(slot.getEspecialidad() != null ? slot.getEspecialidad().getId() : null)
                .especialidadNombre(slot.getEspecialidad() != null ? slot.getEspecialidad().getNombre() : null)
                .build();
    }
}
