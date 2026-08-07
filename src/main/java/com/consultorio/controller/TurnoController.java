package com.consultorio.controller;

import com.consultorio.domain.EstadoTurno;
import com.consultorio.dto.TurnoReservaDTO;
import com.consultorio.dto.TurnoResponseDTO;
import com.consultorio.service.TurnoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/turnos")
public class TurnoController {

    private final TurnoService turnoService;

    @Autowired
    public TurnoController(TurnoService turnoService) {
        this.turnoService = turnoService;
    }

    // Reservar un nuevo turno
    @PostMapping
    public ResponseEntity<TurnoResponseDTO> reservarTurno(@Valid @RequestBody TurnoReservaDTO dto) {
        TurnoResponseDTO turno = turnoService.reservarTurno(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(turno);
    }

    // Cancelar turno
    @PutMapping("/{id}/cancelar")
    public ResponseEntity<TurnoResponseDTO> cancelarTurno(@PathVariable Long id) {
        TurnoResponseDTO turno = turnoService.cancelarTurno(id);
        return ResponseEntity.ok(turno);
    }

    // Cambiar estado del turno (ej. COMPLETADO, AUSENTE)
    @PutMapping("/{id}/estado")
    public ResponseEntity<TurnoResponseDTO> cambiarEstado(
            @PathVariable Long id,
            @RequestParam EstadoTurno nuevoEstado) {
        TurnoResponseDTO turno = turnoService.cambiarEstadoTurno(id, nuevoEstado);
        return ResponseEntity.ok(turno);
    }

    // Mis turnos (como paciente)
    @GetMapping("/paciente/{pacienteId}")
    public ResponseEntity<List<TurnoResponseDTO>> obtenerTurnosPorPaciente(@PathVariable Long pacienteId) {
        return ResponseEntity.ok(turnoService.obtenerTurnosPorPaciente(pacienteId));
    }
}
