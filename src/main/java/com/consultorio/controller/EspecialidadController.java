package com.consultorio.controller;

import com.consultorio.domain.Especialidad;
import com.consultorio.service.EspecialidadService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/especialidades")
public class EspecialidadController {

    private final EspecialidadService especialidadService;

    @Autowired
    public EspecialidadController(EspecialidadService especialidadService) {
        this.especialidadService = especialidadService;
    }

    @GetMapping
    public ResponseEntity<List<Especialidad>> listar() {
        return ResponseEntity.ok(especialidadService.listarTodas());
    }

    @PostMapping
    public ResponseEntity<Especialidad> crear(@Valid @RequestBody Especialidad especialidad) {
        Especialidad nueva = especialidadService.crearEspecialidad(especialidad);
        return ResponseEntity.status(HttpStatus.CREATED).body(nueva);
    }
}
