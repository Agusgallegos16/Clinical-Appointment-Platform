package com.consultorio.security;

import com.consultorio.domain.Doctor;
import com.consultorio.repository.TurnoRepository;
import com.consultorio.service.PacienteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("securityUtils")
public class SecurityUtils {

    private final PacienteService pacienteService;
    private final TurnoRepository turnoRepository;

    @Autowired
    public SecurityUtils(PacienteService pacienteService, TurnoRepository turnoRepository) {
        this.pacienteService = pacienteService;
        this.turnoRepository = turnoRepository;
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

        // Personal administrativo tiene acceso legítimo de gestión
        if (esAdmin() || esSecretaria()) {
            return true;
        }

        String email = obtenerEmailUsuarioAutenticado();
        if (email == null) {
            return false;
        }

        // Si es DOCTOR, solo puede acceder a pacientes con los que tiene relación asistencial (turno)
        boolean esDoctor = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_DOCTOR"));
        if (esDoctor) {
            return turnoRepository.existsByDoctorUsuarioEmailIgnoreCaseAndPacienteId(email, pacienteId);
        }

        // Si es PACIENTE, verificar que sea titular o tutor legal
        return pacienteService.esPacientePropietarioOTutor(pacienteId, email);
    }

    public void verificarAccesoDoctor(Doctor doctor, boolean permitirSecretaria) {
        String emailAutenticado = obtenerEmailUsuarioAutenticado();
        if (emailAutenticado == null) {
            throw new AccessDeniedException("Authentication is required to perform this action.");
        }
        if (esAdmin() || (permitirSecretaria && esSecretaria())) {
            return;
        }
        if (doctor != null
                && doctor.getUsuario() != null
                && doctor.getUsuario().getEmail() != null
                && doctor.getUsuario().getEmail().equalsIgnoreCase(emailAutenticado)) {
            return;
        }
        throw new AccessDeniedException("Access denied: you do not have permission to access another doctor's resources.");
    }
}
