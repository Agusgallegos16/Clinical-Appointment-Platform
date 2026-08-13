package com.consultorio.dto;

import com.consultorio.domain.DiaSemana;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class AplicarPlantillaDTO {
    @NotNull(message = "El ID de la plantilla es obligatorio")
    private Long plantillaId;

    private DiaSemana diaSemana; // Opcional: para aplicar a todos los días semanales iguales (ej. LUNES)

    private LocalDate fecha; // Opcional: para aplicar a una fecha puntual específica (ej. 2026-08-20)

    private LocalDate fechaDesde; // Opcional: vigencia inicio para aplicación recurrente

    private LocalDate fechaHasta; // Opcional: vigencia fin para aplicación recurrente
}
