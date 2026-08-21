package com.consultorio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SlotDisponibilidadDTO {
    private Long id;
    private LocalTime hora;
    private boolean disponible;
    private Long especialidadId;
    private String especialidadNombre;
}
