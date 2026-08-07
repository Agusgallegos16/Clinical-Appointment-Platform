package com.consultorio.service;

import com.consultorio.domain.Doctor;
import com.consultorio.domain.HorarioAtencion;
import com.consultorio.domain.Rol;
import com.consultorio.domain.Usuario;
import com.consultorio.dto.HorarioAtencionDTO;
import com.consultorio.dto.RegistroDoctorDTO;
import com.consultorio.repository.DoctorRepository;
import com.consultorio.repository.EspecialidadRepository;
import com.consultorio.repository.HorarioAtencionRepository;
import com.consultorio.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;

@Service
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final EspecialidadRepository especialidadRepository;
    private final UsuarioRepository usuarioRepository;
    private final HorarioAtencionRepository horarioAtencionRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public DoctorService(DoctorRepository doctorRepository,
                         EspecialidadRepository especialidadRepository,
                         UsuarioRepository usuarioRepository,
                         HorarioAtencionRepository horarioAtencionRepository,
                         EmailService emailService,
                         PasswordEncoder passwordEncoder) {
        this.doctorRepository = doctorRepository;
        this.especialidadRepository = especialidadRepository;
        this.usuarioRepository = usuarioRepository;
        this.horarioAtencionRepository = horarioAtencionRepository;
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

        // Permitir actualizar el email del Usuario asociado
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

        if (dto.getDiaSemana() == null && dto.getFecha() == null) {
            throw new IllegalArgumentException("Debe especificar un día de la semana o una fecha puntual para el horario.");
        }

        if (dto.getHoraInicio().isAfter(dto.getHoraFin()) || dto.getHoraInicio().equals(dto.getHoraFin())) {
            throw new IllegalArgumentException("La hora de inicio debe ser anterior a la hora de fin.");
        }

        // VALIDACIÓN DE SUPERPOSICIÓN DE HORARIOS
        List<HorarioAtencion> horariosExistentes = horarioAtencionRepository.findByDoctorId(doctorId);
        LocalTime newStart = dto.getHoraInicio();
        LocalTime newEnd = dto.getHoraFin();

        for (HorarioAtencion h : horariosExistentes) {
            boolean mismoDia = (dto.getDiaSemana() != null && dto.getDiaSemana() == h.getDiaSemana()) ||
                               (dto.getFecha() != null && dto.getFecha().equals(h.getFecha()));

            if (mismoDia) {
                // Verificar intersección de rangos: [newStart, newEnd) vs [h.getHoraInicio(), h.getHoraFin())
                if (newStart.isBefore(h.getHoraFin()) && newEnd.isAfter(h.getHoraInicio())) {
                    throw new IllegalArgumentException(String.format(
                            "El horario (%s - %s) se superpone con un horario ya configurado (%s - %s).",
                            newStart, newEnd, h.getHoraInicio(), h.getHoraFin()
                    ));
                }
            }
        }

        HorarioAtencion horario = HorarioAtencion.builder()
                .doctor(doctor)
                .diaSemana(dto.getDiaSemana())
                .fecha(dto.getFecha())
                .horaInicio(dto.getHoraInicio())
                .horaFin(dto.getHoraFin())
                .duracionTurnoMinutos(dto.getDuracionTurnoMinutos() > 0 ? dto.getDuracionTurnoMinutos() : 30)
                .build();

        return horarioAtencionRepository.save(horario);
    }

    public List<HorarioAtencion> obtenerHorariosDoctor(Long doctorId) {
        return horarioAtencionRepository.findByDoctorId(doctorId);
    }

    @Transactional
    public void eliminarHorarioAtencion(Long horarioId) {
        horarioAtencionRepository.deleteById(horarioId);
    }
}
