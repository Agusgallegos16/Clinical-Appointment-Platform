package com.consultorio.dto;

import com.consultorio.domain.Rol;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioBasicDTO {
    private Long id;
    private String email;
    private Rol rol;
    private boolean activo;
    private boolean emailVerificado;
}
