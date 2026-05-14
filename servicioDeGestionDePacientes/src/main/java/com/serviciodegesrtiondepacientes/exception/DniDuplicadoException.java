package com.serviciodegesrtiondepacientes.exception;

public class DniDuplicadoException extends RuntimeException {

    public DniDuplicadoException(Long dni) {
        super("Ya existe un paciente registrado con el DNI: " + dni);
    }
}
