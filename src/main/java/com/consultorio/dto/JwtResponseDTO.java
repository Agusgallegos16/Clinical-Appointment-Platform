package com.consultorio.dto;

import com.consultorio.domain.Rol;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

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
    private UUID entidadId; // ID (UUID) del Paciente o Doctor asociado
}
