package com.consultorio.service;

import com.consultorio.domain.EstadoTurno;
import com.consultorio.domain.SlotHorario;
import com.consultorio.domain.Turno;
import com.consultorio.dto.SlotDisponibilidadDTO;
import com.consultorio.repository.SlotHorarioRepository;
import com.consultorio.repository.TurnoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DisponibilidadService {

    private final SlotHorarioRepository slotHorarioRepository;
    private final TurnoRepository turnoRepository;

    @Autowired
    public DisponibilidadService(SlotHorarioRepository slotHorarioRepository,
                                 TurnoRepository turnoRepository) {
        this.slotHorarioRepository = slotHorarioRepository;
        this.turnoRepository = turnoRepository;
    }

    public List<SlotDisponibilidadDTO> obtenerHorariosDisponibles(Long doctorId, LocalDate fecha) {
        return obtenerHorariosDisponibles(doctorId, fecha, null);
    }

    public List<SlotDisponibilidadDTO> obtenerHorariosDisponibles(Long doctorId, LocalDate fecha, Long especialidadId) {
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
            resultado.add(SlotDisponibilidadDTO.builder()
                    .hora(s.getHoraInicio())
                    .disponible(disponible)
                    .build());
        }

        resultado.sort(Comparator.comparing(SlotDisponibilidadDTO::getHora));
        return resultado;
    }
}
