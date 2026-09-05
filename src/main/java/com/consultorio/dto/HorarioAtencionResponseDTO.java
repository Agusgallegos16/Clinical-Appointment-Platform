package com.consultorio.dto;

import com.consultorio.domain.DiaSemana;
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
public class HorarioAtencionResponseDTO {
    private Long id;
    private UUID doctorId;
    private Long especialidadId;
    private String especialidadNombre;
    private DiaSemana diaSemana;
    private LocalDate fecha;
    private LocalDate fechaDesde;
    private LocalDate fechaHasta;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private int duracionTurnoMinutos;
}
