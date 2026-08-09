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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

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
        if (dto.getConfirmarPassword() != null && !dto.getPassword().equals(dto.getConfirmarPassword())) {
            throw new IllegalArgumentException("Las contraseñas no coinciden. Por favor verifíquelas.");
        }
        if (usuarioRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Ya existe un usuario registrado con el email: " + dto.getEmail());
        }
        if (pacienteRepository.existsByDni(dto.getDni())) {
            throw new IllegalArgumentException("Ya existe un paciente registrado con el DNI: " + dto.getDni());
        }

        String tokenVerificacion = UUID.randomUUID().toString();

        Usuario usuario = Usuario.builder()
                .email(dto.getEmail())
                .password(passwordEncoder.encode(dto.getPassword()))
                .rol(Rol.PACIENTE)
                .activo(false) // Inactivo hasta que confirme por email
                .emailVerificado(false)
                .tokenVerificacionEmail(tokenVerificacion)
                .tokenVerificacionExpiracion(LocalDateTime.now().plusHours(24))
                .build();

        Paciente paciente = Paciente.builder()
                .usuario(usuario)
                .nombre(dto.getNombre())
                .apellido(dto.getApellido())
                .dni(dto.getDni())
                .telefono(dto.getTelefono())
                .build();

        Paciente guardado = pacienteRepository.save(paciente);

        // Enviar correo de verificación de cuenta (Double Opt-In)
        emailService.enviarEmailVerificacion(guardado.getUsuario().getEmail(), guardado.getNombre(), tokenVerificacion);

        return guardado;
    }

    @Transactional
    public boolean confirmarEmail(String token) {
        Usuario usuario = usuarioRepository.findByTokenVerificacionEmail(token)
                .orElseThrow(() -> new IllegalArgumentException("El token de activación es inválido o no existe."));

        if (usuario.getTokenVerificacionExpiracion() != null && usuario.getTokenVerificacionExpiracion().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("El token de activación ha expirado. Por favor solicite un nuevo registro.");
        }

        usuario.setActivo(true);
        usuario.setEmailVerificado(true);
        usuario.setTokenVerificacionEmail(null);
        usuario.setTokenVerificacionExpiracion(null);
        usuarioRepository.save(usuario);

        // Enviar correo de bienvenida tras activar exitosamente
        pacienteRepository.findByUsuarioEmail(usuario.getEmail())
                .ifPresent(p -> emailService.enviarEmailBienvenida(usuario.getEmail(), p.getNombre()));

        return true;
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
