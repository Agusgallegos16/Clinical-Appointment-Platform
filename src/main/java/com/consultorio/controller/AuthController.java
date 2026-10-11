package com.consultorio.controller;

import com.consultorio.domain.Doctor;
import com.consultorio.domain.Paciente;
import com.consultorio.dto.*;
import com.consultorio.service.AuthService;
import com.consultorio.service.DoctorService;
import com.consultorio.service.PacienteService;
import com.consultorio.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticación y Registro", description = "Endpoints para registro, verificación de email, restablecimiento de clave y Token JWT.")
public class AuthController {

    private final PacienteService pacienteService;
    private final DoctorService doctorService;
    private final UsuarioService usuarioService;
    private final AuthService authService;

    @Autowired
    public AuthController(PacienteService pacienteService,
                          DoctorService doctorService,
                          UsuarioService usuarioService,
                          AuthService authService) {
        this.pacienteService = pacienteService;
        this.doctorService = doctorService;
        this.usuarioService = usuarioService;
        this.authService = authService;
    }

    @PostMapping("/registro-paciente")
    @Operation(summary = "Registro de un nuevo Paciente (Público)")
    public ResponseEntity<Paciente> registrarPaciente(@Valid @RequestBody RegistroPacienteDTO dto) {
        Paciente paciente = pacienteService.registrarPaciente(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(paciente);
    }

    @PostMapping("/registro-doctor")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Registro de un nuevo Doctor (Exclusivo ADMIN)")
    public ResponseEntity<Doctor> registrarDoctor(@Valid @RequestBody RegistroDoctorDTO dto) {
        Doctor doctor = doctorService.registrarDoctor(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(doctor);
    }

    @GetMapping("/confirmar-email")
    @Operation(summary = "Confirmación de correo electrónico vía token (Público)")
    public ResponseEntity<Map<String, String>> confirmarEmail(@RequestParam("token") String token) {
        pacienteService.confirmarEmail(token);
        return ResponseEntity.ok(Map.of("message", "¡Correo electrónico verificado exitosamente! Tu cuenta ha sido activada."));
    }

    @PostMapping("/solicitar-restablecimiento-password")
    @Operation(summary = "Solicitar restablecimiento de contraseña vía email (Público)")
    public ResponseEntity<Map<String, String>> solicitarRestablecimientoPassword(@Valid @RequestBody SolicitudRestablecerPasswordDTO dto) {
        usuarioService.solicitarRestablecimientoPassword(dto);
        return ResponseEntity.ok(Map.of("message", "Se ha enviado un enlace de confirmación a tu correo electrónico para cambiar la contraseña."));
    }

    @GetMapping("/confirmar-restablecimiento-password")
    @Operation(summary = "Confirmar el cambio de contraseña vía token (Público)")
    public ResponseEntity<Map<String, String>> confirmarRestablecimientoPassword(@RequestParam("token") String token) {
        usuarioService.confirmarRestablecimientoPassword(token);
        return ResponseEntity.ok(Map.of("message", "Contraseña cambiada con éxito. Ya puedes iniciar sesión con tu nueva contraseña."));
    }

    @PostMapping("/establecer-password-doctor")
    @Operation(summary = "Establecer la contraseña y activar cuenta de médico vía token (Público)")
    public ResponseEntity<Map<String, String>> establecerPasswordDoctor(@Valid @RequestBody EstablecerPasswordDoctorDTO dto) {
        usuarioService.establecerPasswordDoctor(dto);
        return ResponseEntity.ok(Map.of("message", "¡Contraseña configurada exitosamente! Tu cuenta de profesional médico ha sido activada. Ya puedes iniciar sesión."));
    }

    @PostMapping("/login")
    @Operation(summary = "Iniciar Sesión y obtener Token JWT Bearer")
    public ResponseEntity<JwtResponseDTO> login(@Valid @RequestBody LoginDTO loginDTO) {
        return ResponseEntity.ok(authService.autenticar(loginDTO));
    }
}
