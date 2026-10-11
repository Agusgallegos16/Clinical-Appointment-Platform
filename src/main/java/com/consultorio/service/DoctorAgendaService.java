package com.consultorio.service;

import com.consultorio.domain.*;
import com.consultorio.dto.HorarioAtencionDTO;
import com.consultorio.repository.DoctorRepository;
import com.consultorio.repository.HorarioAtencionRepository;
import com.consultorio.repository.SlotHorarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DoctorAgendaService {

    private final HorarioAtencionRepository horarioAtencionRepository;
    private final SlotHorarioRepository slotHorarioRepository;
    private final DoctorRepository doctorRepository;
    private final EspecialidadService especialidadService;

    @Autowired
    public DoctorAgendaService(HorarioAtencionRepository horarioAtencionRepository,
                               SlotHorarioRepository slotHorarioRepository,
                               DoctorRepository doctorRepository,
                               EspecialidadService especialidadService) {
        this.horarioAtencionRepository = horarioAtencionRepository;
        this.slotHorarioRepository = slotHorarioRepository;
        this.doctorRepository = doctorRepository;
        this.especialidadService = especialidadService;
    }

    @Transactional
    public HorarioAtencion agregarHorarioAtencion(UUID doctorId, HorarioAtencionDTO dto) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new IllegalArgumentException("Doctor no encontrado con ID: " + doctorId));
        LocalDate hoy = LocalDate.now();

        if (dto.getFecha() != null && dto.getFecha().isBefore(hoy)) {
            throw new IllegalArgumentException("No se pueden configurar horarios para fechas pasadas.");
        }

        if (dto.getFechaHasta() != null && dto.getFechaHasta().isBefore(hoy)) {
            throw new IllegalArgumentException("La fecha límite de vigencia no puede ser anterior a la fecha actual.");
        }

        if (dto.getDiaSemana() == null && dto.getFecha() == null) {
            throw new IllegalArgumentException("Debe especificar un día de la semana o una fecha puntual para el horario.");
        }

        if (dto.getHoraInicio().isAfter(dto.getHoraFin()) || dto.getHoraInicio().equals(dto.getHoraFin())) {
            throw new IllegalArgumentException("La hora de inicio debe ser anterior a la hora de fin.");
        }

        int duracion = dto.getDuracionTurnoMinutos() > 0 ? dto.getDuracionTurnoMinutos() : 30;

        // Validar que los slots propuestos no colisionen con slots existentes en la base de datos
        validarNoColisionSlots(doctorId, dto.getDiaSemana(), dto.getFecha(), dto.getFechaDesde(), dto.getFechaHasta(), dto.getHoraInicio(), dto.getHoraFin(), duracion);

        Especialidad especialidad = null;
        if (dto.getEspecialidadId() != null) {
            especialidad = especialidadService.buscarPorId(dto.getEspecialidadId()).orElse(null);
        }

        HorarioAtencion horario = HorarioAtencion.builder()
                .doctor(doctor)
                .especialidad(especialidad)
                .diaSemana(dto.getDiaSemana())
                .fecha(dto.getFecha())
                .fechaDesde(dto.getFechaDesde())
                .fechaHasta(dto.getFechaHasta())
                .horaInicio(dto.getHoraInicio())
                .horaFin(dto.getHoraFin())
                .duracionTurnoMinutos(duracion)
                .build();

        HorarioAtencion guardado = horarioAtencionRepository.save(horario);

        // Generar slots instanciados concretos
        if (dto.getFecha() != null) {
            generarSlotsParaFecha(doctor, dto.getFecha(), dto.getHoraInicio(), dto.getHoraFin(), duracion, especialidad, true);
        } else if (dto.getDiaSemana() != null) {
            instanciarSlotsSemanales(doctor, dto.getDiaSemana(), dto.getHoraInicio(), dto.getHoraFin(), duracion, especialidad, dto.getFechaDesde(), dto.getFechaHasta());
        }

        return guardado;
    }

    @Transactional
    public HorarioAtencion actualizarHorarioAtencion(Long horarioId, HorarioAtencionDTO dto) {
        HorarioAtencion horario = horarioAtencionRepository.findById(horarioId)
                .orElseThrow(() -> new IllegalArgumentException("Horario no encontrado con ID: " + horarioId));

        LocalDate hoy = LocalDate.now();

        if (dto.getFecha() != null && dto.getFecha().isBefore(hoy)) {
            throw new IllegalArgumentException("No se pueden configurar horarios para fechas pasadas.");
        }

        if (dto.getHoraInicio().isAfter(dto.getHoraFin()) || dto.getHoraInicio().equals(dto.getHoraFin())) {
            throw new IllegalArgumentException("La hora de inicio debe ser anterior a la hora de fin.");
        }

        int duracion = dto.getDuracionTurnoMinutos() > 0 ? dto.getDuracionTurnoMinutos() : 30;

        // Eliminar temporalmente los slots del horario anterior para poder re-validar e instanciar
        eliminarSlotsDeHorario(horario);

        // Validar colisiones
        validarNoColisionSlots(horario.getDoctor().getId(), dto.getDiaSemana(), dto.getFecha(), dto.getFechaDesde(), dto.getFechaHasta(), dto.getHoraInicio(), dto.getHoraFin(), duracion);

        Especialidad especialidad = null;
        if (dto.getEspecialidadId() != null) {
            especialidad = especialidadService.buscarPorId(dto.getEspecialidadId()).orElse(null);
        }

        horario.setEspecialidad(especialidad);
        horario.setDiaSemana(dto.getDiaSemana());
        horario.setFecha(dto.getFecha());
        horario.setFechaDesde(dto.getFechaDesde());
        horario.setFechaHasta(dto.getFechaHasta());
        horario.setHoraInicio(dto.getHoraInicio());
        horario.setHoraFin(dto.getHoraFin());
        horario.setDuracionTurnoMinutos(duracion);

        HorarioAtencion guardado = horarioAtencionRepository.save(horario);

        // Regenerar los slots correspondientes
        if (dto.getFecha() != null) {
            generarSlotsParaFecha(horario.getDoctor(), dto.getFecha(), dto.getHoraInicio(), dto.getHoraFin(), duracion, especialidad, true);
        } else if (dto.getDiaSemana() != null) {
            instanciarSlotsSemanales(horario.getDoctor(), dto.getDiaSemana(), dto.getHoraInicio(), dto.getHoraFin(), duracion, especialidad, dto.getFechaDesde(), dto.getFechaHasta());
        }

        return guardado;
    }

    @Transactional(readOnly = true)
    public List<HorarioAtencion> obtenerHorariosDoctor(UUID doctorId) {
        return horarioAtencionRepository.findByDoctorId(doctorId);
    }

    @Transactional
    public void eliminarHorarioAtencion(Long horarioId) {
        HorarioAtencion horario = horarioAtencionRepository.findById(horarioId).orElse(null);
        if (horario != null) {
            eliminarSlotsDeHorario(horario);
            horarioAtencionRepository.delete(horario);
        }
    }

    @Transactional
    public void limpiarHorariosSemana(UUID doctorId, LocalDate desde, LocalDate hasta) {
        slotHorarioRepository.deleteByDoctorIdAndFechaBetween(doctorId, desde, hasta);

        List<HorarioAtencion> horarios = horarioAtencionRepository.findByDoctorId(doctorId);
        for (HorarioAtencion h : horarios) {
            if (h.getFecha() != null && !h.getFecha().isBefore(desde) && !h.getFecha().isAfter(hasta)) {
                horarioAtencionRepository.delete(h);
            }
        }
    }

    @Transactional
    public void limpiarHorariosPorFecha(UUID doctorId, LocalDate fecha) {
        horarioAtencionRepository.deleteByDoctorIdAndFecha(doctorId, fecha);
    }

    @Transactional
    public void limpiarHorariosPorDiaSemana(UUID doctorId, DiaSemana diaSemana) {
        horarioAtencionRepository.deleteByDoctorIdAndDiaSemanaAndFechaIsNull(doctorId, diaSemana);
    }

    @Transactional(readOnly = true)
    public List<SlotHorario> obtenerSlotsDoctor(UUID doctorId, LocalDate desde, LocalDate hasta) {
        if (desde != null && hasta != null) {
            return slotHorarioRepository.findByDoctorIdAndFechaBetween(doctorId, desde, hasta);
        }
        return slotHorarioRepository.findByDoctorId(doctorId);
    }

    @Transactional(readOnly = true)
    public List<SlotHorario> obtenerSlotsDoctorPorFecha(UUID doctorId, LocalDate fecha) {
        return slotHorarioRepository.findByDoctorIdAndFecha(doctorId, fecha);
    }

    @Transactional
    public void eliminarSlotIndividual(Long slotId) {
        slotHorarioRepository.deleteById(slotId);
    }

    @Transactional(readOnly = true)
    public boolean existeSlotActivo(UUID doctorId, LocalDate fecha, LocalTime horaInicio) {
        return slotHorarioRepository.existsByDoctorIdAndFechaAndHoraInicio(doctorId, fecha, horaInicio);
    }

    private void eliminarSlotsDeHorario(HorarioAtencion horario) {
        if (horario.getFecha() != null) {
            List<SlotHorario> slots = slotHorarioRepository.findByDoctorIdAndFecha(horario.getDoctor().getId(), horario.getFecha());
            List<SlotHorario> aBorrar = slots.stream()
                    .filter(s -> s.isEsPuntual() && horario.getHoraInicio().isBefore(s.getHoraFin()) && horario.getHoraFin().isAfter(s.getHoraInicio()))
                    .collect(Collectors.toList());
            slotHorarioRepository.deleteAll(aBorrar);
        } else if (horario.getDiaSemana() != null) {
            LocalDate inicio = (horario.getFechaDesde() != null && !horario.getFechaDesde().isBefore(LocalDate.now())) ? horario.getFechaDesde() : LocalDate.now();
            LocalDate fin = (horario.getFechaHasta() != null) ? horario.getFechaHasta() : inicio.plusWeeks(8);
            for (LocalDate date = inicio; !date.isAfter(fin); date = date.plusDays(1)) {
                if (mapearDiaSemana(date.getDayOfWeek()) == horario.getDiaSemana()) {
                    List<SlotHorario> slots = slotHorarioRepository.findByDoctorIdAndFecha(horario.getDoctor().getId(), date);
                    List<SlotHorario> aBorrar = slots.stream()
                            .filter(s -> !s.isEsPuntual() && horario.getHoraInicio().isBefore(s.getHoraFin()) && horario.getHoraFin().isAfter(s.getHoraInicio()))
                            .collect(Collectors.toList());
                    slotHorarioRepository.deleteAll(aBorrar);
                }
            }
        }
    }

    private void validarNoColisionSlots(UUID doctorId, DiaSemana diaSemana, LocalDate fecha, LocalDate fechaDesde, LocalDate fechaHasta, LocalTime horaInicio, LocalTime horaFin, int duracionMinutos) {
        List<LocalDate> fechasAEvaluar = new ArrayList<>();
        if (fecha != null) {
            fechasAEvaluar.add(fecha);
        } else if (diaSemana != null) {
            LocalDate inicio = (fechaDesde != null && !fechaDesde.isBefore(LocalDate.now())) ? fechaDesde : LocalDate.now();
            LocalDate fin = (fechaHasta != null) ? fechaHasta : inicio.plusWeeks(8);
            for (LocalDate date = inicio; !date.isAfter(fin); date = date.plusDays(1)) {
                if (mapearDiaSemana(date.getDayOfWeek()) == diaSemana) {
                    fechasAEvaluar.add(date);
                }
            }
        }

        int duracion = duracionMinutos > 0 ? duracionMinutos : 30;

        List<LocalTime[]> rangosPropuestos = new ArrayList<>();
        LocalTime actual = horaInicio;
        while (actual.plusMinutes(duracion).isBefore(horaFin) || actual.plusMinutes(duracion).equals(horaFin)) {
            rangosPropuestos.add(new LocalTime[]{actual, actual.plusMinutes(duracion)});
            actual = actual.plusMinutes(duracion);
        }

        if (rangosPropuestos.isEmpty()) {
            throw new IllegalArgumentException("El rango de horas ingresado es menor a la duración del turno (" + duracion + " minutos).");
        }

        for (LocalDate dateTarget : fechasAEvaluar) {
            List<SlotHorario> slotsExistentes = slotHorarioRepository.findByDoctorIdAndFecha(doctorId, dateTarget);

            for (LocalTime[] rango : rangosPropuestos) {
                LocalTime pStart = rango[0];
                LocalTime pEnd = rango[1];

                for (SlotHorario slotExist : slotsExistentes) {
                    if (pStart.isBefore(slotExist.getHoraFin()) && pEnd.isAfter(slotExist.getHoraInicio())) {
                        throw new IllegalArgumentException(String.format(
                                "El horario ingresado (%s hs a %s hs para el día %s) colisiona con un turno ya existente (%s hs a %s hs). Por favor seleccione un horario libre.",
                                pStart, pEnd, dateTarget, slotExist.getHoraInicio(), slotExist.getHoraFin()
                        ));
                    }
                }
            }
        }
    }

    private void instanciarSlotsSemanales(Doctor doctor, DiaSemana diaSemana, LocalTime horaInicio, LocalTime horaFin, int duracionMinutos, Especialidad especialidad, LocalDate fechaDesde, LocalDate fechaHasta) {
        LocalDate inicio = (fechaDesde != null && !fechaDesde.isBefore(LocalDate.now())) ? fechaDesde : LocalDate.now();
        LocalDate fin = (fechaHasta != null) ? fechaHasta : inicio.plusWeeks(8);

        for (LocalDate date = inicio; !date.isAfter(fin); date = date.plusDays(1)) {
            if (mapearDiaSemana(date.getDayOfWeek()) == diaSemana) {
                generarSlotsParaFecha(doctor, date, horaInicio, horaFin, duracionMinutos, especialidad, false);
            }
        }
    }

    private void generarSlotsParaFecha(Doctor doctor, LocalDate fecha, LocalTime horaInicio, LocalTime horaFin, int duracionMinutos, Especialidad especialidad, boolean esPuntual) {
        List<SlotHorario> existentes = slotHorarioRepository.findByDoctorIdAndFecha(doctor.getId(), fecha);

        if (esPuntual) {
            // Horario puntual sobreescribe cualquier slot existente en ese rango
            List<SlotHorario> colisionantes = existentes.stream()
                    .filter(s -> horaInicio.isBefore(s.getHoraFin()) && horaFin.isAfter(s.getHoraInicio()))
                    .collect(Collectors.toList());
            if (!colisionantes.isEmpty()) {
                slotHorarioRepository.deleteAll(colisionantes);
            }
        } else {
            // Horario semanal borra solo los slots semanales previos colisionantes
            List<SlotHorario> semanalesColisionantes = existentes.stream()
                    .filter(s -> !s.isEsPuntual() && horaInicio.isBefore(s.getHoraFin()) && horaFin.isAfter(s.getHoraInicio()))
                    .collect(Collectors.toList());
            if (!semanalesColisionantes.isEmpty()) {
                slotHorarioRepository.deleteAll(semanalesColisionantes);
            }
        }

        // Obtener remanentes actualizados
        List<SlotHorario> remanentes = slotHorarioRepository.findByDoctorIdAndFecha(doctor.getId(), fecha);

        LocalTime actual = horaInicio;
        int duracion = duracionMinutos > 0 ? duracionMinutos : 30;

        List<SlotHorario> nuevosSlots = new ArrayList<>();
        while (actual.plusMinutes(duracion).isBefore(horaFin) || actual.plusMinutes(duracion).equals(horaFin)) {
            LocalTime slotStart = actual;
            LocalTime slotEnd = actual.plusMinutes(duracion);

            // Si es semanal, no chocar con un slot puntual existente
            boolean chocaConPuntual = !esPuntual && remanentes.stream()
                    .anyMatch(s -> s.isEsPuntual() && slotStart.isBefore(s.getHoraFin()) && slotEnd.isAfter(s.getHoraInicio()));

            if (!chocaConPuntual) {
                SlotHorario slot = SlotHorario.builder()
                        .doctor(doctor)
                        .especialidad(especialidad)
                        .fecha(fecha)
                        .horaInicio(slotStart)
                        .horaFin(slotEnd)
                        .duracionMinutos(duracion)
                        .esPuntual(esPuntual)
                        .build();
                nuevosSlots.add(slot);
            }

            actual = actual.plusMinutes(duracion);
        }

        if (!nuevosSlots.isEmpty()) {
            slotHorarioRepository.saveAll(nuevosSlots);
        }
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
