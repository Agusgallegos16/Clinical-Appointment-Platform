package com.consultorio.controller;

import com.consultorio.domain.Doctor;
import com.consultorio.domain.Paciente;
import com.consultorio.domain.Rol;
import com.consultorio.domain.Usuario;
import com.consultorio.dto.*;
import com.consultorio.repository.DoctorRepository;
import com.consultorio.repository.PacienteRepository;
import com.consultorio.repository.UsuarioRepository;
import com.consultorio.security.JwtUtils;
import com.consultorio.service.DoctorService;
import com.consultorio.service.PacienteService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final PacienteService pacienteService;
    private final DoctorService doctorService;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final UsuarioRepository usuarioRepository;
    private final PacienteRepository pacienteRepository;
    private final DoctorRepository doctorRepository;

    @Autowired
    public AuthController(PacienteService pacienteService,
                          DoctorService doctorService,
                          AuthenticationManager authenticationManager,
                          JwtUtils jwtUtils,
                          UsuarioRepository usuarioRepository,
                          PacienteRepository pacienteRepository,
                          DoctorRepository doctorRepository) {
        this.pacienteService = pacienteService;
        this.doctorService = doctorService;
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
        this.usuarioRepository = usuarioRepository;
        this.pacienteRepository = pacienteRepository;
        this.doctorRepository = doctorRepository;
    }

    @PostMapping("/registro-paciente")
    public ResponseEntity<Paciente> registrarPaciente(@Valid @RequestBody RegistroPacienteDTO dto) {
        Paciente paciente = pacienteService.registrarPaciente(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(paciente);
    }

    @PostMapping("/registro-doctor")
    public ResponseEntity<Doctor> registrarDoctor(@Valid @RequestBody RegistroDoctorDTO dto) {
        Doctor doctor = doctorService.registrarDoctor(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(doctor);
    }

    @PostMapping("/login")
    public ResponseEntity<JwtResponseDTO> login(@Valid @RequestBody LoginDTO loginDTO) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginDTO.getEmail(), loginDTO.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        Usuario usuario = usuarioRepository.findByEmail(loginDTO.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        String token = jwtUtils.generarToken(usuario.getEmail(), usuario.getRol().name());

        Long entidadId = null;
        if (usuario.getRol() == Rol.PACIENTE) {
            Optional<Paciente> paciente = pacienteRepository.findByUsuarioId(usuario.getId());
            if (paciente.isPresent()) entidadId = paciente.get().getId();
        } else if (usuario.getRol() == Rol.DOCTOR) {
            Optional<Doctor> doctor = doctorRepository.findByUsuarioId(usuario.getId());
            if (doctor.isPresent()) entidadId = doctor.get().getId();
        }

        JwtResponseDTO response = JwtResponseDTO.builder()
                .token(token)
                .tipo("Bearer")
                .id(usuario.getId())
                .email(usuario.getEmail())
                .rol(usuario.getRol())
                .entidadId(entidadId)
                .build();

        return ResponseEntity.ok(response);
    }
}
