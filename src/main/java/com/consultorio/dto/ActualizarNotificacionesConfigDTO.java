package com.consultorio.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "DTO para actualizar la configuración de switches de resúmenes automáticos.")
public class ActualizarNotificacionesConfigDTO {

    @Schema(description = "Habilitar o deshabilitar el resumen diario automático a médicos", example = "true")
    private Boolean resumenDiarioMedicosHabilitado;

    @Schema(description = "Habilitar o deshabilitar el reporte semanal automático a médicos", example = "false")
    private Boolean resumenSemanalMedicosHabilitado;
}
