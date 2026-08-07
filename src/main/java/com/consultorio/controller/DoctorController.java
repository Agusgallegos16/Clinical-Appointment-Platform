package com.consultorio.controller;

import com.consultorio.domain.Doctor;
import com.consultorio.domain.HorarioAtencion;
import com.consultorio.domain.PlantillaAgenda;
import com.consultorio.dto.*;
import com.consultorio.service.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/doctores")
public class DoctorController {

    private final DoctorService doctorService;
    private final DisponibilidadService disponibilidadService;
    private final TurnoService turnoService;
    private final PlantillaAgendaService plantillaAgendaService;
    private final NotificacionProgramadaService notificacionProgramadaService;

    @Autowired
    public DoctorController(DoctorService doctorService,
                            DisponibilidadService disponibilidadService,
                            TurnoService turnoService,
                            PlantillaAgendaService plantillaAgendaService,
                            NotificacionProgramadaService notificacionProgramadaService) {
        this.doctorService = doctorService;
        this.disponibilidadService = disponibilidadService;
        this.turnoService = turnoService;
        this.plantillaAgendaService = plantillaAgendaService;
        this.notificacionProgramadaService = notificacionProgramadaService;
    }

    @GetMapping
    public ResponseEntity<List<Doctor>> listarDoctores(
            @RequestParam(required = false) Long especialidadId) {
        if (especialidadId != null) {
            return ResponseEntity.ok(doctorService.listarPorEspecialidad(especialidadId));
        }
        return ResponseEntity.ok(doctorService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Doctor> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(doctorService.obtenerPorId(id));
    }

    // Configurar horario de atención manual
    @PostMapping("/{id}/horarios")
    public ResponseEntity<HorarioAtencion> agregarHorario(
            @PathVariable Long id,
            @Valid @RequestBody HorarioAtencionDTO dto) {
        HorarioAtencion horario = doctorService.agregarHorarioAtencion(id, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(horario);
    }

    @GetMapping("/{id}/horarios")
    public ResponseEntity<List<HorarioAtencion>> obtenerHorarios(@PathVariable Long id) {
        return ResponseEntity.ok(doctorService.obtenerHorariosDoctor(id));
    }

    // --- PLANTILLAS DE AGENDA ---

    @PostMapping("/{id}/plantillas")
    public ResponseEntity<PlantillaAgenda> crearPlantilla(
            @PathVariable Long id,
            @Valid @RequestBody CrearPlantillaDTO dto) {
        PlantillaAgenda plantilla = plantillaAgendaService.crearPlantilla(id, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(plantilla);
    }

    @GetMapping("/{id}/plantillas")
    public ResponseEntity<List<PlantillaAgenda>> listarPlantillas(@PathVariable Long id) {
        return ResponseEntity.ok(plantillaAgendaService.listarPlantillasDoctor(id));
    }

    @PostMapping("/{id}/aplicar-plantilla")
    public ResponseEntity<List<HorarioAtencion>> aplicarPlantilla(
            @PathVariable Long id,
            @Valid @RequestBody AplicarPlantillaDTO dto) {
        List<HorarioAtencion> nuevosHorarios = plantillaAgendaService.aplicarPlantilla(id, dto);
        return ResponseEntity.ok(nuevosHorarios);
    }

    // --- DISPONIBILIDAD Y AGENDA ---

    @GetMapping("/{id}/disponibilidad")
    public ResponseEntity<List<SlotDisponibilidadDTO>> obtenerDisponibilidad(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ResponseEntity.ok(disponibilidadService.obtenerHorariosDisponibles(id, fecha));
    }

    @GetMapping("/{id}/agenda")
    public ResponseEntity<List<TurnoResponseDTO>> obtenerAgenda(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ResponseEntity.ok(turnoService.obtenerAgendaDoctor(id, fecha));
    }

    // --- NOTIFICACIONES ---

    // Disparar manualmente el envío del resumen diario de mañana a los doctores (para pruebas rápidas)
    @PostMapping("/ejecutar-resumen-diario")
    public ResponseEntity<String> ejecutarResumenDiario() {
        notificacionProgramadaService.enviarResumenDiarioADoctores();
        return ResponseEntity.ok("Proceso de envío de resúmenes diarios ejecutado correctamente.");
    }
}
