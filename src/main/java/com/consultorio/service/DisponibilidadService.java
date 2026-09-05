package com.consultorio.service;

import com.consultorio.domain.EstadoTurno;
import com.consultorio.domain.SlotHorario;
import com.consultorio.domain.Turno;
import com.consultorio.dto.SlotDisponibilidadDTO;
import com.consultorio.repository.DoctorRepository;
import com.consultorio.repository.SlotHorarioRepository;
import com.consultorio.repository.TurnoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DisponibilidadService {

    private final SlotHorarioRepository slotHorarioRepository;
    private final TurnoRepository turnoRepository;
    private final DoctorRepository doctorRepository;

    @Autowired
    public DisponibilidadService(SlotHorarioRepository slotHorarioRepository,
                                 TurnoRepository turnoRepository,
                                 DoctorRepository doctorRepository) {
        this.slotHorarioRepository = slotHorarioRepository;
        this.turnoRepository = turnoRepository;
        this.doctorRepository = doctorRepository;
    }

    public List<SlotDisponibilidadDTO> obtenerHorariosDisponibles(UUID doctorId, LocalDate fecha) {
        return obtenerHorariosDisponibles(doctorId, fecha, null);
    }

    public List<SlotDisponibilidadDTO> obtenerHorariosDisponibles(UUID doctorId, LocalDate fecha, Long especialidadId) {
        var doctorOpt = doctorRepository.findById(doctorId);
        if (doctorOpt.isPresent() && !doctorOpt.get().isDisponibleParaTurnos()) {
            return Collections.emptyList();
        }

        LocalDateTime inicioDia = fecha.atStartOfDay();
        LocalDateTime finDia = fecha.atTime(23, 59, 59);

        // Turnos reservados que no están cancelados
        List<Turno> turnosOcupados = turnoRepository.findByDoctorIdAndFechaHoraBetweenAndEstadoNot(
                doctorId, inicioDia, finDia, EstadoTurno.CANCELADO);

        Set<LocalTime> horasOcupadas = turnosOcupados.stream()
                .map(t -> t.getFechaHora().toLocalTime())
                .collect(Collectors.toSet());

        // Consultar slots concretos guardados para este doctor en esta fecha
        List<SlotHorario> slotsDoctor = slotHorarioRepository.findByDoctorIdAndFecha(doctorId, fecha);

        // Filtrar por especialidad: coincide la especialidad requerida o es General (null)
        List<SlotHorario> slotsFiltrados = slotsDoctor.stream()
                .filter(s -> especialidadId == null || s.getEspecialidad() == null || s.getEspecialidad().getId().equals(especialidadId))
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
}
