package com.serviciodegesrtiondepacientes.controller;

import com.serviciodegesrtiondepacientes.domain.pacientes.Paciente;
import com.serviciodegesrtiondepacientes.service.PacienteService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pacientes")
public class PacienteController {

    private final PacienteService pacienteService;

    @Autowired
    public PacienteController(PacienteService pacienteService) {
        this.pacienteService = pacienteService;
    }

    // GET /api/pacientes?page=0&size=20&sort=apellido,asc
    @GetMapping
    public Page<Paciente> listarPacientes(@PageableDefault(size = 20, sort = "apellido") Pageable pageable) {
        return pacienteService.obtenerTodosLosPacientes(pageable);
    }

    // GET /api/pacientes/{id}
    @GetMapping("/{id}")
    public ResponseEntity<Paciente> obtenerPacientePorId(@PathVariable Long id) {
        Paciente paciente = pacienteService.obtenerPacientePorId(id);
        return ResponseEntity.ok(paciente);
    }

    // GET /api/pacientes/dni/{dni}
    @GetMapping("/dni/{dni}")
    public ResponseEntity<Paciente> obtenerPacientePorDni(@PathVariable Long dni) {
        Paciente paciente = pacienteService.obtenerPacientePorDni(dni);
        return ResponseEntity.ok(paciente);
    }

    // GET /api/pacientes/buscar?termino=Juan&page=0&size=20
    @GetMapping("/buscar")
    public Page<Paciente> buscarPacientes(
            @RequestParam String termino,
            @PageableDefault(size = 20, sort = "apellido") Pageable pageable) {
        return pacienteService.buscarPorNombreOApellido(termino, pageable);
    }

    // POST /api/pacientes
    @PostMapping
    public ResponseEntity<Paciente> crearPaciente(@Valid @RequestBody Paciente paciente) {
        Paciente nuevoPaciente = pacienteService.guardarPaciente(paciente);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevoPaciente);
    }

    // PUT /api/pacientes/{id}
    @PutMapping("/{id}")
    public ResponseEntity<Paciente> actualizarPaciente(
            @PathVariable Long id,
            @Valid @RequestBody Paciente paciente) {
        Paciente pacienteActualizado = pacienteService.actualizarPaciente(id, paciente);
        return ResponseEntity.ok(pacienteActualizado);
    }

    // DELETE /api/pacientes/{id} (borrado lógico)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivarPaciente(@PathVariable Long id) {
        pacienteService.desactivarPaciente(id);
        return ResponseEntity.noContent().build();
    }
}
