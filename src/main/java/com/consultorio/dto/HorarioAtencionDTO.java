package com.consultorio.dto;

import com.consultorio.domain.DiaSemana;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class HorarioAtencionDTO {
    private DiaSemana diaSemana; // Opcional: para horario semanal recurrente (ej. LUNES)
    private LocalDate fecha;     // Opcional: para fecha concreta puntual (ej. 2026-08-10)

    @NotNull(message = "La hora de inicio es obligatoria")
    private LocalTime horaInicio;

    @NotNull(message = "La hora de fin es obligatoria")
    private LocalTime horaFin;

    private int duracionTurnoMinutos = 30;
}
