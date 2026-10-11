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

    public UsuarioAdminDTO(Long id, String email, Rol rol, boolean activo, Boolean bloqueado, boolean emailVerificado, String nombre, String apellido) {
        this.id = id;
        this.email = email;
        this.rol = rol;
        this.activo = activo;
        this.bloqueado = Boolean.TRUE.equals(bloqueado);
        this.emailVerificado = emailVerificado;
        this.nombre = nombre;
        this.apellido = apellido;
    }
}
