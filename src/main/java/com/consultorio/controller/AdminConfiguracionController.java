package com.consultorio.controller;

import com.consultorio.dto.ActualizarNotificacionesConfigDTO;
import com.consultorio.dto.NotificacionesConfigDTO;
import com.consultorio.service.ConfiguracionSistemaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/configuracion")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Configuración del Sistema (Admin)", description = "Endpoints para la gestión global de configuraciones y notificaciones.")
public class AdminConfiguracionController {

    private final ConfiguracionSistemaService configuracionSistemaService;

    @Autowired
    public AdminConfiguracionController(ConfiguracionSistemaService configuracionSistemaService) {
        this.configuracionSistemaService = configuracionSistemaService;
    }

    @GetMapping("/notificaciones")
    @Operation(summary = "Obtener el estado de configuración global de notificaciones y resúmenes automáticos")
    public ResponseEntity<NotificacionesConfigDTO> obtenerConfiguracionNotificaciones() {
        return ResponseEntity.ok(configuracionSistemaService.obtenerNotificacionesConfigDTO());
    }

    @PatchMapping("/notificaciones")
    @Operation(summary = "Actualizar el estado de los interruptores globales de resúmenes diarios y semanales")
    public ResponseEntity<NotificacionesConfigDTO> actualizarConfiguracionNotificaciones(
            @Valid @RequestBody ActualizarNotificacionesConfigDTO dto,
            Authentication authentication) {
        String usuarioEmail = (authentication != null && authentication.getName() != null)
                ? authentication.getName()
                : "ADMIN";
        NotificacionesConfigDTO actualizada = configuracionSistemaService.actualizarNotificacionesConfig(dto, usuarioEmail);
        return ResponseEntity.ok(actualizada);
    }
}
