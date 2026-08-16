package com.consultorio.controller;

import com.consultorio.domain.*;
import com.consultorio.dto.AplicarPlantillaDTO;
import com.consultorio.dto.CrearPlantillaDTO;
import com.consultorio.dto.HorarioAtencionDTO;
import com.consultorio.dto.RegistroDoctorDTO;
import com.consultorio.dto.SlotDisponibilidadDTO;
import com.consultorio.dto.TurnoResponseDTO;
import com.consultorio.service.DisponibilidadService;
import com.consultorio.service.DoctorService;
import com.consultorio.service.NotificacionProgramadaService;
import com.consultorio.service.PlantillaAgendaService;
import com.consultorio.service.TurnoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import java.util.UUID;

@RestController
@RequestMapping("/api/doctores")
@Tag(name = "Gestión de Médicos", description = "Endpoints para registro, consulta, horarios y disponibilidad de doctores.")
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

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Registrar un nuevo médico (Exclusivo ADMIN)")
    public ResponseEntity<Doctor> registrarDoctor(@Valid @RequestBody RegistroDoctorDTO dto) {
        Doctor nuevoDoctor = doctorService.registrarDoctor(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevoDoctor);
    }

    @GetMapping
    @Operation(summary = "Listar todos los médicos o filtrar por especialidad (Público)")
    public ResponseEntity<List<Doctor>> listarDoctores(
            @Parameter(description = "ID opcional de especialidad médica para filtrar") @RequestParam(required = false) Long especialidadId,
            @Parameter(description = "Si es true, solo retorna médicos visibles para reserva por pacientes") @RequestParam(required = false, defaultValue = "false") boolean soloVisibles) {
        if (especialidadId != null) {
            return ResponseEntity.ok(doctorService.listarPorEspecialidad(especialidadId, soloVisibles));
        }
        return ResponseEntity.ok(doctorService.listarTodos(soloVisibles));
    }

    @PatchMapping("/{id}/disponibilidad-turnos")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Activar o desactivar la visibilidad pública de turnos del médico (DOCTOR / ADMIN)")
    public ResponseEntity<Doctor> cambiarDisponibilidadTurnos(
            @Parameter(description = "UUID del profesional médico") @PathVariable UUID id,
            @Parameter(description = "true para perfil visible en reserva web, false para ocultar") @RequestParam boolean disponible) {
        return ResponseEntity.ok(doctorService.cambiarDisponibilidadTurnos(id, disponible));
    }

    @PatchMapping("/{id}/advertencia-bloqueante")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Configurar el mensaje de advertencia bloqueante del médico (DOCTOR / ADMIN)",
               description = "Si está activa, despliega un cartel emergente al paciente y BLOQUEA la reserva web.")
    public ResponseEntity<Doctor> configurarAdvertenciaBloqueante(
            @Parameter(description = "UUID del profesional médico") @PathVariable UUID id,
            @Parameter(description = "true para activar el cartel bloqueante") @RequestParam boolean activa,
            @Parameter(description = "Texto de advertencia e instrucciones para el paciente") @RequestParam(required = false) String mensaje) {
        return ResponseEntity.ok(doctorService.configurarAdvertenciaBloqueante(id, activa, mensaje));
    }

    @PatchMapping("/{id}/advertencia-informativa")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Configurar el mensaje de advertencia informativo del médico (DOCTOR / ADMIN)",
               description = "Si está activa, despliega un aviso pero PERMITE al paciente presionar Continuar y reservar.")
    public ResponseEntity<Doctor> configurarAdvertenciaInformativa(
            @Parameter(description = "UUID del profesional médico") @PathVariable UUID id,
            @Parameter(description = "true para activar el cartel informativo") @RequestParam boolean activa,
            @Parameter(description = "Texto de indicaciones para el paciente") @RequestParam(required = false) String mensaje) {
        return ResponseEntity.ok(doctorService.configurarAdvertenciaInformativa(id, activa, mensaje));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener el detalle de un médico por su ID (Público)")
    public ResponseEntity<Doctor> obtenerDoctorPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(doctorService.obtenerPorId(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar datos de un médico existente (Exclusivo ADMIN)")
    public ResponseEntity<Doctor> actualizarDoctor(
            @PathVariable UUID id,
            @RequestBody RegistroDoctorDTO dto) {
        return ResponseEntity.ok(doctorService.actualizarDoctor(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Eliminar un médico por su ID (Exclusivo ADMIN)")
    public ResponseEntity<Void> eliminarDoctor(@PathVariable UUID id) {
        doctorService.eliminarDoctor(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/horarios")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Agregar un nuevo horario de atención semanal o por fecha puntual (DOCTOR / ADMIN)")
    public ResponseEntity<HorarioAtencion> agregarHorario(
            @PathVariable UUID id,
            @Valid @RequestBody HorarioAtencionDTO dto) {
        HorarioAtencion horario = doctorService.agregarHorarioAtencion(id, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(horario);
    }

    @PutMapping("/horarios/{horarioId}")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Actualizar parámetros de una franja horaria existente (DOCTOR / ADMIN)")
    public ResponseEntity<HorarioAtencion> actualizarHorario(
            @PathVariable Long horarioId,
            @Valid @RequestBody HorarioAtencionDTO dto) {
        return ResponseEntity.ok(doctorService.actualizarHorarioAtencion(horarioId, dto));
    }

    @GetMapping("/{id}/horarios")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Obtener todos los horarios de atención configurados por un doctor (DOCTOR / ADMIN)")
    public ResponseEntity<List<HorarioAtencion>> obtenerHorarios(@PathVariable UUID id) {
        return ResponseEntity.ok(doctorService.obtenerHorariosDoctor(id));
    }

    @DeleteMapping("/horarios/{horarioId}")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Eliminar un horario de atención (DOCTOR / ADMIN)")
    public ResponseEntity<Void> eliminarHorario(@PathVariable Long horarioId) {
        doctorService.eliminarHorarioAtencion(horarioId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/horarios/semana")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Limpiar/Borrar todos los turnos configurados para una semana específica (DOCTOR / ADMIN)")
    public ResponseEntity<Void> limpiarHorariosSemana(
            @PathVariable UUID id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        doctorService.limpiarHorariosSemana(id, desde, hasta);
        return ResponseEntity.noContent().build();
    }

    // ENDPOINTS DE SLOTS INDIVIDUALES
    @GetMapping("/{id}/slots")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Obtener la lista de slots instanciados concretos del doctor (DOCTOR / ADMIN)")
    public ResponseEntity<List<SlotHorario>> obtenerSlots(
            @PathVariable UUID id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(doctorService.obtenerSlotsDoctor(id, desde, hasta));
    }

    @DeleteMapping("/slots/{slotId}")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Eliminar/Deshabilitar un slot individual concreto (DOCTOR / ADMIN)")
    public ResponseEntity<Void> eliminarSlot(@PathVariable Long slotId) {
        doctorService.eliminarSlotIndividual(slotId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/plantillas")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Crear una nueva plantilla de agenda personalizada (DOCTOR / ADMIN)")
    public ResponseEntity<PlantillaAgenda> crearPlantilla(
            @PathVariable UUID id,
            @Valid @RequestBody CrearPlantillaDTO dto) {
        PlantillaAgenda plantilla = plantillaAgendaService.crearPlantilla(id, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(plantilla);
    }

    @PutMapping("/plantillas/{plantillaId}")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Actualizar/Editar una plantilla de agenda existente (DOCTOR / ADMIN)")
    public ResponseEntity<PlantillaAgenda> actualizarPlantilla(
            @PathVariable Long plantillaId,
            @Valid @RequestBody CrearPlantillaDTO dto) {
        return ResponseEntity.ok(plantillaAgendaService.actualizarPlantilla(plantillaId, dto));
    }

    @GetMapping("/{id}/plantillas")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Listar todas las plantillas de agenda del doctor (DOCTOR / ADMIN)")
    public ResponseEntity<List<PlantillaAgenda>> listarPlantillas(@PathVariable UUID id) {
        return ResponseEntity.ok(plantillaAgendaService.listarPlantillasDoctor(id));
    }

    @DeleteMapping("/plantillas/{plantillaId}")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Eliminar una plantilla de agenda (DOCTOR / ADMIN)")
    public ResponseEntity<Void> eliminarPlantilla(@PathVariable Long plantillaId) {
        plantillaAgendaService.eliminarPlantilla(plantillaId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/aplicar-plantilla")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Aplicar una plantilla de agenda a una fecha puntual (DOCTOR / ADMIN)")
    public ResponseEntity<List<HorarioAtencion>> aplicarPlantilla(
            @PathVariable UUID id,
            @Valid @RequestBody AplicarPlantillaDTO dto) {
        List<HorarioAtencion> nuevosHorarios = plantillaAgendaService.aplicarPlantilla(id, dto);
        return ResponseEntity.ok(nuevosHorarios);
    }

    @GetMapping("/{id}/disponibilidad")
    @Operation(summary = "Consultar slots de horarios libres disponibles para reservar en una fecha y especialidad (Público)")
    public ResponseEntity<List<SlotDisponibilidadDTO>> obtenerDisponibilidad(
            @PathVariable UUID id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) Long especialidadId) {
        return ResponseEntity.ok(disponibilidadService.obtenerHorariosDisponibles(id, fecha, especialidadId));
    }

    @GetMapping("/{id}/agenda")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Consultar la agenda privada de turnos confirmados del doctor para un día (DOCTOR / ADMIN)")
    public ResponseEntity<List<TurnoResponseDTO>> obtenerAgenda(
            @PathVariable UUID id,
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
