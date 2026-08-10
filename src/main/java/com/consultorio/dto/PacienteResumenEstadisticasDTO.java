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
public class PacienteResumenEstadisticasDTO {
    private Long id;
    private String nombre;
    private String apellido;
    private String dni;
    private String telefono;
    private String email;
    private LocalDate fechaNacimiento;
    private Integer edad;

    // Métricas de Turnos
    private int totalTurnos;
    private int totalCompletados;
    private int totalAusentes;
    private int totalCancelados;
    private int totalPendientes;

    // Porcentajes
    private double porcentajeCompletados;
    private double porcentajeAusentes;
    private double porcentajeCancelados;
    private double porcentajePendientes;
}
