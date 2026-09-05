package com.consultorio.mapper;

import com.consultorio.domain.*;
import com.consultorio.dto.*;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class DtoMapper {

    public EspecialidadResponseDTO toDto(Especialidad especialidad) {
        if (especialidad == null) return null;
        return EspecialidadResponseDTO.builder()
                .id(especialidad.getId())
                .nombre(especialidad.getNombre())
                .descripcion(especialidad.getDescripcion())
                .build();
    }

    public UsuarioBasicDTO toDto(Usuario usuario) {
        if (usuario == null) return null;
        return UsuarioBasicDTO.builder()
                .id(usuario.getId())
                .email(usuario.getEmail())
                .rol(usuario.getRol())
                .activo(usuario.isActivo())
                .emailVerificado(usuario.isEmailVerificado())
                .build();
    }

    public DoctorResponseDTO toDto(Doctor doctor) {
        if (doctor == null) return null;
        List<EspecialidadResponseDTO> especialidadesDto = doctor.getEspecialidades() != null
                ? doctor.getEspecialidades().stream().map(this::toDto).collect(Collectors.toList())
                : Collections.emptyList();

        String email = doctor.getUsuario() != null ? doctor.getUsuario().getEmail() : null;

        return DoctorResponseDTO.builder()
                .id(doctor.getId())
                .nombre(doctor.getNombre())
                .apellido(doctor.getApellido())
                .email(email)
                .fotoUrl(doctor.getFotoUrl())
                .usuario(toDto(doctor.getUsuario()))
                .especialidades(especialidadesDto)
                .disponibleParaTurnos(doctor.isDisponibleParaTurnos())
                .tieneAdvertenciaBloqueante(doctor.isTieneAdvertenciaBloqueante())
                .mensajeAdvertenciaBloqueante(doctor.getMensajeAdvertenciaBloqueante())
                .tieneAdvertenciaInformativa(doctor.isTieneAdvertenciaInformativa())
                .mensajeAdvertenciaInformativa(doctor.getMensajeAdvertenciaInformativa())
                .build();
    }

    public PacienteResponseDTO toDto(Paciente paciente) {
        if (paciente == null) return null;
        UUID tutorId = paciente.getTutor() != null ? paciente.getTutor().getId() : null;
        String tutorNombre = paciente.getTutor() != null
                ? (paciente.getTutor().getNombre() + " " + paciente.getTutor().getApellido())
                : null;

        return PacienteResponseDTO.builder()
                .id(paciente.getId())
                .dni(paciente.getDni())
                .nombre(paciente.getNombre())
                .apellido(paciente.getApellido())
                .telefono(paciente.getTelefono())
                .email(paciente.getEmail())
                .fechaNacimiento(paciente.getFechaNacimiento())
                .edad(paciente.getEdad())
                .usuario(toDto(paciente.getUsuario()))
                .tutorId(tutorId)
                .tutorNombre(tutorNombre)
                .build();
    }

    public HorarioAtencionResponseDTO toDto(HorarioAtencion h) {
        if (h == null) return null;
        return HorarioAtencionResponseDTO.builder()
                .id(h.getId())
                .doctorId(h.getDoctor() != null ? h.getDoctor().getId() : null)
                .especialidadId(h.getEspecialidad() != null ? h.getEspecialidad().getId() : null)
                .especialidadNombre(h.getEspecialidad() != null ? h.getEspecialidad().getNombre() : null)
                .diaSemana(h.getDiaSemana())
                .fecha(h.getFecha())
                .fechaDesde(h.getFechaDesde())
                .fechaHasta(h.getFechaHasta())
                .horaInicio(h.getHoraInicio())
                .horaFin(h.getHoraFin())
                .duracionTurnoMinutos(h.getDuracionTurnoMinutos())
                .build();
    }

    public SlotHorarioResponseDTO toDto(SlotHorario slot) {
        if (slot == null) return null;
        return SlotHorarioResponseDTO.builder()
                .id(slot.getId())
                .doctorId(slot.getDoctor() != null ? slot.getDoctor().getId() : null)
                .especialidadId(slot.getEspecialidad() != null ? slot.getEspecialidad().getId() : null)
                .especialidadNombre(slot.getEspecialidad() != null ? slot.getEspecialidad().getNombre() : null)
                .especialidad(toDto(slot.getEspecialidad()))
                .fecha(slot.getFecha())
                .horaInicio(slot.getHoraInicio())
                .horaFin(slot.getHoraFin())
                .duracionMinutos(slot.getDuracionMinutos())
                .esPuntual(slot.isEsPuntual())
                .build();
    }

    public DetallePlantillaResponseDTO toDto(DetallePlantilla d) {
        if (d == null) return null;
        return DetallePlantillaResponseDTO.builder()
                .id(d.getId())
                .especialidadId(d.getEspecialidad() != null ? d.getEspecialidad().getId() : null)
                .especialidad(toDto(d.getEspecialidad()))
                .horaInicio(d.getHoraInicio())
                .horaFin(d.getHoraFin())
                .duracionTurnoMinutos(d.getDuracionTurnoMinutos())
                .build();
    }

    public PlantillaAgendaResponseDTO toDto(PlantillaAgenda p) {
        if (p == null) return null;
        List<DetallePlantillaResponseDTO> detalles = p.getDetalles() != null
                ? p.getDetalles().stream().map(this::toDto).collect(Collectors.toList())
                : Collections.emptyList();

        return PlantillaAgendaResponseDTO.builder()
                .id(p.getId())
                .doctorId(p.getDoctor() != null ? p.getDoctor().getId() : null)
                .nombre(p.getNombre())
                .descripcion(p.getDescripcion())
                .detalles(detalles)
                .build();
    }
}
