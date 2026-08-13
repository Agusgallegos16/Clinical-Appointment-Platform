package com.consultorio.controller;

import com.consultorio.domain.Paciente;
import com.consultorio.security.SecurityUtils;
import com.consultorio.service.PacienteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/pacientes")
@Tag(name = "Gestión de Pacientes", description = "Endpoints para consulta de datos del paciente.")
public class PacienteController {

    private final PacienteService pacienteService;
    private final SecurityUtils securityUtils;

    @Autowired
    public PacienteController(PacienteService pacienteService, SecurityUtils securityUtils) {
        this.pacienteService = pacienteService;
        this.securityUtils = securityUtils;
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('PACIENTE') or hasRole('ADMIN') or hasRole('DOCTOR')")
    @Operation(summary = "Obtener el detalle de un paciente por su ID (PACIENTE / ADMIN / DOCTOR)")
    public ResponseEntity<Paciente> obtenerPacientePorId(@PathVariable UUID id) {
        return ResponseEntity.ok(pacienteService.obtenerPorId(id));
    }

    @GetMapping("/{id}/estadisticas")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN') or hasRole('PACIENTE')")
    @Operation(summary = "Obtener ficha y métricas estadísticas del paciente (DOCTOR / ADMIN / PACIENTE)")
    public ResponseEntity<com.consultorio.dto.PacienteResumenEstadisticasDTO> obtenerEstadisticasPaciente(@PathVariable UUID id) {
        return ResponseEntity.ok(pacienteService.obtenerEstadisticasPaciente(id));
    }

    @PostMapping("/menores")
    @PreAuthorize("hasRole('PACIENTE') or hasRole('ADMIN')")
    @Operation(summary = "Registrar un menor a cargo del paciente tutor (PACIENTE / ADMIN)")
    public ResponseEntity<com.consultorio.dto.PacienteMenorResponseDTO> registrarMenor(
            @jakarta.validation.Valid @RequestBody com.consultorio.dto.RegistroMenorDTO dto) {
        String emailAutenticado = securityUtils.obtenerEmailUsuarioAutenticado();
        Paciente tutor = pacienteService.obtenerPorUsuarioEmail(emailAutenticado);
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED)
                .body(pacienteService.registrarMenor(tutor.getId(), dto));
    }

    @GetMapping("/menores")
    @PreAuthorize("hasRole('PACIENTE') or hasRole('ADMIN')")
    @Operation(summary = "Listar los menores a cargo del paciente tutor (PACIENTE / ADMIN)")
    public ResponseEntity<java.util.List<com.consultorio.dto.PacienteMenorResponseDTO>> listarMenores() {
        String emailAutenticado = securityUtils.obtenerEmailUsuarioAutenticado();
        Paciente tutor = pacienteService.obtenerPorUsuarioEmail(emailAutenticado);
        return ResponseEntity.ok(pacienteService.listarMenoresDeTutor(tutor.getId()));
    }

    @DeleteMapping("/menores/{menorId}")
    @PreAuthorize("hasRole('PACIENTE') or hasRole('ADMIN')")
    @Operation(summary = "Desvincular a un menor a cargo del paciente tutor (PACIENTE / ADMIN)")
    public ResponseEntity<Void> desvincularMenor(@PathVariable UUID menorId) {
        String emailAutenticado = securityUtils.obtenerEmailUsuarioAutenticado();
        Paciente tutor = pacienteService.obtenerPorUsuarioEmail(emailAutenticado);
        pacienteService.desvincularMenor(tutor.getId(), menorId);
        return ResponseEntity.noContent().build();
    }
}
