package com.consultorio.service;

import com.consultorio.domain.*;
import com.consultorio.dto.BloqueoHorarioDTO;
import com.consultorio.dto.HorarioAtencionDTO;
import com.consultorio.dto.RegistroDoctorDTO;
import com.consultorio.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final EspecialidadRepository especialidadRepository;
    private final UsuarioRepository usuarioRepository;
    private final HorarioAtencionRepository horarioAtencionRepository;
    private final BloqueoHorarioRepository bloqueoHorarioRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public DoctorService(DoctorRepository doctorRepository,
                         EspecialidadRepository especialidadRepository,
                         UsuarioRepository usuarioRepository,
                         HorarioAtencionRepository horarioAtencionRepository,
                         BloqueoHorarioRepository bloqueoHorarioRepository,
                         EmailService emailService,
                         PasswordEncoder passwordEncoder) {
        this.doctorRepository = doctorRepository;
        this.especialidadRepository = especialidadRepository;
        this.usuarioRepository = usuarioRepository;
        this.horarioAtencionRepository = horarioAtencionRepository;
        this.bloqueoHorarioRepository = bloqueoHorarioRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Doctor registrarDoctor(RegistroDoctorDTO dto) {
        if (usuarioRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("El email ya se encuentra registrado: " + dto.getEmail());
        }

        String rawPassword = (dto.getPassword() != null && !dto.getPassword().isBlank()) ? dto.getPassword() : "123456";

        Usuario usuario = Usuario.builder()
                .email(dto.getEmail())
                .password(passwordEncoder.encode(rawPassword))
                .rol(Rol.DOCTOR)
                .activo(true)
                .build();

        var especialidades = (dto.getEspecialidadIds() != null && !dto.getEspecialidadIds().isEmpty())
                ? especialidadRepository.findAllById(dto.getEspecialidadIds())
                : List.<com.consultorio.domain.Especialidad>of();

        Doctor doctor = Doctor.builder()
                .usuario(usuario)
                .nombre(dto.getNombre())
                .apellido(dto.getApellido())
                .especialidades(especialidades)
                .build();

        Doctor guardado = doctorRepository.save(doctor);
        emailService.enviarEmailBienvenida(guardado.getUsuario().getEmail(), guardado.getNombre());
        return guardado;
    }

    @Transactional
    public Doctor actualizarDoctor(Long id, RegistroDoctorDTO dto) {
        Doctor doctor = obtenerPorId(id);

        doctor.setNombre(dto.getNombre());
        doctor.setApellido(dto.getApellido());

        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            if (!doctor.getUsuario().getEmail().equalsIgnoreCase(dto.getEmail()) &&
                    usuarioRepository.existsByEmail(dto.getEmail())) {
                throw new IllegalArgumentException("El email ya pertenece a otro usuario: " + dto.getEmail());
            }
            doctor.getUsuario().setEmail(dto.getEmail());
        }

        if (dto.getEspecialidadIds() != null) {
            var especialidades = especialidadRepository.findAllById(dto.getEspecialidadIds());
            doctor.setEspecialidades(especialidades);
        }

        return doctorRepository.save(doctor);
    }

    @Transactional
    public void eliminarDoctor(Long id) {
        Doctor doctor = obtenerPorId(id);
        doctorRepository.delete(doctor);
    }

    public List<Doctor> listarTodos() {
        return doctorRepository.findAll();
    }

    public List<Doctor> listarPorEspecialidad(Long especialidadId) {
        return doctorRepository.findByEspecialidadesId(especialidadId);
    }

    public Doctor obtenerPorId(Long id) {
        return doctorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Doctor no encontrado con ID: " + id));
    }

    @Transactional
    public HorarioAtencion agregarHorarioAtencion(Long doctorId, HorarioAtencionDTO dto) {
        Doctor doctor = obtenerPorId(doctorId);

        LocalDate hoy = LocalDate.now();

        // VALIDACIÓN DE FECHAS PASADAS
        if (dto.getFecha() != null && dto.getFecha().isBefore(hoy)) {
            throw new IllegalArgumentException("No se pueden configurar horarios para fechas pasadas.");
        }

        if (dto.getFechaHasta() != null && dto.getFechaHasta().isBefore(hoy)) {
            throw new IllegalArgumentException("La fecha límite de vigencia (fecha hasta) no puede ser anterior a la fecha actual.");
        }

        if (dto.getDiaSemana() == null && dto.getFecha() == null) {
            throw new IllegalArgumentException("Debe especificar un día de la semana o una fecha puntual para el horario.");
        }

        if (dto.getHoraInicio().isAfter(dto.getHoraFin()) || dto.getHoraInicio().equals(dto.getHoraFin())) {
            throw new IllegalArgumentException("La hora de inicio debe ser anterior a la hora de fin.");
        }

        // VALIDACIÓN DE SUPERPOSICIÓN STRICTA
        validarSuperposicionHorarios(doctorId, dto, null);

        Especialidad especialidad = null;
        if (dto.getEspecialidadId() != null) {
            especialidad = especialidadRepository.findById(dto.getEspecialidadId()).orElse(null);
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
                .duracionTurnoMinutos(dto.getDuracionTurnoMinutos() > 0 ? dto.getDuracionTurnoMinutos() : 30)
                .build();

        return horarioAtencionRepository.save(horario);
    }

    @Transactional
    public HorarioAtencion actualizarHorarioAtencion(Long horarioId, HorarioAtencionDTO dto) {
        HorarioAtencion horario = horarioAtencionRepository.findById(horarioId)
                .orElseThrow(() -> new IllegalArgumentException("Horario no encontrado con ID: " + horarioId));

        LocalDate hoy = LocalDate.now();

        // VALIDACIÓN DE FECHAS PASADAS
        if (dto.getFecha() != null && dto.getFecha().isBefore(hoy)) {
            throw new IllegalArgumentException("No se pueden configurar horarios para fechas pasadas.");
        }

        if (dto.getFechaHasta() != null && dto.getFechaHasta().isBefore(hoy)) {
            throw new IllegalArgumentException("La fecha límite de vigencia (fecha hasta) no puede ser anterior a la fecha actual.");
        }

        if (dto.getHoraInicio().isAfter(dto.getHoraFin()) || dto.getHoraInicio().equals(dto.getHoraFin())) {
            throw new IllegalArgumentException("La hora de inicio debe ser anterior a la hora de fin.");
        }

        // VALIDACIÓN DE SUPERPOSICIÓN STRICTA EXCLUYENDO EL ID ACTUAL
        validarSuperposicionHorarios(horario.getDoctor().getId(), dto, horarioId);

        if (dto.getEspecialidadId() != null) {
            Especialidad especialidad = especialidadRepository.findById(dto.getEspecialidadId()).orElse(null);
            horario.setEspecialidad(especialidad);
        } else {
            horario.setEspecialidad(null);
        }

        horario.setDiaSemana(dto.getDiaSemana());
        horario.setFecha(dto.getFecha());
        horario.setFechaDesde(dto.getFechaDesde());
        horario.setFechaHasta(dto.getFechaHasta());
        horario.setHoraInicio(dto.getHoraInicio());
        horario.setHoraFin(dto.getHoraFin());
        horario.setDuracionTurnoMinutos(dto.getDuracionTurnoMinutos() > 0 ? dto.getDuracionTurnoMinutos() : 30);

        return horarioAtencionRepository.save(horario);
    }

    private void validarSuperposicionHorarios(Long doctorId, HorarioAtencionDTO dto, Long idAExcluir) {
        List<HorarioAtencion> existentes = horarioAtencionRepository.findByDoctorId(doctorId);

        LocalTime newStart = dto.getHoraInicio();
        LocalTime newEnd = dto.getHoraFin();

        DiaSemana newDiaSemana = dto.getDiaSemana();
        LocalDate newFecha = dto.getFecha();
        if (newFecha != null && newDiaSemana == null) {
            newDiaSemana = mapearDiaSemana(newFecha.getDayOfWeek());
        }

        for (HorarioAtencion h : existentes) {
            if (idAExcluir != null && h.getId().equals(idAExcluir)) {
                continue;
            }

            DiaSemana hDiaSemana = h.getDiaSemana();
            LocalDate hFecha = h.getFecha();
            if (hFecha != null && hDiaSemana == null) {
                hDiaSemana = mapearDiaSemana(hFecha.getDayOfWeek());
            }

            boolean coincidenEnDia = false;

            if (newFecha != null && hFecha != null) {
                coincidenEnDia = newFecha.equals(hFecha);
            } else if (newFecha != null && hDiaSemana != null) {
                boolean enVigencia = (h.getFechaDesde() == null || !newFecha.isBefore(h.getFechaDesde())) &&
                                     (h.getFechaHasta() == null || !newFecha.isAfter(h.getFechaHasta()));
                coincidenEnDia = (newDiaSemana == hDiaSemana) && enVigencia;
            } else if (newDiaSemana != null && hFecha != null) {
                boolean enVigencia = (dto.getFechaDesde() == null || !hFecha.isBefore(dto.getFechaDesde())) &&
                                     (dto.getFechaHasta() == null || !hFecha.isAfter(dto.getFechaHasta()));
                coincidenEnDia = (newDiaSemana == hDiaSemana) && enVigencia;
            } else if (newDiaSemana != null && hDiaSemana != null) {
                if (newDiaSemana == hDiaSemana) {
                    boolean v1Desde = dto.getFechaDesde() != null;
                    boolean v1Hasta = dto.getFechaHasta() != null;
                    boolean v2Desde = h.getFechaDesde() != null;
                    boolean v2Hasta = h.getFechaHasta() != null;

                    boolean noSeCruzan = (v1Hasta && v2Desde && dto.getFechaHasta().isBefore(h.getFechaDesde())) ||
                                         (v1Desde && v2Hasta && dto.getFechaDesde().isAfter(h.getFechaHasta()));
                    coincidenEnDia = !noSeCruzan;
                }
            }

            if (coincidenEnDia) {
                if (newStart.isBefore(h.getHoraFin()) && newEnd.isAfter(h.getHoraInicio())) {
                    String diaStr = (hFecha != null) ? "para el día " + hFecha : "para los " + hDiaSemana;
                    throw new IllegalArgumentException(String.format(
                            "Conflicto de horario: La franja (%s - %s) se superpone con un horario ya configurado %s (%s - %s).",
                            newStart, newEnd, diaStr, h.getHoraInicio(), h.getHoraFin()
                    ));
                }
            }
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

    public List<HorarioAtencion> obtenerHorariosDoctor(Long doctorId) {
        return horarioAtencionRepository.findByDoctorId(doctorId);
    }

    @Transactional
    public void eliminarHorarioAtencion(Long horarioId) {
        horarioAtencionRepository.deleteById(horarioId);
    }

    @Transactional
    public BloqueoHorario bloquearSlotIndividual(Long doctorId, BloqueoHorarioDTO dto) {
        Doctor doctor = obtenerPorId(doctorId);

        if (dto.getFecha().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("No se pueden deshabilitar turnos de fechas pasadas.");
        }

        BloqueoHorario bloqueo = BloqueoHorario.builder()
                .doctor(doctor)
                .fecha(dto.getFecha())
                .horaInicio(dto.getHoraInicio())
                .horaFin(dto.getHoraFin())
                .build();

        return bloqueoHorarioRepository.save(bloqueo);
    }

    public List<BloqueoHorario> obtenerBloqueosDoctor(Long doctorId) {
        return bloqueoHorarioRepository.findByDoctorId(doctorId);
    }

    @Transactional
    public void eliminarBloqueoSlot(Long bloqueoId) {
        bloqueoHorarioRepository.deleteById(bloqueoId);
    }
}
