package com.consultorio.service;

import com.consultorio.domain.*;
import com.consultorio.dto.CrearSlotIndividualDTO;
import com.consultorio.dto.HorarioAtencionDTO;
import com.consultorio.repository.DoctorRepository;
import com.consultorio.repository.HorarioAtencionRepository;
import com.consultorio.repository.SlotHorarioRepository;
import com.consultorio.repository.TurnoRepository;
import com.consultorio.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DoctorAgendaService {

    private final HorarioAtencionRepository horarioAtencionRepository;
    private final SlotHorarioRepository slotHorarioRepository;
    private final DoctorRepository doctorRepository;
    private final TurnoRepository turnoRepository;
    private final EspecialidadService especialidadService;
    private final SecurityUtils securityUtils;
    private final AuditLogService auditLogService;
    private final Clock clock;

    @Autowired
    public DoctorAgendaService(HorarioAtencionRepository horarioAtencionRepository,
                               SlotHorarioRepository slotHorarioRepository,
                               DoctorRepository doctorRepository,
                               TurnoRepository turnoRepository,
                               EspecialidadService especialidadService,
                               SecurityUtils securityUtils,
                               AuditLogService auditLogService,
                               Clock clock) {
        this.horarioAtencionRepository = horarioAtencionRepository;
        this.slotHorarioRepository = slotHorarioRepository;
        this.doctorRepository = doctorRepository;
        this.turnoRepository = turnoRepository;
        this.especialidadService = especialidadService;
        this.securityUtils = securityUtils;
        this.auditLogService = auditLogService;
        this.clock = clock;
    }

    @Transactional
    public HorarioAtencion agregarHorarioAtencion(UUID doctorId, HorarioAtencionDTO dto) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new IllegalArgumentException("Doctor no encontrado con ID: " + doctorId));
        securityUtils.verificarAccesoDoctor(doctor, false);
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
        securityUtils.verificarAccesoDoctor(horario.getDoctor(), false);

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
        Doctor doctor = doctorRepository.findById(doctorId).orElse(null);
        securityUtils.verificarAccesoDoctor(doctor, false);
        return horarioAtencionRepository.findByDoctorId(doctorId);
    }

    @Transactional
    public void eliminarHorarioAtencion(Long horarioId) {
        HorarioAtencion horario = horarioAtencionRepository.findById(horarioId).orElse(null);
        securityUtils.verificarAccesoDoctor(horario != null ? horario.getDoctor() : null, false);
        if (horario != null) {
            eliminarSlotsDeHorario(horario);
            horarioAtencionRepository.delete(horario);
        }
    }

    @Transactional
    public void limpiarHorariosSemana(UUID doctorId, LocalDate desde, LocalDate hasta) {
        Doctor doctor = doctorRepository.findById(doctorId).orElse(null);
        securityUtils.verificarAccesoDoctor(doctor, false);
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
        Doctor doctor = doctorRepository.findById(doctorId).orElse(null);
        securityUtils.verificarAccesoDoctor(doctor, false);
        if (desde != null && hasta != null) {
            return slotHorarioRepository.findByDoctorIdAndFechaBetween(doctorId, desde, hasta);
        }
        return slotHorarioRepository.findByDoctorId(doctorId);
    }

    @Transactional(readOnly = true)
    public List<SlotHorario> obtenerSlotsDoctorPorFecha(UUID doctorId, LocalDate fecha) {
        return slotHorarioRepository.findByDoctorIdAndFecha(doctorId, fecha);
    }

    @Transactional(readOnly = true)
    public List<SlotHorario> obtenerSlotsDoctorDesdeOrdenados(UUID doctorId, LocalDate fechaDesde) {
        return slotHorarioRepository.findFuturosPorDoctorOrdenados(doctorId, fechaDesde);
    }

    @Transactional
    public SlotHorario crearSlotIndividual(UUID doctorId, CrearSlotIndividualDTO dto) {
        Doctor doctorCheck = doctorRepository.findById(doctorId).orElse(null);
        securityUtils.verificarAccesoDoctor(doctorCheck, true);
        if (doctorCheck == null) {
            throw new IllegalArgumentException("Doctor no encontrado con ID: " + doctorId);
        }

        Doctor doctor = doctorRepository.findByIdWithLock(doctorId)
                .orElseThrow(() -> new IllegalArgumentException("Doctor no encontrado con ID: " + doctorId));
        if (!doctor.isDisponibleParaTurnos()) {
            throw new IllegalStateException("El profesional no está habilitado para recibir turnos.");
        }

        if (dto.getHoraInicio().getSecond() != 0 || dto.getHoraInicio().getNano() != 0) {
            throw new IllegalArgumentException("La hora de inicio no debe incluir segundos.");
        }

        int duracion = dto.getDuracionMinutos() != null ? dto.getDuracionMinutos() : 30;
        if (duracion < 5 || duracion > 240) {
            throw new IllegalArgumentException("La duración del turno debe estar entre 5 y 240 minutos.");
        }

        LocalDateTime fechaHoraInicio = LocalDateTime.of(dto.getFecha(), dto.getHoraInicio());
        if (fechaHoraInicio.isBefore(LocalDateTime.now(clock))) {
            throw new IllegalArgumentException("No se pueden crear horarios en fechas u horas pasadas.");
        }

        LocalTime horaFin = dto.getHoraInicio().plusMinutes(duracion);
        if (!horaFin.isAfter(dto.getHoraInicio())) {
            throw new IllegalArgumentException("El horario excede el límite del día (00:00 hs).");
        }

        Especialidad especialidad = null;
        if (dto.getEspecialidadId() != null) {
            especialidad = especialidadService.buscarPorId(dto.getEspecialidadId())
                    .orElseThrow(() -> new IllegalArgumentException("Especialidad no encontrada con ID: " + dto.getEspecialidadId()));
            Long espId = especialidad.getId();
            boolean perteneceAlDoctor = doctor.getEspecialidades() != null
                    && doctor.getEspecialidades().stream().anyMatch(e -> espId.equals(e.getId()));
            if (!perteneceAlDoctor) {
                throw new IllegalArgumentException("El profesional no atiende la especialidad seleccionada.");
            }
        }

        List<SlotHorario> slotsExistentes = slotHorarioRepository.findByDoctorIdAndFecha(doctorId, dto.getFecha());
        buscarSlotColisionante(slotsExistentes, dto.getHoraInicio(), horaFin).ifPresent(slotExist -> {
            throw new IllegalStateException(formatearMensajeColision(dto.getHoraInicio(), horaFin, dto.getFecha(), slotExist));
        });

        LocalDateTime fechaHoraFin = LocalDateTime.of(dto.getFecha(), horaFin);
        boolean colisionaTurnoActivo = turnoRepository
                .existsByDoctorIdAndFechaHoraGreaterThanEqualAndFechaHoraLessThanAndEstadoNot(
                        doctorId, fechaHoraInicio, fechaHoraFin, EstadoTurno.CANCELADO);
        if (colisionaTurnoActivo) {
            throw new IllegalStateException("Ya existe un turno activo reservado dentro de ese rango horario.");
        }

        SlotHorario guardado = slotHorarioRepository.save(
                construirSlot(doctor, especialidad, dto.getFecha(), dto.getHoraInicio(), horaFin, duracion, true)
        );

        auditLogService.record(
                "SLOT_CREATED",
                "SUCCESS",
                securityUtils.obtenerIdUsuarioAutenticado(),
                resolverRolActor(),
                "SLOT",
                guardado.getId() != null ? guardado.getId().toString() : null,
                String.format("doctor=%s fecha=%s hora=%s duracion=%d", doctor.getId(), dto.getFecha(), dto.getHoraInicio(), duracion)
        );

        return guardado;
    }

    @Transactional
    public void eliminarSlotIndividual(Long slotId) {
        SlotHorario slot = slotHorarioRepository.findById(slotId).orElse(null);
        securityUtils.verificarAccesoDoctor(slot != null ? slot.getDoctor() : null, true);
        if (slot != null) {
            slotHorarioRepository.delete(slot);
        }
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

                buscarSlotColisionante(slotsExistentes, pStart, pEnd).ifPresent(slotExist -> {
                    throw new IllegalArgumentException(formatearMensajeColision(pStart, pEnd, dateTarget, slotExist));
                });
            }
        }
    }

    private Optional<SlotHorario> buscarSlotColisionante(List<SlotHorario> slotsExistentes, LocalTime horaInicio, LocalTime horaFin) {
        return slotsExistentes.stream()
                .filter(slotExist -> horaInicio.isBefore(slotExist.getHoraFin()) && horaFin.isAfter(slotExist.getHoraInicio()))
                .findFirst();
    }

    private String formatearMensajeColision(LocalTime horaInicio, LocalTime horaFin, LocalDate fecha, SlotHorario slotExist) {
        return String.format(
                "El horario ingresado (%s hs a %s hs para el día %s) colisiona con un turno ya existente (%s hs a %s hs). Por favor seleccione un horario libre.",
                horaInicio, horaFin, fecha, slotExist.getHoraInicio(), slotExist.getHoraFin()
        );
    }

    private SlotHorario construirSlot(Doctor doctor, Especialidad especialidad, LocalDate fecha,
                                      LocalTime horaInicio, LocalTime horaFin, int duracionMinutos, boolean esPuntual) {
        return SlotHorario.builder()
                .doctor(doctor)
                .especialidad(especialidad)
                .fecha(fecha)
                .horaInicio(horaInicio)
                .horaFin(horaFin)
                .duracionMinutos(duracionMinutos)
                .esPuntual(esPuntual)
                .build();
    }

    private String resolverRolActor() {
        if (securityUtils.esAdmin()) {
            return "ADMIN";
        }
        if (securityUtils.esSecretaria()) {
            return "SECRETARIA";
        }
        return "DOCTOR";
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
                nuevosSlots.add(construirSlot(doctor, especialidad, fecha, slotStart, slotEnd, duracion, esPuntual));
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
