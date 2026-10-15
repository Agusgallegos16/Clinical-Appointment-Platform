package com.consultorio.service;

import com.consultorio.domain.AuthProvider;
import com.consultorio.domain.Paciente;
import com.consultorio.domain.Rol;
import com.consultorio.domain.Usuario;
import com.consultorio.repository.PacienteRepository;
import com.consultorio.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class OAuth2ProvisioningService {

    private static final Logger log = LoggerFactory.getLogger(OAuth2ProvisioningService.class);

    private final UsuarioRepository usuarioRepository;
    private final PacienteRepository pacienteRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public OAuth2ProvisioningService(UsuarioRepository usuarioRepository,
                                     PacienteRepository pacienteRepository,
                                     PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.pacienteRepository = pacienteRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Usuario procesarUsuarioOAuth2(String email, String googleId, String name, String givenName, String familyName) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("El email recibido desde Google OAuth es nulo o vacío");
        }

        String normalizedEmail = email.trim().toLowerCase();
        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmailIgnoreCase(normalizedEmail);

        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            if (usuario.isBloqueado()) {
                log.warn("⛔ Intento de acceso OAuth2 para cuenta bloqueada: {}", normalizedEmail);
                throw new IllegalStateException("USER_BLOCKED");
            }

            if (usuario.getGoogleId() == null && googleId != null) {
                usuario.setGoogleId(googleId);
                log.info("🔗 Cuenta vinculada con Google ID para usuario: {}", normalizedEmail);
            }

            // Al autenticar vía Google, validamos y activamos la cuenta automáticamente
            if (!usuario.isEmailVerificado() || !usuario.isActivo()) {
                usuario.setEmailVerificado(true);
                usuario.setActivo(true);
                usuario.setTokenVerificacionEmail(null);
                usuario.setTokenVerificacionExpiracion(null);
            }

            // Si el usuario es de rol PACIENTE pero no tiene entidad Paciente asociada, la creamos
            if (usuario.getRol() == Rol.PACIENTE && pacienteRepository.findByUsuarioId(usuario.getId()).isEmpty()) {
                crearPacienteParaUsuario(usuario, name, givenName, familyName);
            }

            return usuarioRepository.save(usuario);
        }

        // Just-In-Time Provisioning: Creación de nuevo Usuario y Paciente
        log.info("👤 Creando nuevo usuario JIT vía Google OAuth: {}", normalizedEmail);

        Usuario nuevoUsuario = Usuario.builder()
                .email(normalizedEmail)
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .rol(Rol.PACIENTE)
                .activo(true)
                .emailVerificado(true)
                .googleId(googleId)
                .authProvider(AuthProvider.GOOGLE)
                .build();

        Usuario usuarioGuardado = usuarioRepository.save(nuevoUsuario);
        crearPacienteParaUsuario(usuarioGuardado, name, givenName, familyName);

        return usuarioGuardado;
    }

    private void crearPacienteParaUsuario(Usuario usuario, String name, String givenName, String familyName) {
        String nombre = (givenName != null && !givenName.isBlank()) ? givenName.trim() : (name != null ? name.trim() : "Paciente");
        String apellido = (familyName != null && !familyName.isBlank()) ? familyName.trim() : "";

        if (apellido.isBlank() && name != null && name.contains(" ")) {
            int idx = name.lastIndexOf(" ");
            nombre = name.substring(0, idx).trim();
            apellido = name.substring(idx + 1).trim();
        }

        if (apellido.isBlank()) {
            apellido = ".";
        }

        Paciente paciente = Paciente.builder()
                .usuario(usuario)
                .nombre(nombre)
                .apellido(apellido)
                .email(usuario.getEmail())
                .dni(null)
                .build();

        pacienteRepository.save(paciente);
        log.info("✅ Perfil de Paciente creado automáticamente para: {}", usuario.getEmail());
    }
}
