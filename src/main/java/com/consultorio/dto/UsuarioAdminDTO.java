package com.consultorio.dto;

import com.consultorio.domain.Rol;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioAdminDTO {
    private Long id;
    private String email;
    private Rol rol;
    private boolean activo;
    private boolean bloqueado;
    private boolean emailVerificado;
    private String nombre;
    private String apellido;
}
