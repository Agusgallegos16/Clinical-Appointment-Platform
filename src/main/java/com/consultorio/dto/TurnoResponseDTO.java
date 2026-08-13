package com.consultorio.dto;

import com.consultorio.domain.EstadoTurno;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TurnoResponseDTO {
    private UUID id;
    private UUID pacienteId;
    private String pacienteNombre;
    private UUID doctorId;
    private String doctorNombre;
    private String especialidadNombre;
    private LocalDateTime fechaHora;
    private EstadoTurno estado;
    private String motivoConsulta;
    private String motivoCancelacion;
    private String googleEventId;
}
