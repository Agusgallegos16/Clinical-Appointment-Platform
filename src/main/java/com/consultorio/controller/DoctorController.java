package com.consultorio.controller;

import com.consultorio.domain.Doctor;
import com.consultorio.domain.HorarioAtencion;
import com.consultorio.domain.PlantillaAgenda;
import com.consultorio.dto.*;
import com.consultorio.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/doctores")
@Tag(name = "Doctores y Agendas", description = "Endpoints para consulta de profesionales, configuración de horarios, plantillas de agenda y disponibilidad.")
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
    @Operation(summary = "Listar médicos (opcionalmente filtrados por especialidad) (Público)")
    public ResponseEntity<List<Doctor>> listarDoctores(
            @RequestParam(required = false) Long especialidadId) {
        if (especialidadId != null) {
            return ResponseEntity.ok(doctorService.listarPorEspecialidad(especialidadId));
        }
        return ResponseEntity.ok(doctorService.listarTodos());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener perfil de un médico por su ID (Público)")
    public ResponseEntity<Doctor> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(doctorService.obtenerPorId(id));
    }

    @PostMapping("/{id}/horarios")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Agregar un nuevo horario de atención semanal o por fecha puntual (DOCTOR / ADMIN)")
    public ResponseEntity<HorarioAtencion> agregarHorario(
            @PathVariable Long id,
            @Valid @RequestBody HorarioAtencionDTO dto) {
        HorarioAtencion horario = doctorService.agregarHorarioAtencion(id, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(horario);
    }

    @GetMapping("/{id}/horarios")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Obtener todos los horarios de atención configurados por un doctor (DOCTOR / ADMIN)")
    public ResponseEntity<List<HorarioAtencion>> obtenerHorarios(@PathVariable Long id) {
        return ResponseEntity.ok(doctorService.obtenerHorariosDoctor(id));
    }

    @PostMapping("/{id}/plantillas")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Crear una nueva plantilla de agenda personalizada (ej. Día de Prácticas) (DOCTOR / ADMIN)")
    public ResponseEntity<PlantillaAgenda> crearPlantilla(
            @PathVariable Long id,
            @Valid @RequestBody CrearPlantillaDTO dto) {
        PlantillaAgenda plantilla = plantillaAgendaService.crearPlantilla(id, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(plantilla);
    }

    @GetMapping("/{id}/plantillas")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Listar todas las plantillas de agenda del doctor (DOCTOR / ADMIN)")
    public ResponseEntity<List<PlantillaAgenda>> listarPlantillas(@PathVariable Long id) {
        return ResponseEntity.ok(plantillaAgendaService.listarPlantillasDoctor(id));
    }

    @PostMapping("/{id}/aplicar-plantilla")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Aplicar una plantilla de agenda a una fecha puntual (DOCTOR / ADMIN)")
    public ResponseEntity<List<HorarioAtencion>> aplicarPlantilla(
            @PathVariable Long id,
            @Valid @RequestBody AplicarPlantillaDTO dto) {
        List<HorarioAtencion> nuevosHorarios = plantillaAgendaService.aplicarPlantilla(id, dto);
        return ResponseEntity.ok(nuevosHorarios);
    }

    @GetMapping("/{id}/disponibilidad")
    @Operation(summary = "Consultar slots de horarios libres disponibles para reservar en una fecha (Público)")
    public ResponseEntity<List<SlotDisponibilidadDTO>> obtenerDisponibilidad(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ResponseEntity.ok(disponibilidadService.obtenerHorariosDisponibles(id, fecha));
    }

    @GetMapping("/{id}/agenda")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Consultar la agenda privada de turnos confirmados del doctor para un día (DOCTOR / ADMIN)")
    public ResponseEntity<List<TurnoResponseDTO>> obtenerAgenda(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ResponseEntity.ok(turnoService.obtenerAgendaDoctor(id, fecha));
    }

    @PostMapping("/ejecutar-resumen-diario")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Ejecutar manualmente el envío de resúmenes diarios por email (Exclusivo ADMIN)")
    public ResponseEntity<String> ejecutarResumenDiario(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        LocalDate fechaTarget = (fecha != null) ? fecha : LocalDate.now().plusDays(1);
        notificacionProgramadaService.enviarResumenDiarioADoctoresParaFecha(fechaTarget);
        return ResponseEntity.ok("Proceso de envío de resúmenes diarios ejecutado correctamente para la fecha: " + fechaTarget);
    }

    @PostMapping("/ejecutar-resumen-semanal")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Ejecutar manualmente el envío de reportes semanales de actividad a doctores (Exclusivo ADMIN)")
    public ResponseEntity<String> ejecutarResumenSemanal(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        LocalDate hoy = LocalDate.now();
        LocalDate inicioTarget = (desde != null) ? desde : hoy.with(DayOfWeek.MONDAY);
        LocalDate finTarget = (hasta != null) ? hasta : hoy.with(DayOfWeek.SUNDAY);

        notificacionProgramadaService.enviarResumenSemanalADoctoresParaRango(inicioTarget, finTarget);
        return ResponseEntity.ok(String.format("Proceso de envío de reportes semanales ejecutado correctamente para el período: %s al %s", inicioTarget, finTarget));
    }
}
