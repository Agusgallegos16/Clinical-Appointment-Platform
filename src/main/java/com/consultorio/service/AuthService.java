package com.consultorio.service;

import com.consultorio.domain.Doctor;
import com.consultorio.domain.Paciente;
import com.consultorio.domain.Rol;
import com.consultorio.domain.Usuario;
import com.consultorio.dto.JwtResponseDTO;
import com.consultorio.dto.LoginDTO;
import com.consultorio.security.JwtUtils;
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

    @Autowired
    public AuthService(UsuarioService usuarioService,
                       DoctorService doctorService,
                       PacienteService pacienteService,
                       AuthenticationManager authenticationManager,
                       JwtUtils jwtUtils) {
        this.usuarioService = usuarioService;
        this.doctorService = doctorService;
        this.pacienteService = pacienteService;
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
    }

    @Transactional(readOnly = true)
    public JwtResponseDTO autenticar(LoginDTO loginDTO) {
        Usuario usuario = usuarioService.obtenerPorEmail(loginDTO.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Credenciales inválidas."));

        if (usuario.isBloqueado()) {
            throw new IllegalArgumentException("Error: Su cuenta ha sido bloqueada por el administrador.");
        }

        if (!usuario.isEmailVerificado() || !usuario.isActivo()) {
            throw new IllegalArgumentException("Debe confirmar su correo electrónico antes de iniciar sesión. Por favor revise su bandeja de entrada.");
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginDTO.getEmail(), loginDTO.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String token = jwtUtils.generarToken(usuario.getEmail(), usuario.getRol().name());

        UUID entidadId = null;
        if (usuario.getRol() == Rol.PACIENTE) {
            Optional<Paciente> paciente = pacienteService.obtenerPorUsuarioId(usuario.getId());
            if (paciente.isPresent()) {
                entidadId = paciente.get().getId();
            }
        } else if (usuario.getRol() == Rol.DOCTOR) {
            Optional<Doctor> doctor = doctorService.obtenerPorUsuarioId(usuario.getId());
            if (doctor.isPresent()) {
                entidadId = doctor.get().getId();
            }
        }

        return JwtResponseDTO.builder()
                .token(token)
                .tipo("Bearer")
                .id(usuario.getId())
                .email(usuario.getEmail())
                .rol(usuario.getRol())
                .entidadId(entidadId)
                .build();
    }
}
