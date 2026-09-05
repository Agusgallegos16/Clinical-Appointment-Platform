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

import com.consultorio.dto.PacienteResponseDTO;
import com.consultorio.mapper.DtoMapper;
import com.consultorio.service.TurnoService;

@RestController
@RequestMapping("/api/pacientes")
@Tag(name = "Gestión de Pacientes y Menores a Cargo", description = "Endpoints para consulta de perfil del paciente, ficha con estadísticas y gestión de menores a cargo del tutor.")
public class PacienteController {

    private final PacienteService pacienteService;
    private final TurnoService turnoService;
    private final SecurityUtils securityUtils;
    private final DtoMapper dtoMapper;

    @Autowired
    public PacienteController(PacienteService pacienteService,
                              TurnoService turnoService,
                              SecurityUtils securityUtils,
                              DtoMapper dtoMapper) {
        this.pacienteService = pacienteService;
        this.turnoService = turnoService;
        this.securityUtils = securityUtils;
        this.dtoMapper = dtoMapper;
    }

    @GetMapping("/mi-perfil")
    @PreAuthorize("hasRole('PACIENTE') or hasRole('SECRETARIA')")
    @Operation(summary = "Obtener los datos del perfil del paciente autenticado (PACIENTE / SECRETARIA)")
    public ResponseEntity<PacienteResponseDTO> obtenerMiPerfil() {
        String emailAutenticado = securityUtils.obtenerEmailUsuarioAutenticado();
        return ResponseEntity.ok(dtoMapper.toDto(pacienteService.obtenerPorUsuarioEmail(emailAutenticado)));
    }

    @PutMapping("/mi-perfil")
    @PreAuthorize("hasRole('PACIENTE') or hasRole('SECRETARIA')")
    @Operation(summary = "Actualizar los datos del perfil del paciente autenticado (PACIENTE / SECRETARIA)")
    public ResponseEntity<PacienteResponseDTO> actualizarMiPerfil(
            @jakarta.validation.Valid @RequestBody com.consultorio.dto.ActualizarPerfilPacienteDTO dto) {
        String emailAutenticado = securityUtils.obtenerEmailUsuarioAutenticado();
        Paciente paciente = pacienteService.obtenerPorUsuarioEmail(emailAutenticado);
        return ResponseEntity.ok(dtoMapper.toDto(pacienteService.actualizarPerfilPaciente(paciente.getId(), dto)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('PACIENTE') or hasRole('ADMIN') or hasRole('DOCTOR') or hasRole('SECRETARIA')")
    @Operation(summary = "Obtener el detalle de un paciente por su ID (PACIENTE / ADMIN / DOCTOR / SECRETARIA)")
    public ResponseEntity<PacienteResponseDTO> obtenerPacientePorId(@PathVariable UUID id) {
        return ResponseEntity.ok(dtoMapper.toDto(pacienteService.obtenerPorId(id)));
    }

    @GetMapping("/buscar-por-dni/{dni}")
    @PreAuthorize("hasRole('SECRETARIA') or hasRole('ADMIN') or hasRole('DOCTOR')")
    @Operation(summary = "Buscar paciente por DNI para autocompletado en agendamiento (SECRETARIA / ADMIN / DOCTOR)")
    public ResponseEntity<PacienteResponseDTO> buscarPorDni(@PathVariable Long dni) {
        return pacienteService.obtenerPorDniOpt(dni)
                .map(dtoMapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/estadisticas")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN') or hasRole('PACIENTE') or hasRole('SECRETARIA')")
    @Operation(summary = "Obtener ficha y métricas estadísticas del paciente (DOCTOR / ADMIN / PACIENTE / SECRETARIA)")
    public ResponseEntity<com.consultorio.dto.PacienteResumenEstadisticasDTO> obtenerEstadisticasPaciente(@PathVariable UUID id) {
        return ResponseEntity.ok(turnoService.obtenerEstadisticasPaciente(id));
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
