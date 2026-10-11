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
public class DoctorResponseDTO {
    private UUID id;
    private String nombre;
    private String apellido;
    private String email;
    private String fotoUrl;
    private UsuarioBasicDTO usuario;
    private List<EspecialidadResponseDTO> especialidades;
    private boolean disponibleParaTurnos;
    private boolean tieneAdvertenciaBloqueante;
    private String mensajeAdvertenciaBloqueante;
    private boolean tieneAdvertenciaInformativa;
    private String mensajeAdvertenciaInformativa;
}
