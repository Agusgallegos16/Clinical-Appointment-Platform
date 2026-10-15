package com.consultorio.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompletarPerfilPacienteDTO {

    @NotNull(message = "El DNI es obligatorio")
    @Min(value = 1000000, message = "El DNI debe ser válido (mínimo 7 u 8 dígitos)")
    private Long dni;

    @NotBlank(message = "El teléfono de contacto es obligatorio")
    private String telefono;

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @Past(message = "La fecha de nacimiento debe corresponder al pasado")
    private LocalDate fechaNacimiento;
}
