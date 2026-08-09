package com.consultorio.service;

import com.consultorio.domain.Usuario;
import com.consultorio.dto.SolicitudRestablecerPasswordDTO;
import com.consultorio.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class UsuarioService {

    private static final Logger log = LoggerFactory.getLogger(UsuarioService.class);

    private final UsuarioRepository usuarioRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UsuarioService(UsuarioRepository usuarioRepository,
                          EmailService emailService,
                          PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
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
}
