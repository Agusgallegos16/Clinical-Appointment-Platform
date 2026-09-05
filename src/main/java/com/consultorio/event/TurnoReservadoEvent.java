package com.consultorio.event;

import java.util.UUID;

public record TurnoReservadoEvent(UUID turnoId, String emailDestino) {
}
