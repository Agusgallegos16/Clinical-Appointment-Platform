package com.consultorio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;

@Data
public class CrearPlantillaDTO {
    @NotBlank(message = "El nombre de la plantilla es obligatorio")
    private String nombre;

    private String descripcion;

    @NotEmpty(message = "Debe especificar al menos una franja horaria")
    private List<DetallePlantillaDTO> detalles;
}
