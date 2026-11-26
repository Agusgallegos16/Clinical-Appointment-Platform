package com.consultorio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProximoTurnoDisponibleDTO {
    private Long slotId;
    private LocalDate fecha;
    private LocalTime hora;
    private Long especialidadId;
    private String especialidadNombre;
}
