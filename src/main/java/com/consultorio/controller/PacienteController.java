package com.consultorio.controller;

import com.consultorio.domain.Paciente;
import com.consultorio.service.PacienteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pacientes")
@Tag(name = "Gestión de Pacientes", description = "Endpoints para consulta de datos del paciente.")
public class PacienteController {

    private final PacienteService pacienteService;

    @Autowired
    public PacienteController(PacienteService pacienteService) {
        this.pacienteService = pacienteService;
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('PACIENTE') or hasRole('ADMIN') or hasRole('DOCTOR')")
    @Operation(summary = "Obtener el detalle de un paciente por su ID (PACIENTE / ADMIN / DOCTOR)")
    public ResponseEntity<Paciente> obtenerPacientePorId(@PathVariable Long id) {
        return ResponseEntity.ok(pacienteService.obtenerPorId(id));
    }

    @GetMapping("/{id}/estadisticas")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN') or hasRole('PACIENTE')")
    @Operation(summary = "Obtener ficha y métricas estadísticas del paciente (DOCTOR / ADMIN / PACIENTE)")
    public ResponseEntity<com.consultorio.dto.PacienteResumenEstadisticasDTO> obtenerEstadisticasPaciente(@PathVariable Long id) {
        return ResponseEntity.ok(pacienteService.obtenerEstadisticasPaciente(id));
    }
}
