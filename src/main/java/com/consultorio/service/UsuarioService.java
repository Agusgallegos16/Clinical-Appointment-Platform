package com.consultorio.service;

import com.consultorio.domain.Rol;
import com.consultorio.domain.Usuario;
import com.consultorio.dto.EstablecerPasswordDoctorDTO;
import com.consultorio.dto.SolicitudRestablecerPasswordDTO;
import com.consultorio.dto.UsuarioAdminDTO;
import com.consultorio.event.UsuarioEliminadoEvent;
import com.consultorio.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class UsuarioService {

    private final static Logger log = LoggerFactory.getLogger(UsuarioService.class);

    private final UsuarioRepository usuarioRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    public UsuarioService(UsuarioRepository usuarioRepository,
                          EmailService emailService,
                          PasswordEncoder passwordEncoder,
                          ApplicationEventPublisher eventPublisher) {
        this.usuarioRepository = usuarioRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
    }

    public Optional<Usuario> obtenerPorEmail(String email) {
        return usuarioRepository.findByEmail(email);
    }

    public boolean existePorEmail(String email) {
        if (email == null) return false;
        return usuarioRepository.existsByEmail(email.trim());
    }

    @Transactional
    public Usuario crearUsuarioParaPaciente(String email, String passwordRaw, String tokenVerificacion) {
        if (usuarioRepository.existsByEmail(email.trim())) {
            throw new IllegalArgumentException("Ya existe un usuario registrado con el email: " + email);
        }
        Usuario usuario = Usuario.builder()
                .email(email.trim())
                .password(passwordEncoder.encode(passwordRaw))
                .rol(Rol.PACIENTE)
                .activo(false)
                .emailVerificado(false)
                .tokenVerificacionEmail(tokenVerificacion)
                .tokenVerificacionExpiracion(LocalDateTime.now().plusHours(24))
                .build();
        return usuarioRepository.save(usuario);
    }

    @Transactional
    public Usuario activarCuentaPaciente(String token) {
        Usuario usuario = usuarioRepository.findByTokenVerificacionEmail(token)
                .orElseThrow(() -> new IllegalArgumentException("El token de activación es inválido o no existe."));

        if (usuario.getTokenVerificacionExpiracion() != null
                && usuario.getTokenVerificacionExpiracion().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException(
                    "El token de activación ha expirado. Por favor solicite un nuevo registro.");
        }

        usuario.setActivo(true);
        usuario.setEmailVerificado(true);
        usuario.setTokenVerificacionEmail(null);
        usuario.setTokenVerificacionExpiracion(null);
        return usuarioRepository.save(usuario);
    }

    @Transactional
    public void actualizarCredenciales(Long usuarioId, String nuevoEmail, String nuevaPasswordRaw) {
        Usuario usuario = obtenerPorId(usuarioId);
        if (nuevoEmail != null && !nuevoEmail.isBlank() && !usuario.getEmail().equalsIgnoreCase(nuevoEmail.trim())) {
            if (usuarioRepository.existsByEmail(nuevoEmail.trim())) {
                throw new IllegalArgumentException("Ya existe una cuenta registrada con el email: " + nuevoEmail);
            }
            usuario.setEmail(nuevoEmail.trim());
        }
        if (nuevaPasswordRaw != null && !nuevaPasswordRaw.trim().isEmpty()) {
            if (nuevaPasswordRaw.trim().length() < 6) {
                throw new IllegalArgumentException("La nueva contraseña debe tener al menos 6 caracteres.");
            }
            usuario.setPassword(passwordEncoder.encode(nuevaPasswordRaw.trim()));
        }
        usuarioRepository.save(usuario);
    }

    public Usuario obtenerPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con ID: " + id));
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
            // Token de 24hs expirado: descartar el registro para permitir un nuevo alta
            Long usuarioId = usuario.getId();
            usuarioRepository.delete(usuario);
            eventPublisher.publishEvent(new UsuarioEliminadoEvent(usuarioId));
            throw new IllegalArgumentException("El enlace de activación ha expirado. La solicitud de alta fue descartada. Solicite al Administrador que vuelva a registrar su perfil.");
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
    public Page<Usuario> buscarUsuariosPaginadosEntidad(String query, Pageable pageable) {
        if (query != null && !query.trim().isEmpty()) {
            return usuarioRepository.buscarPorNombreApellidoOEmail(query.trim(), pageable);
        }
        return usuarioRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Optional<Usuario> buscarPorEmailIgnoreCase(String email) {
        if (email == null) return Optional.empty();
        return usuarioRepository.findByEmailIgnoreCase(email.trim());
    }

    @Transactional
    public Usuario crearUsuarioParaAdmin(String email, Rol rol, String tokenActivacion) {
        Usuario usuario = Usuario.builder()
                .email(email)
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .rol(rol)
                .activo(false)
                .emailVerificado(false)
                .tokenVerificacionEmail(tokenActivacion)
                .tokenVerificacionExpiracion(LocalDateTime.now().plusHours(24))
                .build();
        return usuarioRepository.save(usuario);
    }

    @Transactional
    public void eliminarEntidadDirecta(Usuario usuario) {
        if (usuario != null) {
            Long id = usuario.getId();
            usuarioRepository.delete(usuario);
            usuarioRepository.flush();
            eventPublisher.publishEvent(new UsuarioEliminadoEvent(id));
        }
    }

    @Transactional
    public UsuarioAdminDTO cambiarEstadoBloqueo(Long usuarioId, boolean bloquear, String emailAdminActual) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con ID: " + usuarioId));

        if (usuario.getEmail().equalsIgnoreCase(emailAdminActual)) {
            throw new IllegalArgumentException("Un administrador no puede cambiar su propio estado de bloqueo.");
        }

        usuario.setBloqueado(bloquear);
        Usuario actualizado = usuarioRepository.save(usuario);

        log.info("🛡️ Estado de bloqueo de usuario {} cambiado a: {}", usuario.getEmail(), bloquear);

        return UsuarioAdminDTO.builder()
                .id(actualizado.getId())
                .email(actualizado.getEmail())
                .rol(actualizado.getRol())
                .activo(actualizado.isActivo())
                .bloqueado(actualizado.isBloqueado())
                .emailVerificado(actualizado.isEmailVerificado())
                .build();
    }

    @Transactional
    public void eliminarUsuario(Long usuarioId, String emailAdminActual) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con ID: " + usuarioId));

        if (usuario.getEmail().equalsIgnoreCase(emailAdminActual)) {
            throw new IllegalArgumentException("Un administrador no puede auto-eliminarse del sistema.");
        }

        usuarioRepository.delete(usuario);
        eventPublisher.publishEvent(new UsuarioEliminadoEvent(usuarioId));
        log.info("🗑️ Usuario {} eliminado del sistema por el admin {}", usuario.getEmail(), emailAdminActual);
    }
}
