package com.consultorio.service;

import com.consultorio.domain.*;
import com.consultorio.dto.PacienteResumenEstadisticasDTO;
import com.consultorio.dto.TurnoReservaDTO;
import com.consultorio.dto.TurnoReservaSecretariaDTO;
import com.consultorio.dto.TurnoResponseDTO;
import com.consultorio.event.TurnoCanceladoEvent;
import com.consultorio.event.TurnoReservadoEvent;
import com.consultorio.repository.TurnoRepository;
import com.consultorio.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TurnoService {

    private static final Logger log = LoggerFactory.getLogger(TurnoService.class);

    private final TurnoRepository turnoRepository;
    private final PacienteService pacienteService;
    private final DoctorService doctorService;
    private final EspecialidadService especialidadService;
    private final SecurityUtils securityUtils;
    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    public TurnoService(TurnoRepository turnoRepository,
                        PacienteService pacienteService,
                        DoctorService doctorService,
                        EspecialidadService especialidadService,
                        SecurityUtils securityUtils,
                        ApplicationEventPublisher eventPublisher) {
        this.turnoRepository = turnoRepository;
        this.pacienteService = pacienteService;
        this.doctorService = doctorService;
        this.especialidadService = especialidadService;
        this.securityUtils = securityUtils;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public TurnoResponseDTO reservarTurno(TurnoReservaDTO dto) {
        if (dto.getFechaHora() == null || dto.getFechaHora().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("No se pueden reservar turnos para fechas o momentos del pasado.");
        }

        Paciente paciente = pacienteService.obtenerPorId(dto.getPacienteId());
        Doctor doctor = doctorService.obtenerPorId(dto.getDoctorId());
        Especialidad especialidad = especialidadService.obtenerPorId(dto.getEspecialidadId());

        // Control de Seguridad IDOR: El paciente autenticado solo puede reservar para sí mismo (salvo ADMIN)
        String emailAutenticado = securityUtils.obtenerEmailUsuarioAutenticado();
        if (emailAutenticado == null) {
            throw new AccessDeniedException("Debe incluir un Token JWT válido en el encabezado Authorization para realizar esta acción.");
        }

        boolean esAdmin = securityUtils.esAdmin();
        boolean esSecretaria = securityUtils.esSecretaria();
        boolean esSuPaciente = paciente.getUsuario() != null && paciente.getUsuario().getEmail().equalsIgnoreCase(emailAutenticado);
        boolean esTutor = paciente.getTutor() != null && paciente.getTutor().getUsuario() != null && paciente.getTutor().getUsuario().getEmail().equalsIgnoreCase(emailAutenticado);

        if (!esAdmin && !esSecretaria && !esSuPaciente && !esTutor) {
            throw new AccessDeniedException("Acceso denegado: No tiene permisos para agendar turnos a nombre de este paciente.");
        }

        Turno turno = validarYConstruirTurno(doctor, especialidad, paciente, dto.getFechaHora(), dto.getTieneObraSocial(), dto.getObraSocial(), dto.getMotivoConsulta());

        Turno guardado;
        try {
            guardado = turnoRepository.save(turno);
        } catch (org.springframework.dao.DataIntegrityViolationException dive) {
            throw new IllegalStateException("El horario seleccionado acaba de ser reservado por otro usuario. Por favor elija otro turno disponible.");
        }

        TurnoResponseDTO responseDTO = mapearResponseDTO(guardado);

        String emailDestino = obtenerEmailNotificacionPaciente(guardado.getPaciente());
        eventPublisher.publishEvent(new TurnoReservadoEvent(guardado.getId(), emailDestino));

        return responseDTO;
    }

    @Transactional
    public TurnoResponseDTO reservarTurnoSecretaria(TurnoReservaSecretariaDTO dto) {
        if (dto.getFechaHora() == null || dto.getFechaHora().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("No se pueden reservar turnos para fechas o momentos del pasado.");
        }

        Doctor doctor = doctorService.obtenerPorId(dto.getDoctorId());
        Especialidad especialidad = especialidadService.obtenerPorId(dto.getEspecialidadId());

        Paciente paciente = pacienteService.obtenerORegistrarPacienteExpress(
                dto.getNombre(), dto.getApellido(), dto.getDni(), dto.getTelefono(), dto.getEmail(), dto.getFechaNacimiento()
        );

        Turno turno = validarYConstruirTurno(doctor, especialidad, paciente, dto.getFechaHora(), dto.getTieneObraSocial(), dto.getObraSocial(), dto.getMotivoConsulta());

        Turno guardado;
        try {
            guardado = turnoRepository.save(turno);
        } catch (org.springframework.dao.DataIntegrityViolationException dive) {
            throw new IllegalStateException("El horario seleccionado acaba de ser reservado por otro usuario. Por favor elija otro turno disponible.");
        }

        TurnoResponseDTO responseDTO = mapearResponseDTO(guardado);

        String emailDestino = (dto.getEmail() != null && !dto.getEmail().trim().isEmpty())
                ? dto.getEmail().trim()
                : obtenerEmailNotificacionPaciente(guardado.getPaciente());

        eventPublisher.publishEvent(new TurnoReservadoEvent(guardado.getId(), emailDestino));

        return responseDTO;
    }

    @Transactional
    public TurnoResponseDTO cancelarTurno(UUID turnoId) {
        Turno turno = turnoRepository.findById(turnoId)
                .orElseThrow(() -> new IllegalArgumentException("Turno no encontrado con ID: " + turnoId));

        // Control de Seguridad IDOR: Solo el paciente, su tutor, doctor dueño del turno o ADMIN pueden cancelarlo
        String emailAutenticado = securityUtils.obtenerEmailUsuarioAutenticado();
        if (emailAutenticado == null) {
            throw new AccessDeniedException("Debe incluir un Token JWT válido en el encabezado Authorization para realizar esta acción.");
        }

        boolean esAdmin = securityUtils.esAdmin();
        boolean esSecretaria = securityUtils.esSecretaria();
        boolean esSuPaciente = turno.getPaciente().getUsuario() != null && turno.getPaciente().getUsuario().getEmail().equalsIgnoreCase(emailAutenticado);
        boolean esTutor = turno.getPaciente().getTutor() != null && turno.getPaciente().getTutor().getUsuario() != null && turno.getPaciente().getTutor().getUsuario().getEmail().equalsIgnoreCase(emailAutenticado);
        boolean esSuDoctor = turno.getDoctor().getUsuario().getEmail().equalsIgnoreCase(emailAutenticado);

        if (!esAdmin && !esSecretaria && !esSuPaciente && !esTutor && !esSuDoctor) {
            throw new AccessDeniedException("Acceso denegado: No tiene permisos para cancelar este turno.");
        }

        if (turno.getEstado() == EstadoTurno.COMPLETADO) {
            throw new IllegalStateException("No se puede cancelar un turno que ya fue completado.");
        }

        turno.setEstado(EstadoTurno.CANCELADO);
        Turno actualizado = turnoRepository.save(turno);

        eventPublisher.publishEvent(new TurnoCanceladoEvent(actualizado.getId(), null, false));

        return mapearResponseDTO(actualizado);
    }

    @Transactional
    public TurnoResponseDTO cancelarTurnoPorDoctor(UUID turnoId, String motivoCancelacion) {
        if (motivoCancelacion == null || motivoCancelacion.trim().isEmpty()) {
            throw new IllegalArgumentException("Debe ingresar una justificación obligatoria para cancelar el turno del paciente.");
        }

        Turno turno = turnoRepository.findById(turnoId)
                .orElseThrow(() -> new IllegalArgumentException("Turno no encontrado con ID: " + turnoId));

        String emailAutenticado = securityUtils.obtenerEmailUsuarioAutenticado();
        if (emailAutenticado == null) {
            throw new AccessDeniedException("Debe incluir un Token JWT válido en el encabezado Authorization para realizar esta acción.");
        }

        boolean esAdmin = securityUtils.esAdmin();
        boolean esSecretaria = securityUtils.esSecretaria();
        boolean esSuDoctor = turno.getDoctor().getUsuario().getEmail().equalsIgnoreCase(emailAutenticado);

        if (!esAdmin && !esSecretaria && !esSuDoctor) {
            throw new AccessDeniedException("Acceso denegado: Solo el médico asignado, la secretaría o un Administrador pueden justificar y cancelar este turno.");
        }

        if (turno.getEstado() == EstadoTurno.COMPLETADO) {
            throw new IllegalStateException("No se puede cancelar un turno que ya fue completado.");
        }

        turno.setEstado(EstadoTurno.CANCELADO);
        turno.setMotivoCancelacion(motivoCancelacion.trim());
        Turno actualizado = turnoRepository.save(turno);

        eventPublisher.publishEvent(new TurnoCanceladoEvent(actualizado.getId(), motivoCancelacion.trim(), true));

        return mapearResponseDTO(actualizado);
    }

    @Transactional
    public TurnoResponseDTO cambiarEstadoTurno(UUID turnoId, EstadoTurno nuevoEstado) {
        Turno turno = turnoRepository.findById(turnoId)
                .orElseThrow(() -> new IllegalArgumentException("Turno no encontrado con ID: " + turnoId));

        turno.setEstado(nuevoEstado);
        Turno actualizado = turnoRepository.save(turno);
        return mapearResponseDTO(actualizado);
    }

    public List<TurnoResponseDTO> obtenerTurnosPorPaciente(UUID pacienteId) {
        Paciente paciente = pacienteService.obtenerPorId(pacienteId);

        // Control de Seguridad IDOR: El paciente solo puede listar sus propios turnos (salvo ADMIN)
        String emailAutenticado = securityUtils.obtenerEmailUsuarioAutenticado();
        if (emailAutenticado == null) {
            throw new AccessDeniedException("Debe incluir un Token JWT válido en el encabezado Authorization para realizar esta acción.");
        }

        boolean esAdmin = securityUtils.esAdmin();
        boolean esSecretaria = securityUtils.esSecretaria();
        boolean esSuPaciente = paciente.getUsuario() != null && paciente.getUsuario().getEmail().equalsIgnoreCase(emailAutenticado);
        boolean esTutor = paciente.getTutor() != null && paciente.getTutor().getUsuario() != null && paciente.getTutor().getUsuario().getEmail().equalsIgnoreCase(emailAutenticado);

        if (!esAdmin && !esSecretaria && !esSuPaciente && !esTutor) {
            throw new AccessDeniedException("Acceso denegado: No tiene permiso para consultar los turnos de este paciente.");
        }

        return turnoRepository.findByPacienteIdOrderByFechaHoraDesc(pacienteId)
                .stream()
                .map(this::mapearResponseDTO)
                .collect(Collectors.toList());
    }

    public List<TurnoResponseDTO> obtenerAgendaDoctor(UUID doctorId, LocalDate fecha) {
        Doctor doctor = doctorService.obtenerPorId(doctorId);

        String emailAutenticado = securityUtils.obtenerEmailUsuarioAutenticado();
        if (emailAutenticado == null) {
            throw new AccessDeniedException("Debe incluir un Token JWT válido en el encabezado Authorization para realizar esta acción.");
        }

        boolean esAdmin = securityUtils.esAdmin();
        boolean esSecretaria = securityUtils.esSecretaria();
        if (!esAdmin && !esSecretaria && !doctor.getUsuario().getEmail().equalsIgnoreCase(emailAutenticado)) {
            throw new AccessDeniedException("Acceso denegado: No tiene permiso para ver la agenda de otro profesional.");
        }

        LocalDateTime inicio = fecha.atStartOfDay();
        LocalDateTime fin = fecha.atTime(23, 59, 59);

        return turnoRepository.findByDoctorIdAndFechaHoraBetweenAndEstadoNotOrderByFechaHoraAsc(
                        doctorId, inicio, fin, EstadoTurno.CANCELADO)
                .stream()
                .map(this::mapearResponseDTO)
                .sorted((t1, t2) -> {
                    boolean p1 = t1.getEstado() == EstadoTurno.CONFIRMADO || t1.getEstado() == EstadoTurno.PENDIENTE;
                    boolean p2 = t2.getEstado() == EstadoTurno.CONFIRMADO || t2.getEstado() == EstadoTurno.PENDIENTE;
                    if (p1 && !p2) return -1;
                    if (!p1 && p2) return 1;
                    return t1.getFechaHora().compareTo(t2.getFechaHora());
                })
                .collect(Collectors.toList());
    }

    public List<TurnoResponseDTO> obtenerAgendaDoctorInterno(UUID doctorId, LocalDate fecha) {
        LocalDateTime inicio = fecha.atStartOfDay();
        LocalDateTime fin = fecha.atTime(23, 59, 59);

        return turnoRepository.findByDoctorIdAndFechaHoraBetweenAndEstadoNotOrderByFechaHoraAsc(
                        doctorId, inicio, fin, EstadoTurno.CANCELADO)
                .stream()
                .map(this::mapearResponseDTO)
                .collect(Collectors.toList());
    }

    public List<TurnoResponseDTO> obtenerTurnosRangoDoctor(UUID doctorId, LocalDate desde, LocalDate hasta) {
        LocalDateTime inicio = desde.atStartOfDay();
        LocalDateTime fin = hasta.atTime(23, 59, 59);

        return turnoRepository.findByDoctorIdAndFechaHoraBetweenOrderByFechaHoraAsc(doctorId, inicio, fin)
                .stream()
                .map(this::mapearResponseDTO)
                .collect(Collectors.toList());
    }

    public Set<LocalTime> obtenerHorasOcupadasDoctor(UUID doctorId, LocalDate fecha) {
        LocalDateTime inicioDia = fecha.atStartOfDay();
        LocalDateTime finDia = fecha.atTime(23, 59, 59);
        List<Turno> turnosOcupados = turnoRepository.findByDoctorIdAndFechaHoraBetweenAndEstadoNot(
                doctorId, inicioDia, finDia, EstadoTurno.CANCELADO);
        return turnosOcupados.stream()
                .map(t -> t.getFechaHora().toLocalTime())
                .collect(Collectors.toSet());
    }

    @Transactional(readOnly = true)
    public PacienteResumenEstadisticasDTO obtenerEstadisticasPaciente(UUID pacienteId) {
        Paciente paciente = pacienteService.obtenerPorId(pacienteId);

        List<Turno> turnos = turnoRepository.findByPacienteIdOrderByFechaHoraDesc(pacienteId);

        int total = turnos.size();
        int completados = (int) turnos.stream().filter(t -> t.getEstado() == EstadoTurno.COMPLETADO).count();
        int ausentes = (int) turnos.stream().filter(t -> t.getEstado() == EstadoTurno.AUSENTE).count();
        int cancelados = (int) turnos.stream().filter(t -> t.getEstado() == EstadoTurno.CANCELADO).count();
        int pendientes = (int) turnos.stream()
                .filter(t -> t.getEstado() == EstadoTurno.CONFIRMADO || t.getEstado() == EstadoTurno.PENDIENTE).count();

        double pctCompletados = total > 0 ? Math.round((completados * 100.0 / total) * 10.0) / 10.0 : 0.0;
        double pctAusentes = total > 0 ? Math.round((ausentes * 100.0 / total) * 10.0) / 10.0 : 0.0;
        double pctCancelados = total > 0 ? Math.round((cancelados * 100.0 / total) * 10.0) / 10.0 : 0.0;
        double pctPendientes = total > 0 ? Math.round((pendientes * 100.0 / total) * 10.0) / 10.0 : 0.0;

        return PacienteResumenEstadisticasDTO.builder()
                .id(paciente.getId())
                .nombre(paciente.getNombre())
                .apellido(paciente.getApellido())
                .dni(String.valueOf(paciente.getDni()))
                .telefono(paciente.getTelefono())
                .email(paciente.getEmail() != null ? paciente.getEmail() : "")
                .fechaNacimiento(paciente.getFechaNacimiento())
                .edad(paciente.getEdad())
                .totalTurnos(total)
                .totalCompletados(completados)
                .totalAusentes(ausentes)
                .totalCancelados(cancelados)
                .totalPendientes(pendientes)
                .porcentajeCompletados(pctCompletados)
                .porcentajeAusentes(pctAusentes)
                .porcentajeCancelados(pctCancelados)
                .porcentajePendientes(pctPendientes)
                .build();
    }

    private Turno validarYConstruirTurno(Doctor doctor, Especialidad especialidad, Paciente paciente,
                                         LocalDateTime fechaHora, Boolean tieneOS, String osIngresada, String motivo) {
        boolean atiendeEspecialidad = doctor.getEspecialidades().stream()
                .anyMatch(e -> e.getId().equals(especialidad.getId()));

        if (!atiendeEspecialidad) {
            throw new IllegalArgumentException("El doctor " + doctor.getNombre() + " " + doctor.getApellido() +
                    " no atiende la especialidad seleccionada (" + especialidad.getNombre() + ").");
        }

        if (turnoRepository.existsByPacienteIdAndFechaHoraAndEstadoNot(paciente.getId(), fechaHora, EstadoTurno.CANCELADO)) {
            throw new IllegalStateException("El paciente ya posee una cita médica agendada para este mismo día y horario.");
        }

        if (turnoRepository.existsByDoctorIdAndFechaHoraAndEstadoNot(doctor.getId(), fechaHora, EstadoTurno.CANCELADO)) {
            throw new IllegalStateException("El horario seleccionado ya no se encuentra disponible.");
        }

        boolean slotConfigurado = doctor.isDisponibleParaTurnos() &&
                doctorService.existeSlotActivo(doctor.getId(), fechaHora.toLocalDate(), fechaHora.toLocalTime());
        if (!slotConfigurado) {
            throw new IllegalStateException("El horario seleccionado ya no se encuentra configurado o fue deshabilitado por el profesional.");
        }

        boolean poseeOS = tieneOS != null && tieneOS;
        String osNombre = poseeOS
                ? (osIngresada != null && !osIngresada.trim().isEmpty() ? osIngresada.trim() : "Obra Social")
                : "Particular / Sin Obra Social";

        return Turno.builder()
                .paciente(paciente)
                .doctor(doctor)
                .especialidad(especialidad)
                .fechaHora(fechaHora)
                .estado(EstadoTurno.CONFIRMADO)
                .motivoConsulta(motivo)
                .tieneObraSocial(poseeOS)
                .obraSocial(osNombre)
                .build();
    }

    private String obtenerEmailNotificacionPaciente(Paciente paciente) {
        if (paciente == null) return null;

        if (paciente.getUsuario() != null && paciente.getUsuario().getEmail() != null && !paciente.getUsuario().getEmail().trim().isEmpty()) {
            return paciente.getUsuario().getEmail();
        }

        Paciente pacientePersistido = pacienteService.obtenerPorId(paciente.getId());
        if (pacientePersistido.getUsuario() != null && pacientePersistido.getUsuario().getEmail() != null) {
            return pacientePersistido.getUsuario().getEmail();
        }

        if (pacientePersistido.getTutor() != null && pacientePersistido.getTutor().getId() != null) {
            Paciente tutorCompleto = pacienteService.obtenerPorId(pacientePersistido.getTutor().getId());
            if (tutorCompleto != null && tutorCompleto.getUsuario() != null && tutorCompleto.getUsuario().getEmail() != null) {
                return tutorCompleto.getUsuario().getEmail();
            }
        }

        if (pacientePersistido.getEmail() != null && !pacientePersistido.getEmail().trim().isEmpty()) {
            return pacientePersistido.getEmail().trim();
        }

        return null;
    }

    private TurnoResponseDTO mapearResponseDTO(Turno turno) {
        return TurnoResponseDTO.builder()
                .id(turno.getId())
                .pacienteId(turno.getPaciente().getId())
                .pacienteNombre(turno.getPaciente().getNombre() + " " + turno.getPaciente().getApellido())
                .doctorId(turno.getDoctor().getId())
                .doctorNombre(turno.getDoctor().getNombre() + " " + turno.getDoctor().getApellido())
                .especialidadId(turno.getEspecialidad() != null ? turno.getEspecialidad().getId() : null)
                .especialidadNombre(turno.getEspecialidad() != null ? turno.getEspecialidad().getNombre() : null)
                .fechaHora(turno.getFechaHora())
                .estado(turno.getEstado())
                .motivoConsulta(turno.getMotivoConsulta())
                .motivoCancelacion(turno.getMotivoCancelacion())
                .tieneObraSocial(turno.isTieneObraSocial())
                .obraSocial(turno.getObraSocial())
                .googleEventId(turno.getGoogleEventId())
                .build();
    }
}
