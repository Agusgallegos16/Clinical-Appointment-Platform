package com.consultorio.service;

import com.consultorio.domain.*;
import com.consultorio.dto.SlotDisponibilidadDTO;
import com.consultorio.repository.HorarioAtencionRepository;
import com.consultorio.repository.TurnoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DisponibilidadService {

    private final HorarioAtencionRepository horarioAtencionRepository;
    private final TurnoRepository turnoRepository;

    @Autowired
    public DisponibilidadService(HorarioAtencionRepository horarioAtencionRepository,
                                 TurnoRepository turnoRepository) {
        this.horarioAtencionRepository = horarioAtencionRepository;
        this.turnoRepository = turnoRepository;
    }

    public List<SlotDisponibilidadDTO> obtenerHorariosDisponibles(Long doctorId, LocalDate fecha) {
        LocalDateTime inicioDia = fecha.atStartOfDay();
        LocalDateTime finDia = fecha.atTime(23, 59, 59);

        // Turnos reservados que no están cancelados
        List<Turno> turnosOcupados = turnoRepository.findByDoctorIdAndFechaHoraBetweenAndEstadoNot(
                doctorId, inicioDia, finDia, EstadoTurno.CANCELADO);

        Set<LocalTime> horasOcupadas = turnosOcupados.stream()
                .map(t -> t.getFechaHora().toLocalTime())
                .collect(Collectors.toSet());

        // 1. Buscar si hay configuración específica para la fecha puntual
        List<HorarioAtencion> horariosEspeciales = horarioAtencionRepository.findByDoctorIdAndFecha(doctorId, fecha);

        if (!horariosEspeciales.isEmpty()) {
            return generarSlots(horariosEspeciales, horasOcupadas);
        }

        // 2. Si no hay fecha específica, usar la agenda semanal estándar para ese día de la semana
        DiaSemana diaSemana = mapearDiaSemana(fecha.getDayOfWeek());
        List<HorarioAtencion> horariosSemanales = horarioAtencionRepository.findByDoctorIdAndDiaSemanaAndFechaIsNull(doctorId, diaSemana);

        return generarSlots(horariosSemanales, horasOcupadas);
    }

    private List<SlotDisponibilidadDTO> generarSlots(List<HorarioAtencion> horarios, Set<LocalTime> horasOcupadas) {
        List<SlotDisponibilidadDTO> slots = new ArrayList<>();
        for (HorarioAtencion horario : horarios) {
            LocalTime actual = horario.getHoraInicio();
            LocalTime fin = horario.getHoraFin();
            int duracion = horario.getDuracionTurnoMinutos() > 0 ? horario.getDuracionTurnoMinutos() : 30;

            while (actual.plusMinutes(duracion).isBefore(fin) || actual.plusMinutes(duracion).equals(fin)) {
                boolean disponible = !horasOcupadas.contains(actual);
                slots.add(SlotDisponibilidadDTO.builder()
                        .hora(actual)
                        .disponible(disponible)
                        .build());
                actual = actual.plusMinutes(duracion);
            }
        }
        return slots;
    }

    private DiaSemana mapearDiaSemana(DayOfWeek dayOfWeek) {
        switch (dayOfWeek) {
            case MONDAY: return DiaSemana.LUNES;
            case TUESDAY: return DiaSemana.MARTES;
            case WEDNESDAY: return DiaSemana.MIERCOLES;
            case THURSDAY: return DiaSemana.JUEVES;
            case FRIDAY: return DiaSemana.VIERNES;
            case SATURDAY: return DiaSemana.SABADO;
            case SUNDAY: return DiaSemana.DOMINGO;
            default: throw new IllegalArgumentException("Día de la semana no soportado: " + dayOfWeek);
        }
    }
}
