package com.consultorio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlotHorarioResponseDTO {
    private Long id;
    private UUID doctorId;
    private Long especialidadId;
    private String especialidadNombre;
    private EspecialidadResponseDTO especialidad;
    private LocalDate fecha;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private int duracionMinutos;
    private boolean esPuntual;
}
