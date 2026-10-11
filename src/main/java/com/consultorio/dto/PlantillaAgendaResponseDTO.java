package com.consultorio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlantillaAgendaResponseDTO {
    private Long id;
    private UUID doctorId;
    private String nombre;
    private String descripcion;
    private List<DetallePlantillaResponseDTO> detalles;
}
