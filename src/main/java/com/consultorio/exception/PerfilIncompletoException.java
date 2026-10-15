package com.consultorio.exception;

import lombok.Getter;

import java.util.Collections;
import java.util.List;

@Getter
public class PerfilIncompletoException extends RuntimeException {

    private final List<String> camposFaltantes;

    public PerfilIncompletoException(String mensaje, List<String> camposFaltantes) {
        super(mensaje);
        this.camposFaltantes = camposFaltantes != null ? camposFaltantes : Collections.emptyList();
    }
}
