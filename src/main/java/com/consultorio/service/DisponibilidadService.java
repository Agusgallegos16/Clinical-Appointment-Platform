package com.consultorio.service;

import com.consultorio.domain.*;
import com.consultorio.dto.SlotDisponibilidadDTO;
import com.consultorio.repository.BloqueoHorarioRepository;
import com.consultorio.repository.HorarioAtencionRepository;
import com.consultorio.repository.TurnoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
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

    private final HorarioAtencionRepository horarioAtencionRepository;
    private final TurnoRepository turnoRepository;
    private final BloqueoHorarioRepository bloqueoHorarioRepository;

    @Autowired
    public DisponibilidadService(HorarioAtencionRepository horarioAtencionRepository,
                                 TurnoRepository turnoRepository,
                                 BloqueoHorarioRepository bloqueoHorarioRepository) {
        this.horarioAtencionRepository = horarioAtencionRepository;
        this.turnoRepository = turnoRepository;
        this.bloqueoHorarioRepository = bloqueoHorarioRepository;
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

        // Slots bloqueados individualmente por el doctor para esta fecha
        List<BloqueoHorario> bloqueos = bloqueoHorarioRepository.findByDoctorIdAndFecha(doctorId, fecha);

        // 1. Obtener horarios especiales por fecha puntual
        List<HorarioAtencion> horariosEspeciales = horarioAtencionRepository.findByDoctorIdAndFecha(doctorId, fecha);

        // 2. Obtener horarios semanales por día de la semana
        DiaSemana diaSemana = mapearDiaSemana(fecha.getDayOfWeek());
        List<HorarioAtencion> horariosSemanalesRaw = horarioAtencionRepository.findByDoctorIdAndDiaSemanaAndFechaIsNull(doctorId, diaSemana);

        // Filtrar horarios semanales que respeten la vigencia fechaDesde y fechaHasta
        List<HorarioAtencion> horariosSemanales = horariosSemanalesRaw.stream()
                .filter(h -> (h.getFechaDesde() == null || !fecha.isBefore(h.getFechaDesde())) &&
                             (h.getFechaHasta() == null || !fecha.isAfter(h.getFechaHasta())))
                .collect(Collectors.toList());

        // FILTRADO POR ESPECIALIDAD SOLICITADA POR EL PACIENTE
        if (especialidadId != null) {
            horariosEspeciales = horariosEspeciales.stream()
                    .filter(h -> h.getEspecialidad() == null || h.getEspecialidad().getId().equals(especialidadId))
                    .collect(Collectors.toList());

            horariosSemanales = horariosSemanales.stream()
                    .filter(h -> h.getEspecialidad() == null || h.getEspecialidad().getId().equals(especialidadId))
                    .collect(Collectors.toList());
        }

        List<SlotDisponibilidadDTO> slots = new ArrayList<>();

        // A. Agregar todos los slots de los horarios especiales (fecha puntual)
        for (HorarioAtencion he : horariosEspeciales) {
            slots.addAll(generarSlotsParaHorario(he, horasOcupadas, bloqueos));
        }

        // B. Agregar los slots de horarios semanales que NO sean sobreescritos por horarios especiales ni bloqueos
        for (HorarioAtencion hs : horariosSemanales) {
            LocalTime actual = hs.getHoraInicio();
            LocalTime fin = hs.getHoraFin();
            int duracion = hs.getDuracionTurnoMinutos() > 0 ? hs.getDuracionTurnoMinutos() : 30;

            while (actual.plusMinutes(duracion).isBefore(fin) || actual.plusMinutes(duracion).equals(fin)) {
                LocalTime slotStart = actual;
                LocalTime slotEnd = actual.plusMinutes(duracion);

                // Verificar si este slot semanal entra en conflicto con alguna franja especial puntual
                boolean superpuestoConEspecial = false;
                for (HorarioAtencion he : horariosEspeciales) {
                    if (slotStart.isBefore(he.getHoraFin()) && slotEnd.isAfter(he.getHoraInicio())) {
                        superpuestoConEspecial = true;
                        break;
                    }
                }

                // Verificar si este slot fue deshabilitado/bloqueado individualmente por el doctor
                boolean bloqueadoIndividualmente = esSlotBloqueado(slotStart, slotEnd, bloqueos);

                if (!superpuestoConEspecial && !bloqueadoIndividualmente) {
                    boolean disponible = !horasOcupadas.contains(actual);
                    slots.add(SlotDisponibilidadDTO.builder()
                            .hora(actual)
                            .disponible(disponible)
                            .build());
                }

                actual = actual.plusMinutes(duracion);
            }
        }

        // Ordenar slots por hora ascendente
        slots.sort(Comparator.comparing(SlotDisponibilidadDTO::getHora));
        return slots;
    }

    private List<SlotDisponibilidadDTO> generarSlotsParaHorario(HorarioAtencion horario, Set<LocalTime> horasOcupadas, List<BloqueoHorario> bloqueos) {
        List<SlotDisponibilidadDTO> slots = new ArrayList<>();
        LocalTime actual = horario.getHoraInicio();
        LocalTime fin = horario.getHoraFin();
        int duracion = horario.getDuracionTurnoMinutos() > 0 ? horario.getDuracionTurnoMinutos() : 30;

        while (actual.plusMinutes(duracion).isBefore(fin) || actual.plusMinutes(duracion).equals(fin)) {
            LocalTime slotStart = actual;
            LocalTime slotEnd = actual.plusMinutes(duracion);

            if (!esSlotBloqueado(slotStart, slotEnd, bloqueos)) {
                boolean disponible = !horasOcupadas.contains(actual);
                slots.add(SlotDisponibilidadDTO.builder()
                        .hora(actual)
                        .disponible(disponible)
                        .build());
            }
            actual = actual.plusMinutes(duracion);
        }
        return slots;
    }

    private boolean esSlotBloqueado(LocalTime start, LocalTime end, List<BloqueoHorario> bloqueos) {
        for (BloqueoHorario b : bloqueos) {
            if (start.isBefore(b.getHoraFin()) && end.isAfter(b.getHoraInicio())) {
                return true;
            }
        }
        return false;
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
