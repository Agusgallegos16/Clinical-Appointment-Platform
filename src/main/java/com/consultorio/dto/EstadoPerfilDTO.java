package com.consultorio.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EstadoPerfilDTO {
    private UUID pacienteId;
    private String nombre;
    private String apellido;
    private String email;
    private Long dni;
    private String telefono;
    private LocalDate fechaNacimiento;
    private boolean perfilCompleto;
    private List<String> camposFaltantes;
}
