package com.consultorio.service;

import com.consultorio.domain.Doctor;
import com.consultorio.domain.Paciente;
import com.consultorio.domain.Rol;
import com.consultorio.domain.Usuario;
import com.consultorio.dto.EstablecerPasswordDoctorDTO;
import com.consultorio.dto.SolicitudRestablecerPasswordDTO;
import com.consultorio.dto.UsuarioAdminDTO;
import com.consultorio.repository.DoctorRepository;
import com.consultorio.repository.PacienteRepository;
import com.consultorio.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UsuarioService {

    private static final Logger log = LoggerFactory.getLogger(UsuarioService.class);

    private final UsuarioRepository usuarioRepository;
    private final DoctorRepository doctorRepository;
    private final PacienteRepository pacienteRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UsuarioService(UsuarioRepository usuarioRepository,
                          DoctorRepository doctorRepository,
                          PacienteRepository pacienteRepository,
                          EmailService emailService,
                          PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.doctorRepository = doctorRepository;
        this.pacienteRepository = pacienteRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void solicitarRestablecimientoPassword(SolicitudRestablecerPasswordDTO dto) {
        if (dto.getConfirmarNuevaPassword() != null && !dto.getNuevaPassword().equals(dto.getConfirmarNuevaPassword())) {
            throw new IllegalArgumentException("Las contraseñas no coinciden. Por favor verifíquelas.");
        }

        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(dto.getEmail());
        if (usuarioOpt.isEmpty()) {
            throw new IllegalArgumentException("No existe ninguna cuenta registrada con el correo electrónico ingresado.");
        }

        Usuario usuario = usuarioOpt.get();
        String token = UUID.randomUUID().toString();

        usuario.setNuevaPasswordPendiente(passwordEncoder.encode(dto.getNuevaPassword()));
        usuario.setTokenRestablecimientoPassword(token);
        usuario.setTokenRestablecimientoExpiracion(LocalDateTime.now().plusHours(1));

        usuarioRepository.save(usuario);

        // Notificación por email con enlace de confirmación
        emailService.enviarEmailRestablecerPassword(usuario.getEmail(), token);
    }

    @Transactional
    public boolean confirmarRestablecimientoPassword(String token) {
        Usuario usuario = usuarioRepository.findByTokenRestablecimientoPassword(token)
                .orElseThrow(() -> new IllegalArgumentException("El token de confirmación es inválido o no existe."));

        if (usuario.getTokenRestablecimientoExpiracion() != null && usuario.getTokenRestablecimientoExpiracion().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("El token de confirmación ha expirado. Por favor solicite nuevamente el cambio de contraseña.");
        }

        if (usuario.getNuevaPasswordPendiente() == null) {
            throw new IllegalStateException("No hay ninguna contraseña nueva pendiente de activación.");
        }

        // Actualizar la contraseña real en la base de datos recién tras la confirmación vía mail
        usuario.setPassword(usuario.getNuevaPasswordPendiente());
        usuario.setNuevaPasswordPendiente(null);
        usuario.setTokenRestablecimientoPassword(null);
        usuario.setTokenRestablecimientoExpiracion(null);

        usuarioRepository.save(usuario);
        log.info("✅ Contraseña actualizada con éxito para el usuario: {}", usuario.getEmail());

        return true;
    }

    @Transactional
    public boolean establecerPasswordDoctor(EstablecerPasswordDoctorDTO dto) {
        if (dto.getConfirmarPassword() != null && !dto.getPassword().equals(dto.getConfirmarPassword())) {
            throw new IllegalArgumentException("Las contraseñas no coinciden. Por favor verifíquelas.");
        }

        Usuario usuario = usuarioRepository.findByTokenVerificacionEmail(dto.getToken())
                .orElseThrow(() -> new IllegalArgumentException("El token de activación es inválido o no existe."));

        if (usuario.getTokenVerificacionExpiracion() != null && usuario.getTokenVerificacionExpiracion().isBefore(LocalDateTime.now())) {
            // El token de 24hs ha expirado: descartar el registro para permitir un nuevo alta
            Doctor doctorObj = doctorRepository.findByUsuarioId(usuario.getId()).orElse(null);
            if (doctorObj != null) {
                doctorRepository.delete(doctorObj);
            }
            usuarioRepository.delete(usuario);
            throw new IllegalArgumentException("El enlace de activación ha expirado (límite 24hs). La solicitud de alta fue descartada. Solicite al Administrador que vuelva a registrar su perfil.");
        }

        usuario.setPassword(passwordEncoder.encode(dto.getPassword()));
        usuario.setActivo(true);
        usuario.setEmailVerificado(true);
        usuario.setTokenVerificacionEmail(null);
        usuario.setTokenVerificacionExpiracion(null);
        usuarioRepository.save(usuario);

        log.info("✅ Cuenta de médico activada con éxito para: {}", usuario.getEmail());
        return true;
    }

    @Transactional(readOnly = true)
    public List<UsuarioAdminDTO> buscarUsuariosPorEmail(String queryEmail) {
        List<Usuario> usuarios = (queryEmail == null || queryEmail.trim().isEmpty())
                ? usuarioRepository.findAll()
                : usuarioRepository.findByEmailContainingIgnoreCase(queryEmail.trim());

        return usuarios.stream().map(u -> {
            String nombre = null;
            String apellido = null;

            if (u.getRol() == Rol.PACIENTE) {
                Optional<Paciente> p = pacienteRepository.findByUsuarioId(u.getId());
                if (p.isPresent()) {
                    nombre = p.get().getNombre();
                    apellido = p.get().getApellido();
                }
            } else if (u.getRol() == Rol.DOCTOR) {
                Optional<Doctor> d = doctorRepository.findByUsuarioId(u.getId());
                if (d.isPresent()) {
                    nombre = d.get().getNombre();
                    apellido = d.get().getApellido();
                }
            }

            return UsuarioAdminDTO.builder()
                    .id(u.getId())
                    .email(u.getEmail())
                    .rol(u.getRol())
                    .activo(u.isActivo())
                    .bloqueado(u.isBloqueado())
                    .emailVerificado(u.isEmailVerificado())
                    .nombre(nombre)
                    .apellido(apellido)
                    .build();
        }).collect(Collectors.toList());
    }

    @Transactional
    public UsuarioAdminDTO cambiarEstadoBloqueo(Long usuarioId, boolean bloquear, String emailAdminActual) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con ID: " + usuarioId));

        if (usuario.getEmail().equalsIgnoreCase(emailAdminActual)) {
            throw new IllegalArgumentException("Un administrador no puede cambiar su propio estado de bloqueo.");
        }

        usuario.setBloqueado(bloquear);
        usuarioRepository.save(usuario);

        log.info("🛡️ Estado de bloqueo de usuario {} cambiado a: {}", usuario.getEmail(), bloquear);

        return buscarUsuariosPorEmail(usuario.getEmail()).stream().findFirst().orElse(null);
    }

    @Transactional
    public void eliminarUsuario(Long usuarioId, String emailAdminActual) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con ID: " + usuarioId));

        if (usuario.getEmail().equalsIgnoreCase(emailAdminActual)) {
            throw new IllegalArgumentException("Un administrador no puede auto-eliminarse del sistema.");
        }

        // Si es paciente, eliminar o desvincular perfil Paciente
        Optional<Paciente> paciente = pacienteRepository.findByUsuarioId(usuarioId);
        paciente.ifPresent(pacienteRepository::delete);

        // Si es doctor, eliminar o desvincular perfil Doctor
        Optional<Doctor> doctor = doctorRepository.findByUsuarioId(usuarioId);
        doctor.ifPresent(doctorRepository::delete);

        usuarioRepository.delete(usuario);
        log.info("🗑️ Usuario {} eliminado del sistema por el admin {}", usuario.getEmail(), emailAdminActual);
    }
}
