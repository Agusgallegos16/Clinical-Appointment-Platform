package com.consultorio.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Evento de dominio publicado tras confirmarse el commit de la cancelación de un turno.
 * Implementa el patrón Event-Carried State Transfer para aislar el hilo asíncrono
 * de la sesión de Hibernate y evitar excepciones de tipo LazyInitializationException.
 */
public record TurnoCanceladoEvent(
        UUID turnoId,
        String motivo,
        String canceladoPorRol,       // "PACIENTE", "DOCTOR", "SECRETARIA", "ADMIN", "TUTOR"
        String canceladoPorNombre,
        String pacienteEmail,
        String pacienteNombre,
        String doctorEmail,
        String doctorNombre,
        String especialidadNombre,
        LocalDateTime fechaHora,
        String googleEventIdPaciente,
        String googleEventIdDoctor
) {

    /**
     * Constructor de compatibilidad hacia atrás para tests o llamadas legadas con 3 argumentos.
     */
    public TurnoCanceladoEvent(UUID turnoId, String motivo, boolean canceladoPorDoctor) {
        this(
                turnoId,
                motivo,
                canceladoPorDoctor ? "DOCTOR" : "PACIENTE",
                canceladoPorDoctor ? "Cuerpo Médico" : "Paciente",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    /**
     * Determina si la cancelación provino del cuerpo médico o administrativo (Doctor, Secretaría o Admin).
     */
    public boolean canceladoPorDoctor() {
        return "DOCTOR".equalsIgnoreCase(canceladoPorRol)
                || "SECRETARIA".equalsIgnoreCase(canceladoPorRol)
                || "ADMIN".equalsIgnoreCase(canceladoPorRol);
    }
}
