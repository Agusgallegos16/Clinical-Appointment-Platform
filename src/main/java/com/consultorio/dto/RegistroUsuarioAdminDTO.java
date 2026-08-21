package com.consultorio.dto;

import com.consultorio.domain.Rol;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistroUsuarioAdminDTO {

    @NotNull(message = "El rol es obligatorio")
    private Rol rol; // DOCTOR, SECRETARIA, PACIENTE

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Formato de email inválido")
    private String email;

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    private String apellido;

    private Long dni;

    private String telefono;

    private LocalDate fechaNacimiento; // Para paciente

    private List<Long> especialidadIds; // Para doctor

    private String fotoUrl; // Para doctor
}
