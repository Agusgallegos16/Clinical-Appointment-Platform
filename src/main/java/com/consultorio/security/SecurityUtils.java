package com.consultorio.security;

import com.consultorio.service.PacienteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("securityUtils")
public class SecurityUtils {

    private final PacienteService pacienteService;

    @Autowired
    public SecurityUtils(PacienteService pacienteService) {
        this.pacienteService = pacienteService;
    }

    public String obtenerEmailUsuarioAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            if (authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails userDetails) {
                return userDetails.getUsername();
            }
            return authentication.getName();
        }
        return null;
    }

    public Long obtenerIdUsuarioAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl userDetails) {
            return userDetails.getId();
        }
        return null;
    }

    public boolean esAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    public boolean esSecretaria() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SECRETARIA"));
    }

    public boolean puedeAccederAPaciente(UUID pacienteId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        // Personal administrativo y médico tiene acceso legítimo de gestión
        boolean tieneRolPersonal = authentication.getAuthorities().stream().anyMatch(a ->
                a.getAuthority().equals("ROLE_ADMIN") ||
                a.getAuthority().equals("ROLE_SECRETARIA") ||
                a.getAuthority().equals("ROLE_DOCTOR"));

        if (tieneRolPersonal) {
            return true;
        }

        // Si es PACIENTE, verificar que sea titular o tutor legal
        String email = obtenerEmailUsuarioAutenticado();
        if (email == null) {
            return false;
        }
        return pacienteService.esPacientePropietarioOTutor(pacienteId, email);
    }
}
