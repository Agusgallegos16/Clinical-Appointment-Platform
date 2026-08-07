package com.consultorio.service;

import com.consultorio.domain.*;
import com.consultorio.dto.TurnoReservaDTO;
import com.consultorio.dto.TurnoResponseDTO;
import com.consultorio.repository.*;
import com.consultorio.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TurnoService {

    private final TurnoRepository turnoRepository;
    private final PacienteRepository pacienteRepository;
    private final DoctorRepository doctorRepository;
    private final EspecialidadRepository especialidadRepository;
    private final EmailService emailService;
    private final SecurityUtils securityUtils;

    @Autowired
    public TurnoService(TurnoRepository turnoRepository,
                        PacienteRepository pacienteRepository,
                        DoctorRepository doctorRepository,
                        EspecialidadRepository especialidadRepository,
                        EmailService emailService,
                        SecurityUtils securityUtils) {
        this.turnoRepository = turnoRepository;
        this.pacienteRepository = pacienteRepository;
        this.doctorRepository = doctorRepository;
        this.especialidadRepository = especialidadRepository;
        this.emailService = emailService;
        this.securityUtils = securityUtils;
    }

    @Transactional
    public TurnoResponseDTO reservarTurno(TurnoReservaDTO dto) {
        Paciente paciente = pacienteRepository.findById(dto.getPacienteId())
                .orElseThrow(() -> new IllegalArgumentException("Paciente no encontrado"));

        Doctor doctor = doctorRepository.findById(dto.getDoctorId())
                .orElseThrow(() -> new IllegalArgumentException("Doctor no encontrado"));

        Especialidad especialidad = especialidadRepository.findById(dto.getEspecialidadId())
                .orElseThrow(() -> new IllegalArgumentException("Especialidad no encontrada"));

        // Control de Seguridad IDOR: El paciente autenticado solo puede reservar para sí mismo
        String emailAutenticado = securityUtils.obtenerEmailUsuarioAutenticado();
        if (emailAutenticado == null) {
            throw new AccessDeniedException("Debe incluir un Token JWT válido en el encabezado Authorization para realizar esta acción.");
        }
        if (!paciente.getUsuario().getEmail().equalsIgnoreCase(emailAutenticado)) {
            throw new AccessDeniedException("Acceso denegado: No tiene permisos para agendar turnos a nombre de otro paciente.");
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

        Turno turno = Turno.builder()
                .paciente(paciente)
                .doctor(doctor)
                .especialidad(especialidad)
                .fechaHora(dto.getFechaHora())
                .estado(EstadoTurno.CONFIRMADO)
                .motivoConsulta(dto.getMotivoConsulta())
                .build();

        Turno guardado = turnoRepository.save(turno);
        TurnoResponseDTO responseDTO = mapearResponseDTO(guardado);

        // Notificación por email al paciente confirmando la reserva
        emailService.enviarConfirmacionTurno(paciente.getUsuario().getEmail(), responseDTO);

        return responseDTO;
    }

    @Transactional
    public TurnoResponseDTO cancelarTurno(Long turnoId) {
        Turno turno = turnoRepository.findById(turnoId)
                .orElseThrow(() -> new IllegalArgumentException("Turno no encontrado con ID: " + turnoId));

        // Control de Seguridad IDOR: Solo el paciente o el doctor dueño del turno pueden cancelarlo
        String emailAutenticado = securityUtils.obtenerEmailUsuarioAutenticado();
        if (emailAutenticado == null) {
            throw new AccessDeniedException("Debe incluir un Token JWT válido en el encabezado Authorization para realizar esta acción.");
        }

        boolean esSuPaciente = turno.getPaciente().getUsuario().getEmail().equalsIgnoreCase(emailAutenticado);
        boolean esSuDoctor = turno.getDoctor().getUsuario().getEmail().equalsIgnoreCase(emailAutenticado);

        if (!esSuPaciente && !esSuDoctor) {
            throw new AccessDeniedException("Acceso denegado: No tiene permisos para cancelar este turno.");
        }

        if (turno.getEstado() == EstadoTurno.COMPLETADO) {
            throw new IllegalStateException("No se puede cancelar un turno que ya fue completado.");
        }

        turno.setEstado(EstadoTurno.CANCELADO);
        Turno actualizado = turnoRepository.save(turno);
        return mapearResponseDTO(actualizado);
    }

    @Transactional
    public TurnoResponseDTO cambiarEstadoTurno(Long turnoId, EstadoTurno nuevoEstado) {
        Turno turno = turnoRepository.findById(turnoId)
                .orElseThrow(() -> new IllegalArgumentException("Turno no encontrado con ID: " + turnoId));

        turno.setEstado(nuevoEstado);
        Turno actualizado = turnoRepository.save(turno);
        return mapearResponseDTO(actualizado);
    }

    public List<TurnoResponseDTO> obtenerTurnosPorPaciente(Long pacienteId) {
        Paciente paciente = pacienteRepository.findById(pacienteId)
                .orElseThrow(() -> new IllegalArgumentException("Paciente no encontrado"));

        // Control de Seguridad IDOR: El paciente solo puede listar sus propios turnos
        String emailAutenticado = securityUtils.obtenerEmailUsuarioAutenticado();
        if (emailAutenticado == null) {
            throw new AccessDeniedException("Debe incluir un Token JWT válido en el encabezado Authorization para realizar esta acción.");
        }
        if (!paciente.getUsuario().getEmail().equalsIgnoreCase(emailAutenticado)) {
            throw new AccessDeniedException("Acceso denegado: No tiene permiso para consultar los turnos de otro paciente.");
        }

        return turnoRepository.findByPacienteIdOrderByFechaHoraDesc(pacienteId)
                .stream()
                .map(this::mapearResponseDTO)
                .collect(Collectors.toList());
    }

    // Retorna únicamente los turnos activos asignados al doctor (excluye CANCELADOS)
    public List<TurnoResponseDTO> obtenerAgendaDoctor(Long doctorId, LocalDate fecha) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new IllegalArgumentException("Doctor no encontrado con id: " + doctorId));

        // Control de Seguridad IDOR: El doctor solo puede ver su propia agenda
        String emailAutenticado = securityUtils.obtenerEmailUsuarioAutenticado();
        if (emailAutenticado == null) {
            throw new AccessDeniedException("Debe incluir un Token JWT válido en el encabezado Authorization para realizar esta acción.");
        }
        if (!doctor.getUsuario().getEmail().equalsIgnoreCase(emailAutenticado)) {
            throw new AccessDeniedException("Acceso denegado: No tiene permiso para ver la agenda de otro profesional.");
        }

        LocalDateTime inicio = fecha.atStartOfDay();
        LocalDateTime fin = fecha.atTime(23, 59, 59);

        return turnoRepository.findByDoctorIdAndFechaHoraBetweenAndEstadoNotOrderByFechaHoraAsc(
                        doctorId, inicio, fin, EstadoTurno.CANCELADO)
                .stream()
                .map(this::mapearResponseDTO)
                .collect(Collectors.toList());
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
                .build();
    }
}
