package com.consultorio.service;

import com.consultorio.adapter.CalendarioAdapter;
import com.consultorio.domain.*;
import com.consultorio.dto.TurnoReservaDTO;
import com.consultorio.dto.TurnoResponseDTO;
import com.consultorio.repository.*;
import com.consultorio.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TurnoService {

    private static final Logger log = LoggerFactory.getLogger(TurnoService.class);

    private final TurnoRepository turnoRepository;
    private final PacienteRepository pacienteRepository;
    private final DoctorRepository doctorRepository;
    private final EspecialidadRepository especialidadRepository;
    private final UsuarioRepository usuarioRepository;
    private final EmailService emailService;
    private final SecurityUtils securityUtils;
    private final CalendarioAdapter calendarioAdapter;

    @Autowired
    public TurnoService(TurnoRepository turnoRepository,
                        PacienteRepository pacienteRepository,
                        DoctorRepository doctorRepository,
                        EspecialidadRepository especialidadRepository,
                        UsuarioRepository usuarioRepository,
                        EmailService emailService,
                        SecurityUtils securityUtils,
                        CalendarioAdapter calendarioAdapter) {
        this.turnoRepository = turnoRepository;
        this.pacienteRepository = pacienteRepository;
        this.doctorRepository = doctorRepository;
        this.especialidadRepository = especialidadRepository;
        this.usuarioRepository = usuarioRepository;
        this.emailService = emailService;
        this.securityUtils = securityUtils;
        this.calendarioAdapter = calendarioAdapter;
    }

    @Transactional
    public TurnoResponseDTO reservarTurno(TurnoReservaDTO dto) {
        if (dto.getFechaHora() == null || dto.getFechaHora().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("No se pueden reservar turnos para fechas o momentos del pasado.");
        }

        Paciente paciente = pacienteRepository.findById(dto.getPacienteId())
                .orElseThrow(() -> new IllegalArgumentException("Paciente no encontrado"));

        Doctor doctor = doctorRepository.findById(dto.getDoctorId())
                .orElseThrow(() -> new IllegalArgumentException("Doctor no encontrado"));

        Especialidad especialidad = especialidadRepository.findById(dto.getEspecialidadId())
                .orElseThrow(() -> new IllegalArgumentException("Especialidad no encontrada"));

        // Control de Seguridad IDOR: El paciente autenticado solo puede reservar para sí mismo (salvo ADMIN)
        String emailAutenticado = securityUtils.obtenerEmailUsuarioAutenticado();
        if (emailAutenticado == null) {
            throw new AccessDeniedException("Debe incluir un Token JWT válido en el encabezado Authorization para realizar esta acción.");
        }

        boolean esAdmin = esUsuarioAdmin(emailAutenticado);
        boolean esSuPaciente = paciente.getUsuario() != null && paciente.getUsuario().getEmail().equalsIgnoreCase(emailAutenticado);
        boolean esTutor = paciente.getTutor() != null && paciente.getTutor().getUsuario() != null && paciente.getTutor().getUsuario().getEmail().equalsIgnoreCase(emailAutenticado);

        if (!esAdmin && !esSuPaciente && !esTutor) {
            throw new AccessDeniedException("Acceso denegado: No tiene permisos para agendar turnos a nombre de este paciente.");
        }

        // Validar que el doctor efectivamente ejerza esa especialidad
        boolean atiendeEspecialidad = doctor.getEspecialidades().stream()
                .anyMatch(e -> e.getId().equals(especialidad.getId()));

        if (!atiendeEspecialidad) {
            throw new IllegalArgumentException("El doctor " + doctor.getNombre() + " " + doctor.getApellido() +
                    " no atiende la especialidad seleccionada (" + especialidad.getNombre() + ").");
        }

        // Validar si la fechaHora no está ocupada
        boolean ocupado = turnoRepository.existsByDoctorIdAndFechaHoraAndEstadoNot(
                doctor.getId(), dto.getFechaHora(), EstadoTurno.CANCELADO);

        if (ocupado) {
            throw new IllegalStateException("El horario seleccionado ya no se encuentra disponible.");
        }

        boolean tieneOS = dto.getTieneObraSocial() != null && dto.getTieneObraSocial();
        String osNombre = tieneOS
                ? (dto.getObraSocial() != null && !dto.getObraSocial().trim().isEmpty() ? dto.getObraSocial().trim() : "Obra Social")
                : "Particular / Sin Obra Social";

        Turno turno = Turno.builder()
                .paciente(paciente)
                .doctor(doctor)
                .especialidad(especialidad)
                .fechaHora(dto.getFechaHora())
                .estado(EstadoTurno.CONFIRMADO)
                .motivoConsulta(dto.getMotivoConsulta())
                .tieneObraSocial(tieneOS)
                .obraSocial(osNombre)
                .build();

        Turno guardado = turnoRepository.save(turno);

        // Sincronizar en el Google Calendar personal del Paciente (si lo tiene vinculado)
        try {
            if (paciente.getUsuario() != null && paciente.getUsuario().isGoogleCalendarConnected()) {
                String eventIdPaciente = calendarioAdapter.agendarEventoParaUsuario(guardado, paciente.getUsuario());
                if (eventIdPaciente != null) {
                    guardado.setGoogleEventId(eventIdPaciente);
                }
            }
        } catch (Exception e) {
            log.error("No se pudo agendar en el Google Calendar del paciente: {}", e.getMessage());
        }

        // Sincronizar en el Google Calendar personal del Doctor (si lo tiene vinculado)
        try {
            if (doctor.getUsuario().isGoogleCalendarConnected()) {
                String eventIdDoctor = calendarioAdapter.agendarEventoParaUsuario(guardado, doctor.getUsuario());
                if (eventIdDoctor != null) {
                    guardado.setGoogleEventIdDoctor(eventIdDoctor);
                }
            }
        } catch (Exception e) {
            log.error("No se pudo agendar en el Google Calendar del doctor: {}", e.getMessage());
        }

        guardado = turnoRepository.save(guardado);

        TurnoResponseDTO responseDTO = mapearResponseDTO(guardado);

        // Notificación por email al paciente/tutor confirmando la reserva
        String emailDestino = obtenerEmailNotificacionPaciente(guardado.getPaciente());
        if (emailDestino != null) {
            emailService.enviarConfirmacionTurno(emailDestino, responseDTO);
        }

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

        boolean esAdmin = esUsuarioAdmin(emailAutenticado);
        boolean esSuPaciente = turno.getPaciente().getUsuario() != null && turno.getPaciente().getUsuario().getEmail().equalsIgnoreCase(emailAutenticado);
        boolean esTutor = turno.getPaciente().getTutor() != null && turno.getPaciente().getTutor().getUsuario() != null && turno.getPaciente().getTutor().getUsuario().getEmail().equalsIgnoreCase(emailAutenticado);
        boolean esSuDoctor = turno.getDoctor().getUsuario().getEmail().equalsIgnoreCase(emailAutenticado);

        if (!esAdmin && !esSuPaciente && !esTutor && !esSuDoctor) {
            throw new AccessDeniedException("Acceso denegado: No tiene permisos para cancelar este turno.");
        }

        if (turno.getEstado() == EstadoTurno.COMPLETADO) {
            throw new IllegalStateException("No se puede cancelar un turno que ya fue completado.");
        }

        turno.setEstado(EstadoTurno.CANCELADO);
        Turno actualizado = turnoRepository.save(turno);

        // Cancelar evento en el Google Calendar del Paciente (si posee cuenta propia vinculada)
        if (actualizado.getGoogleEventId() != null && actualizado.getPaciente().getUsuario() != null) {
            try {
                calendarioAdapter.cancelarEventoParaUsuario(actualizado.getGoogleEventId(), actualizado.getPaciente().getUsuario());
            } catch (Exception e) {
                log.error("No se pudo cancelar el evento en el calendario del paciente: {}", e.getMessage());
            }
        }

        // Cancelar evento en el Google Calendar del Doctor
        if (actualizado.getGoogleEventIdDoctor() != null && actualizado.getDoctor().getUsuario() != null) {
            try {
                calendarioAdapter.cancelarEventoParaUsuario(actualizado.getGoogleEventIdDoctor(), actualizado.getDoctor().getUsuario());
            } catch (Exception e) {
                log.error("No se pudo cancelar el evento en el calendario del doctor: {}", e.getMessage());
            }
        }

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

        boolean esAdmin = esUsuarioAdmin(emailAutenticado);
        boolean esSuDoctor = turno.getDoctor().getUsuario().getEmail().equalsIgnoreCase(emailAutenticado);

        if (!esAdmin && !esSuDoctor) {
            throw new AccessDeniedException("Acceso denegado: Solo el médico asignado o un Administrador pueden justificar y cancelar este turno.");
        }

        if (turno.getEstado() == EstadoTurno.COMPLETADO) {
            throw new IllegalStateException("No se puede cancelar un turno que ya fue completado.");
        }

        turno.setEstado(EstadoTurno.CANCELADO);
        turno.setMotivoCancelacion(motivoCancelacion.trim());
        Turno actualizado = turnoRepository.save(turno);

        // Cancelar eventos de Google Calendar si existían
        if (actualizado.getGoogleEventId() != null && actualizado.getPaciente().getUsuario() != null) {
            try {
                calendarioAdapter.cancelarEventoParaUsuario(actualizado.getGoogleEventId(), actualizado.getPaciente().getUsuario());
            } catch (Exception e) {
                log.error("No se pudo cancelar el evento en el calendario del paciente: {}", e.getMessage());
            }
        }
        if (actualizado.getGoogleEventIdDoctor() != null && actualizado.getDoctor().getUsuario() != null) {
            try {
                calendarioAdapter.cancelarEventoParaUsuario(actualizado.getGoogleEventIdDoctor(), actualizado.getDoctor().getUsuario());
            } catch (Exception e) {
                log.error("No se pudo cancelar el evento en el calendario del doctor: {}", e.getMessage());
            }
        }

        TurnoResponseDTO dto = mapearResponseDTO(actualizado);

        // Notificación por correo electrónico al paciente/tutor con disculpas y la justificación obligatoria
        String emailDestino = obtenerEmailNotificacionPaciente(actualizado.getPaciente());

        if (emailDestino != null) {
            try {
                emailService.enviarEmailCancelacionDoctor(
                        emailDestino,
                        dto,
                        motivoCancelacion.trim()
                );
            } catch (Exception e) {
                log.error("Error al enviar email de cancelación por médico: {}", e.getMessage());
            }
        }

        return dto;
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
        Paciente paciente = pacienteRepository.findById(pacienteId)
                .orElseThrow(() -> new IllegalArgumentException("Paciente no encontrado"));

        // Control de Seguridad IDOR: El paciente solo puede listar sus propios turnos (salvo ADMIN)
        String emailAutenticado = securityUtils.obtenerEmailUsuarioAutenticado();
        if (emailAutenticado == null) {
            throw new AccessDeniedException("Debe incluir un Token JWT válido en el encabezado Authorization para realizar esta acción.");
        }

        boolean esAdmin = esUsuarioAdmin(emailAutenticado);
        boolean esSuPaciente = paciente.getUsuario() != null && paciente.getUsuario().getEmail().equalsIgnoreCase(emailAutenticado);
        boolean esTutor = paciente.getTutor() != null && paciente.getTutor().getUsuario() != null && paciente.getTutor().getUsuario().getEmail().equalsIgnoreCase(emailAutenticado);

        if (!esAdmin && !esSuPaciente && !esTutor) {
            throw new AccessDeniedException("Acceso denegado: No tiene permiso para consultar los turnos de este paciente.");
        }

        return turnoRepository.findByPacienteIdOrderByFechaHoraDesc(pacienteId)
                .stream()
                .map(this::mapearResponseDTO)
                .collect(Collectors.toList());
    }

    public List<TurnoResponseDTO> obtenerAgendaDoctor(UUID doctorId, LocalDate fecha) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new IllegalArgumentException("Doctor no encontrado con id: " + doctorId));

        String emailAutenticado = securityUtils.obtenerEmailUsuarioAutenticado();
        if (emailAutenticado == null) {
            throw new AccessDeniedException("Debe incluir un Token JWT válido en el encabezado Authorization para realizar esta acción.");
        }

        boolean esAdmin = esUsuarioAdmin(emailAutenticado);
        if (!esAdmin && !doctor.getUsuario().getEmail().equalsIgnoreCase(emailAutenticado)) {
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

    public List<TurnoResponseDTO> obtenerTurnosRangoDoctor(UUID doctorId, LocalDate desde, LocalDate hasta) {
        LocalDateTime inicio = desde.atStartOfDay();
        LocalDateTime fin = hasta.atTime(23, 59, 59);

        return turnoRepository.findByDoctorIdAndFechaHoraBetweenOrderByFechaHoraAsc(doctorId, inicio, fin)
                .stream()
                .map(this::mapearResponseDTO)
                .collect(Collectors.toList());
    }

    private boolean esUsuarioAdmin(String email) {
        return usuarioRepository.findByEmail(email)
                .map(u -> u.getRol() == Rol.ADMIN)
                .orElse(false);
    }

    private String obtenerEmailNotificacionPaciente(Paciente paciente) {
        if (paciente == null) return null;

        if (paciente.getUsuario() != null && paciente.getUsuario().getEmail() != null && !paciente.getUsuario().getEmail().trim().isEmpty()) {
            return paciente.getUsuario().getEmail();
        }

        Paciente pacientePersistido = pacienteRepository.findById(paciente.getId()).orElse(paciente);
        if (pacientePersistido.getUsuario() != null && pacientePersistido.getUsuario().getEmail() != null) {
            return pacientePersistido.getUsuario().getEmail();
        }

        if (pacientePersistido.getTutor() != null && pacientePersistido.getTutor().getId() != null) {
            Paciente tutorCompleto = pacienteRepository.findById(pacientePersistido.getTutor().getId()).orElse(null);
            if (tutorCompleto != null && tutorCompleto.getUsuario() != null && tutorCompleto.getUsuario().getEmail() != null) {
                return tutorCompleto.getUsuario().getEmail();
            }
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
                .especialidadNombre(turno.getEspecialidad().getNombre())
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
