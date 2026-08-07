package com.consultorio.controller;

import com.consultorio.domain.EstadoTurno;
import com.consultorio.dto.TurnoReservaDTO;
import com.consultorio.dto.TurnoResponseDTO;
import com.consultorio.service.TurnoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/turnos")
@Tag(name = "Gestión de Turnos", description = "Endpoints para reserva, cancelación y actualización del estado de turnos médicos.")
public class TurnoController {

    private final TurnoService turnoService;

    @Autowired
    public TurnoController(TurnoService turnoService) {
        this.turnoService = turnoService;
    }

    @PostMapping
    @PreAuthorize("hasRole('PACIENTE') or hasRole('ADMIN')")
    @Operation(summary = "Reservar un nuevo turno médico (PACIENTE / ADMIN)")
    public ResponseEntity<TurnoResponseDTO> reservarTurno(@Valid @RequestBody TurnoReservaDTO dto) {
        TurnoResponseDTO nuevoTurno = turnoService.reservarTurno(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevoTurno);
    }

    @GetMapping("/paciente/{pacienteId}")
    @PreAuthorize("hasRole('PACIENTE') or hasRole('ADMIN')")
    @Operation(summary = "Consultar el historial y turnos agendados de un paciente (PACIENTE / ADMIN)")
    public ResponseEntity<List<TurnoResponseDTO>> obtenerTurnosPorPaciente(@PathVariable Long pacienteId) {
        return ResponseEntity.ok(turnoService.obtenerTurnosPorPaciente(pacienteId));
    }

    @PutMapping("/{id}/cancelar")
    @PreAuthorize("hasRole('PACIENTE') or hasRole('DOCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Cancelar un turno agendado (PACIENTE / DOCTOR / ADMIN)")
    public ResponseEntity<TurnoResponseDTO> cancelarTurno(@PathVariable Long id) {
        return ResponseEntity.ok(turnoService.cancelarTurno(id));
    }

    @PutMapping("/{id}/estado")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Actualizar el estado de un turno (ej. COMPLETADO, AUSENTE) (DOCTOR / ADMIN)")
    public ResponseEntity<TurnoResponseDTO> cambiarEstadoTurno(
            @PathVariable Long id,
            @RequestParam EstadoTurno nuevoEstado) {
        return ResponseEntity.ok(turnoService.cambiarEstadoTurno(id, nuevoEstado));
    }
}
