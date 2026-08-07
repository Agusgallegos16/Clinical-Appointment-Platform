package com.consultorio.service;

import com.consultorio.domain.*;
import com.consultorio.dto.TurnoReservaDTO;
import com.consultorio.dto.TurnoResponseDTO;
import com.consultorio.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    public TurnoService(TurnoRepository turnoRepository,
                        PacienteRepository pacienteRepository,
                        DoctorRepository doctorRepository,
                        EspecialidadRepository especialidadRepository,
                        EmailService emailService) {
        this.turnoRepository = turnoRepository;
        this.pacienteRepository = pacienteRepository;
        this.doctorRepository = doctorRepository;
        this.especialidadRepository = especialidadRepository;
        this.emailService = emailService;
    }

    @Transactional
    public TurnoResponseDTO reservarTurno(TurnoReservaDTO dto) {
        Paciente paciente = pacienteRepository.findById(dto.getPacienteId())
                .orElseThrow(() -> new IllegalArgumentException("Paciente no encontrado"));

        Doctor doctor = doctorRepository.findById(dto.getDoctorId())
                .orElseThrow(() -> new IllegalArgumentException("Doctor no encontrado"));

        Especialidad especialidad = especialidadRepository.findById(dto.getEspecialidadId())
                .orElseThrow(() -> new IllegalArgumentException("Especialidad no encontrada"));

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
        return turnoRepository.findByPacienteIdOrderByFechaHoraDesc(pacienteId)
                .stream()
                .map(this::mapearResponseDTO)
                .collect(Collectors.toList());
    }

    public List<TurnoResponseDTO> obtenerAgendaDoctor(Long doctorId, LocalDate fecha) {
        LocalDateTime inicio = fecha.atStartOfDay();
        LocalDateTime fin = fecha.atTime(23, 59, 59);

        return turnoRepository.findByDoctorIdAndFechaHoraBetweenOrderByFechaHoraAsc(doctorId, inicio, fin)
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
