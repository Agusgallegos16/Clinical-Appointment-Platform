package com.consultorio.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TurnoReservaSecretariaDTO {

    @NotNull(message = "El ID del doctor es obligatorio")
    private UUID doctorId;

    @NotNull(message = "El ID de la especialidad es obligatorio")
    private Long especialidadId;

    @NotNull(message = "La fecha y hora son obligatorias")
    private LocalDateTime fechaHora;

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    private String apellido;

    @NotNull(message = "El DNI es obligatorio")
    @Min(value = 1000000, message = "El DNI debe ser válido")
    private Long dni;

    @Email(message = "Formato de email inválido")
    private String email;

    private String telefono;

    private LocalDate fechaNacimiento;

    private Boolean tieneObraSocial;

    private String obraSocial;

    private String motivoConsulta;
}
