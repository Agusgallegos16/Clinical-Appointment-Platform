package com.consultorio.service;

import com.consultorio.domain.*;
import com.consultorio.dto.HorarioAtencionDTO;
import com.consultorio.dto.RegistroDoctorDTO;
import com.consultorio.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final UsuarioRepository usuarioRepository;
    private final EspecialidadRepository especialidadRepository;
    private final HorarioAtencionRepository horarioAtencionRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public DoctorService(DoctorRepository doctorRepository,
                         UsuarioRepository usuarioRepository,
                         EspecialidadRepository especialidadRepository,
                         HorarioAtencionRepository horarioAtencionRepository,
                         EmailService emailService,
                         PasswordEncoder passwordEncoder) {
        this.doctorRepository = doctorRepository;
        this.usuarioRepository = usuarioRepository;
        this.especialidadRepository = especialidadRepository;
        this.horarioAtencionRepository = horarioAtencionRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Doctor registrarDoctor(RegistroDoctorDTO dto) {
        if (usuarioRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Ya existe un usuario registrado con el email: " + dto.getEmail());
        }
        if (doctorRepository.existsByMatricula(dto.getMatricula())) {
            throw new IllegalArgumentException("Ya existe un doctor con la matrícula: " + dto.getMatricula());
        }

        List<Especialidad> especialidades = new ArrayList<>();
        if (dto.getEspecialidadIds() != null && !dto.getEspecialidadIds().isEmpty()) {
            especialidades = especialidadRepository.findAllById(dto.getEspecialidadIds());
        }

        Usuario usuario = Usuario.builder()
                .email(dto.getEmail())
                .password(passwordEncoder.encode(dto.getPassword()))
                .rol(Rol.DOCTOR)
                .activo(true)
                .build();

        Doctor doctor = Doctor.builder()
                .usuario(usuario)
                .nombre(dto.getNombre())
                .apellido(dto.getApellido())
                .matricula(dto.getMatricula())
                .especialidades(especialidades)
                .build();

        Doctor guardado = doctorRepository.save(doctor);

        // Notificación por email al crearse la cuenta
        emailService.enviarEmailBienvenida(guardado.getUsuario().getEmail(), "Dr/a. " + guardado.getApellido());

        return guardado;
    }

    @Transactional
    public HorarioAtencion agregarHorarioAtencion(Long doctorId, HorarioAtencionDTO dto) {
        Doctor doctor = obtenerPorId(doctorId);

        if (dto.getFecha() == null && dto.getDiaSemana() == null) {
            throw new IllegalArgumentException("Debe especificar al menos un día de la semana o una fecha concreta.");
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

    public Doctor obtenerPorId(Long id) {
        return doctorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Doctor no encontrado con id: " + id));
    }

    public List<Doctor> listarTodos() {
        return doctorRepository.findAll();
    }

    public List<Doctor> listarPorEspecialidad(Long especialidadId) {
        return doctorRepository.findByEspecialidadesId(especialidadId);
    }
}
