package com.consultorio.controller;

import com.consultorio.domain.Especialidad;
import com.consultorio.dto.CrearEspecialidadDTO;
import com.consultorio.dto.EspecialidadResponseDTO;
import com.consultorio.mapper.DtoMapper;
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
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/especialidades")
@Tag(name = "Especialidades Médicas", description = "Endpoints para consultar y gestionar las especialidades médicas del consultorio.")
public class EspecialidadController {

    private final EspecialidadService especialidadService;
    private final DtoMapper dtoMapper;

    @Autowired
    public EspecialidadController(EspecialidadService especialidadService, DtoMapper dtoMapper) {
        this.especialidadService = especialidadService;
        this.dtoMapper = dtoMapper;
    }

    @GetMapping
    @Operation(summary = "Listar todas las especialidades médicas")
    public ResponseEntity<List<EspecialidadResponseDTO>> listar() {
        List<EspecialidadResponseDTO> lista = especialidadService.listarTodas().stream()
                .map(dtoMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(lista);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear una nueva especialidad médica")
    public ResponseEntity<EspecialidadResponseDTO> crear(@Valid @RequestBody CrearEspecialidadDTO dto) {
        Especialidad nueva = especialidadService.crearEspecialidad(dto.getNombre(), dto.getDescripcion());
        return ResponseEntity.status(HttpStatus.CREATED).body(dtoMapper.toDto(nueva));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar una especialidad médica existente")
    public ResponseEntity<EspecialidadResponseDTO> actualizar(@PathVariable Long id, @Valid @RequestBody CrearEspecialidadDTO dto) {
        Especialidad actualizada = especialidadService.actualizarEspecialidad(id, dto.getNombre(), dto.getDescripcion());
        return ResponseEntity.ok(dtoMapper.toDto(actualizada));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Eliminar una especialidad médica por su ID")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        especialidadService.eliminarEspecialidad(id);
        return ResponseEntity.noContent().build();
    }
}
