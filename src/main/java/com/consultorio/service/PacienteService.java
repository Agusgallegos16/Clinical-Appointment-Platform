package com.consultorio.service;

import com.consultorio.domain.Paciente;
import com.consultorio.domain.Rol;
import com.consultorio.domain.Usuario;
import com.consultorio.dto.RegistroPacienteDTO;
import com.consultorio.repository.PacienteRepository;
import com.consultorio.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PacienteService {

    private final PacienteRepository pacienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public PacienteService(PacienteRepository pacienteRepository,
                           UsuarioRepository usuarioRepository,
                           EmailService emailService,
                           PasswordEncoder passwordEncoder) {
        this.pacienteRepository = pacienteRepository;
        this.usuarioRepository = usuarioRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Paciente registrarPaciente(RegistroPacienteDTO dto) {
        if (usuarioRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Ya existe un usuario registrado con el email: " + dto.getEmail());
        }
        if (pacienteRepository.existsByDni(dto.getDni())) {
            throw new IllegalArgumentException("Ya existe un paciente registrado con el DNI: " + dto.getDni());
        }

        Usuario usuario = Usuario.builder()
                .email(dto.getEmail())
                .password(passwordEncoder.encode(dto.getPassword()))
                .rol(Rol.PACIENTE)
                .activo(true)
                .build();

        Paciente paciente = Paciente.builder()
                .usuario(usuario)
                .nombre(dto.getNombre())
                .apellido(dto.getApellido())
                .dni(dto.getDni())
                .telefono(dto.getTelefono())
                .build();

        Paciente guardado = pacienteRepository.save(paciente);

        // Notificación por email al crearse la cuenta
        emailService.enviarEmailBienvenida(guardado.getUsuario().getEmail(), guardado.getNombre());

        return guardado;
    }

    public Paciente obtenerPorId(Long id) {
        return pacienteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Paciente no encontrado con id: " + id));
    }

    public Paciente obtenerPorUsuarioEmail(String email) {
        return pacienteRepository.findByUsuarioEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Paciente no encontrado con email: " + email));
    }

    public List<Paciente> listarTodos() {
        return pacienteRepository.findAll();
    }
}
