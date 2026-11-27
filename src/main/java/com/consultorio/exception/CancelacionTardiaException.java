package com.consultorio.exception;

/**
 * Excepción de regla de negocio lanzada cuando un paciente intenta cancelar
 * un turno con menos de 24 horas de anticipación a la fecha y hora programada.
 */
public class CancelacionTardiaException extends RuntimeException {

    private final String codigo;

    public CancelacionTardiaException(String mensaje) {
        super(mensaje);
        this.codigo = "TURNO_CANCELACION_ANTICIPACION_INSUFICIENTE";
    }

    public CancelacionTardiaException(String mensaje, String codigo) {
        super(mensaje);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
