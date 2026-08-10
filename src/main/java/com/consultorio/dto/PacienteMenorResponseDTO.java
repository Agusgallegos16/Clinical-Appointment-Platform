package com.consultorio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PacienteMenorResponseDTO {
    private Long id;
    private String nombre;
    private String apellido;
    private Long dni;
    private LocalDate fechaNacimiento;
    private Integer edad;
    private String telefono;
    private Long tutorId;
    private String tutorNombre;
}
