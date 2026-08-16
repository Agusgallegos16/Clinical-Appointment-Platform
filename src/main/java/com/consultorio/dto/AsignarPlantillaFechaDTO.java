package com.consultorio.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

@Data
public class AsignarPlantillaFechaDTO {
    @NotNull(message = "La fecha es obligatoria")
    private LocalDate fecha;

    @NotNull(message = "El ID de la plantilla es obligatorio")
    private Long plantillaId;
}
