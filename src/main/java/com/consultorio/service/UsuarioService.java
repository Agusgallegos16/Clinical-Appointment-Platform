package com.consultorio.service;

import com.consultorio.domain.Rol;
import com.consultorio.domain.Usuario;
import com.consultorio.dto.ConfirmarRestablecerPasswordDTO;
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

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Service
public class UsuarioService {

    private final static Logger log = LoggerFactory.getLogger(UsuarioService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UsuarioRepository usuarioRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;
    private final AuditLogService auditLogService;

    @Autowired
    public UsuarioService(UsuarioRepository usuarioRepository,
                          EmailService emailService,
                          PasswordEncoder passwordEncoder,
                          ApplicationEventPublisher eventPublisher,
                          AuditLogService auditLogService) {
        this.usuarioRepository = usuarioRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
        this.auditLogService = auditLogService;
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
    public void actualizarCredenciales(Long usuarioId, String nuevoEmail, String nuevaPasswordRaw, String currentPassword) {
        Usuario usuario = obtenerPorId(usuarioId);
        boolean emailCambia = nuevoEmail != null && !nuevoEmail.isBlank()
                && !usuario.getEmail().equalsIgnoreCase(nuevoEmail.trim());
        boolean passwordCambia = nuevaPasswordRaw != null && !nuevaPasswordRaw.trim().isEmpty();

        if (!emailCambia && !passwordCambia) {
            return;
        }

        if (currentPassword == null || currentPassword.isBlank()
                || !passwordEncoder.matches(currentPassword, usuario.getPassword())) {
            throw new IllegalArgumentException(
                    "Debe ingresar su contraseña actual válida para modificar el correo electrónico o la contraseña.");
        }

        if (emailCambia) {
            if (usuarioRepository.existsByEmail(nuevoEmail.trim())) {
                throw new IllegalArgumentException("Ya existe una cuenta registrada con el email ingresado.");
            }
            usuario.setEmail(nuevoEmail.trim());
        }
        if (passwordCambia) {
            if (nuevaPasswordRaw.trim().length() < 12) {
                throw new IllegalArgumentException("La nueva contraseña debe tener al menos 12 caracteres.");
            }
            usuario.setPassword(passwordEncoder.encode(nuevaPasswordRaw.trim()));
        }
        usuario.incrementTokenVersion();
        usuarioRepository.save(usuario);
        if (auditLogService != null) {
            auditLogService.record("CREDENTIALS_UPDATED", "SUCCESS", usuario.getId(), usuario.getRol().name(),
                    "USUARIO", String.valueOf(usuario.getId()), "User credentials updated and session rotated");
        }
    }

    @Transactional
    public void revocarSesionesUsuario(String email) {
        if (email == null || email.isBlank()) {
            return;
        }
        usuarioRepository.findByEmailIgnoreCase(email.trim()).ifPresent(usuario -> {
            usuario.incrementTokenVersion();
            usuarioRepository.save(usuario);
        });
    }

    public Usuario obtenerPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con ID: " + id));
    }

    @Transactional
    public void solicitarRestablecimientoPassword(SolicitudRestablecerPasswordDTO dto) {
        if (dto == null || dto.getEmail() == null || dto.getEmail().isBlank()) {
            throw new IllegalArgumentException("El email es obligatorio.");
        }

        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmailIgnoreCase(dto.getEmail().trim());
        if (usuarioOpt.isEmpty()) {
            // Silent return to prevent account enumeration
            return;
        }

        Usuario usuario = usuarioOpt.get();
        String token = generarTokenSeguro();

        usuario.setNuevaPasswordPendiente(null);
        usuario.setTokenRestablecimientoPassword(token);
        usuario.setTokenRestablecimientoExpiracion(LocalDateTime.now().plusHours(1));

        usuarioRepository.save(usuario);

        emailService.enviarEmailRestablecerPassword(usuario.getEmail(), token);
    }

    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public boolean confirmarRestablecimientoPassword(ConfirmarRestablecerPasswordDTO dto) {
        if (dto == null || dto.getToken() == null || dto.getToken().isBlank()) {
            throw new IllegalArgumentException("El token de confirmación es inválido o no existe.");
        }
        if (dto.getNuevaPassword() == null || dto.getNuevaPassword().length() < 12) {
            throw new IllegalArgumentException("La nueva contraseña debe tener al menos 12 caracteres.");
        }
        if (!dto.getNuevaPassword().equals(dto.getConfirmarNuevaPassword())) {
            throw new IllegalArgumentException("Las contraseñas no coinciden. Por favor verifíquelas.");
        }

        Usuario usuario = usuarioRepository.findByTokenRestablecimientoPassword(dto.getToken().trim())
                .orElseThrow(() -> new IllegalArgumentException("El token de confirmación es inválido o no existe."));

        if (usuario.getTokenRestablecimientoExpiracion() == null
                || usuario.getTokenRestablecimientoExpiracion().isBefore(LocalDateTime.now())) {
            usuario.setTokenRestablecimientoPassword(null);
            usuario.setTokenRestablecimientoExpiracion(null);
            usuario.setNuevaPasswordPendiente(null);
            usuarioRepository.save(usuario);
            throw new IllegalArgumentException("El token de confirmación ha expirado. Por favor solicite nuevamente el cambio de contraseña.");
        }

        usuario.setPassword(passwordEncoder.encode(dto.getNuevaPassword()));
        usuario.setNuevaPasswordPendiente(null);
        usuario.setTokenRestablecimientoPassword(null);
        usuario.setTokenRestablecimientoExpiracion(null);
        usuario.incrementTokenVersion();

        usuarioRepository.save(usuario);
        if (auditLogService != null) {
            auditLogService.record("PASSWORD_RESET_COMPLETED", "SUCCESS", usuario.getId(), usuario.getRol().name(),
                    "USUARIO", String.valueOf(usuario.getId()), "Password reset confirmed via two-step POST");
        }
        log.info("Contraseña actualizada con éxito para el usuario ID {}", usuario.getId());

        return true;
    }

    private String generarTokenSeguro() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    @Transactional
    public boolean establecerPasswordDoctor(EstablecerPasswordDoctorDTO dto) {
        if (dto.getPassword() == null || dto.getPassword().length() < 12) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 12 caracteres.");
        }
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
        usuario.incrementTokenVersion();
        usuarioRepository.save(usuario);

        log.info("Cuenta de médico activada con éxito para usuario ID {}.", usuario.getId());
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
    public Page<com.consultorio.dto.UsuarioAdminDTO> buscarUsuariosAdminPaginados(String query, Pageable pageable) {
        String cleanQuery = (query != null && !query.trim().isEmpty()) ? query.trim() : "";
        return usuarioRepository.buscarUsuariosAdminPaginados(cleanQuery, pageable);
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
        if (bloquear) {
            usuario.incrementTokenVersion();
        }
        Usuario actualizado = usuarioRepository.save(usuario);
        if (auditLogService != null) {
            auditLogService.record("USER_LOCK_CHANGED", "SUCCESS", null, "ADMIN",
                    "USUARIO", String.valueOf(actualizado.getId()), "Account lock changed to " + bloquear);
        }

        log.info("Estado de bloqueo de usuario ID {} cambiado a: {}", usuario.getId(), bloquear);

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
        if (auditLogService != null) {
            auditLogService.record("USER_DELETED", "SUCCESS", null, "ADMIN",
                    "USUARIO", String.valueOf(usuarioId), "Account deleted by administrator");
        }
        log.info("Usuario ID {} eliminado del sistema por administrador.", usuarioId);
    }
}
