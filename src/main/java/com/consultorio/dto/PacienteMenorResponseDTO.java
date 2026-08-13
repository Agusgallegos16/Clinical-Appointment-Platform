package com.consultorio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PacienteMenorResponseDTO {
    private UUID id;
    private String nombre;
    private String apellido;
    private Long dni;
    private LocalDate fechaNacimiento;
    private Integer edad;
    private String telefono;
    private UUID tutorId;
    private String tutorNombre;
}
