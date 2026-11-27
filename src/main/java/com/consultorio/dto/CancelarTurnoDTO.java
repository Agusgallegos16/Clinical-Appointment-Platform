package com.consultorio.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "DTO para solicitar la cancelación de un turno médico.")
public class CancelarTurnoDTO {

    @Size(max = 500, message = "El motivo de cancelación no puede exceder los 500 caracteres.")
    @Schema(description = "Justificación o motivo de la cancelación. Obligatorio para personal médico/secretaría, opcional para pacientes.", example = "Urgencia médica imprevista.")
    private String motivo;
}
