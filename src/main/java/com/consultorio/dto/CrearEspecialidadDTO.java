package com.consultorio.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrearEspecialidadDTO {
    @NotBlank(message = "El nombre de la especialidad es obligatorio")
    private String nombre;
    private String descripcion;
}
