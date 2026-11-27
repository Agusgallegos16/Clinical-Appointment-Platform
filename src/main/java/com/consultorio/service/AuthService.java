package com.consultorio.service;

import com.consultorio.domain.Doctor;
import com.consultorio.domain.Paciente;
import com.consultorio.domain.Rol;
import com.consultorio.domain.Usuario;
import com.consultorio.dto.JwtResponseDTO;
import com.consultorio.dto.LoginDTO;
import com.consultorio.security.JwtUtils;
import com.consultorio.security.oauth2.OAuth2OneTimeCodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private final UsuarioService usuarioService;
    private final DoctorService doctorService;
    private final PacienteService pacienteService;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final OAuth2OneTimeCodeService oauth2OneTimeCodeService;
    private final AuditLogService auditLogService;

    @Autowired
    public AuthService(UsuarioService usuarioService,
                       DoctorService doctorService,
                       PacienteService pacienteService,
                       AuthenticationManager authenticationManager,
                       JwtUtils jwtUtils,
                       OAuth2OneTimeCodeService oauth2OneTimeCodeService,
                       AuditLogService auditLogService) {
        this.usuarioService = usuarioService;
        this.doctorService = doctorService;
        this.pacienteService = pacienteService;
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
        this.oauth2OneTimeCodeService = oauth2OneTimeCodeService;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public JwtResponseDTO autenticar(LoginDTO loginDTO) {
        Optional<Usuario> usuarioOpt = usuarioService.obtenerPorEmail(loginDTO.getEmail());
        if (usuarioOpt.isEmpty()) {
            recordAudit("AUTH_LOGIN_FAILURE", "FAILURE", null, null, "USUARIO", null, "Failed login: unknown account");
            throw new IllegalArgumentException("Credenciales inválidas.");
        }
        Usuario usuario = usuarioOpt.get();

        if (usuario.isBloqueado()) {
            recordAudit("AUTH_LOGIN_FAILURE", "DENIED", usuario.getId(), usuario.getRol().name(), "USUARIO", String.valueOf(usuario.getId()), "Blocked account login attempt");
            throw new IllegalArgumentException("Error: Su cuenta ha sido bloqueada por el administrador.");
        }

        if (!usuario.isEmailVerificado() || !usuario.isActivo()) {
            recordAudit("AUTH_LOGIN_FAILURE", "DENIED", usuario.getId(), usuario.getRol().name(), "USUARIO", String.valueOf(usuario.getId()), "Inactive or unverified account login attempt");
            throw new IllegalArgumentException("Debe confirmar su correo electrónico antes de iniciar sesión. Por favor revise su bandeja de entrada.");
        }

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginDTO.getEmail(), loginDTO.getPassword())
            );
        } catch (RuntimeException ex) {
            recordAudit("AUTH_LOGIN_FAILURE", "FAILURE", usuario.getId(), usuario.getRol().name(), "USUARIO", String.valueOf(usuario.getId()), "Invalid password");
            throw ex;
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String token = jwtUtils.generarToken(usuario.getEmail(), usuario.getRol().name(), usuario.getTokenVersionOrZero());
        recordAudit("AUTH_LOGIN_SUCCESS", "SUCCESS", usuario.getId(), usuario.getRol().name(), "USUARIO", String.valueOf(usuario.getId()), "JWT login issued");

        return construirJwtResponse(usuario, token, "Bearer");
    }

    @Transactional(readOnly = true)
    public JwtResponseDTO intercambiarCodigoOAuth2(String code) {
        Long userId = oauth2OneTimeCodeService.consumeCode(code);
        Usuario usuario = usuarioService.obtenerPorId(userId);
        if (usuario.isBloqueado()) {
            recordAudit("AUTH_OAUTH2_EXCHANGE", "DENIED", usuario.getId(), usuario.getRol().name(), "USUARIO", String.valueOf(usuario.getId()), "Blocked account OAuth2 exchange");
            throw new IllegalArgumentException("Error: Su cuenta ha sido bloqueada por el administrador.");
        }
        if (!usuario.isActivo()) {
            throw new IllegalArgumentException("La cuenta de usuario se encuentra inactiva.");
        }
        String token = jwtUtils.generarToken(usuario.getEmail(), usuario.getRol().name(), usuario.getTokenVersionOrZero());
        recordAudit("AUTH_OAUTH2_EXCHANGE", "SUCCESS", usuario.getId(), usuario.getRol().name(), "USUARIO", String.valueOf(usuario.getId()), "OAuth2 one-time code exchanged");
        return construirJwtResponse(usuario, token, "Bearer");
    }

    @Transactional(readOnly = true)
    public JwtResponseDTO obtenerUsuarioAutenticado(String email) {
        Usuario usuario = usuarioService.obtenerPorEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));

        if (usuario.isBloqueado()) {
            throw new IllegalArgumentException("Error: Su cuenta ha sido bloqueada por el administrador.");
        }

        // Do not re-mint a fresh JWT on /api/auth/me
        return construirJwtResponse(usuario, null, null);
    }

    @Transactional
    public void revocarSesion(String email) {
        usuarioService.obtenerPorEmail(email).ifPresent(u ->
                recordAudit("AUTH_LOGOUT", "SUCCESS", u.getId(), u.getRol().name(), "USUARIO", String.valueOf(u.getId()), "Session revoked via tokenVersion bump"));
        usuarioService.revocarSesionesUsuario(email);
    }

    private void recordAudit(String eventType, String outcome, Long actorUserId, String actorRole,
                             String targetType, String targetId, String summary) {
        if (auditLogService != null) {
            auditLogService.record(eventType, outcome, actorUserId, actorRole, targetType, targetId, summary);
        }
    }

    private JwtResponseDTO construirJwtResponse(Usuario usuario, String token, String tipo) {
        UUID entidadId = null;
        Boolean perfilCompleto = null;
        if (usuario.getRol() == Rol.PACIENTE) {
            Optional<Paciente> paciente = pacienteService.obtenerPorUsuarioId(usuario.getId());
            if (paciente.isPresent()) {
                entidadId = paciente.get().getId();
                perfilCompleto = paciente.get().isPerfilCompleto();
            } else {
                perfilCompleto = false;
            }
        } else if (usuario.getRol() == Rol.DOCTOR) {
            Optional<Doctor> doctor = doctorService.obtenerPorUsuarioId(usuario.getId());
            if (doctor.isPresent()) {
                entidadId = doctor.get().getId();
            }
            perfilCompleto = true;
        } else {
            perfilCompleto = true;
        }

        return JwtResponseDTO.builder()
                .token(token)
                .tipo(tipo)
                .id(usuario.getId())
                .email(usuario.getEmail())
                .rol(usuario.getRol())
                .entidadId(entidadId)
                .perfilCompleto(perfilCompleto)
                .build();
    }
}
