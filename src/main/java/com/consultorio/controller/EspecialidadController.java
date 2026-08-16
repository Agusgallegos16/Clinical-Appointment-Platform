package com.consultorio.controller;

import com.consultorio.domain.Especialidad;
import com.consultorio.service.EspecialidadService;
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
@RequestMapping("/api/especialidades")
@Tag(name = "Especialidades Médicas", description = "Endpoints para consultar y gestionar las especialidades médicas del consultorio.")
public class EspecialidadController {

    private final EspecialidadService especialidadService;

    @Autowired
    public EspecialidadController(EspecialidadService especialidadService) {
        this.especialidadService = especialidadService;
    }

    @GetMapping
    @Operation(summary = "Listar todas las especialidades médicas (Público)")
    public ResponseEntity<List<Especialidad>> listar() {
        return ResponseEntity.ok(especialidadService.listarTodas());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear una nueva especialidad médica (Exclusivo ADMIN)")
    public ResponseEntity<Especialidad> crear(@Valid @RequestBody Especialidad especialidad) {
        Especialidad nueva = especialidadService.crearEspecialidad(especialidad);
        return ResponseEntity.status(HttpStatus.CREATED).body(nueva);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar una especialidad médica existente (Exclusivo ADMIN)")
    public ResponseEntity<Especialidad> actualizar(@PathVariable Long id, @Valid @RequestBody Especialidad especialidad) {
        Especialidad actualizada = especialidadService.actualizarEspecialidad(id, especialidad);
        return ResponseEntity.ok(actualizada);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Eliminar una especialidad médica por su ID (Exclusivo ADMIN)")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        especialidadService.eliminarEspecialidad(id);
        return ResponseEntity.noContent().build();
    }
}
