package com.consultorio.dto;

import com.consultorio.domain.DiaSemana;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class HorarioAtencionDTO {
    private Long especialidadId;
    private DiaSemana diaSemana;
    private LocalDate fecha;
    private LocalDate fechaDesde;
    private LocalDate fechaHasta;

    @NotNull(message = "La hora de inicio es obligatoria")
    private LocalTime horaInicio;

    @NotNull(message = "La hora de fin es obligatoria")
    private LocalTime horaFin;

    private int duracionTurnoMinutos = 30;
}
