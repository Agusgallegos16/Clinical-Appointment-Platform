package com.consultorio.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OAuth2CodeExchangeDTO {
    @NotBlank(message = "El código de autorización OAuth2 es obligatorio")
    private String code;
}
