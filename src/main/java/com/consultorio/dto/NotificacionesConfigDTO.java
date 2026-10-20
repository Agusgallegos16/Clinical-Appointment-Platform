package com.consultorio.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "DTO que representa la configuración global de resúmenes y notificaciones automáticas.")
public class NotificacionesConfigDTO {

    @Schema(description = "Indica si el envío automático del resumen diario a médicos está habilitado", example = "true")
    private Boolean resumenDiarioMedicosHabilitado;

    @Schema(description = "Indica si el envío automático del reporte semanal a médicos está habilitado", example = "true")
    private Boolean resumenSemanalMedicosHabilitado;

    @Schema(description = "Fecha y hora de la última modificación")
    private LocalDateTime fechaModificacion;

    @Schema(description = "Email o identificador del administrador que realizó la última modificación", example = "admin@consultorio.com")
    private String modificadoPor;
}
