package com.consultorio.dto;

import com.consultorio.domain.EstadoTurno;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TurnoResponseDTO {
    private Long id;
    private Long pacienteId;
    private String pacienteNombre;
    private Long doctorId;
    private String doctorNombre;
    private String especialidadNombre;
    private LocalDateTime fechaHora;
    private EstadoTurno estado;
    private String motivoConsulta;
}
