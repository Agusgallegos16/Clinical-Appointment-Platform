package com.consultorio.dto;

import com.consultorio.domain.Rol;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JwtResponseDTO {
    private String token;
    @Builder.Default
    private String tipo = "Bearer";
    private Long id;
    private String email;
    private Rol rol;
    private Long entidadId; // ID del Paciente o Doctor asociado
}
