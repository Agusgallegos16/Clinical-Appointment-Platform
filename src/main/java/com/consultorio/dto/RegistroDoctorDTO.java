package com.consultorio.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.List;

@Data
public class RegistroDoctorDTO {
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Formato de email inválido")
    private String email;

    private String password;

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    private String apellido;

    private String fotoUrl;

    private List<Long> especialidadIds;
}
