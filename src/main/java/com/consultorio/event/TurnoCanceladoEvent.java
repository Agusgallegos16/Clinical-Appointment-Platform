package com.consultorio.event;

import java.util.UUID;

public record TurnoCanceladoEvent(UUID turnoId, String motivo, boolean canceladoPorDoctor) {
}
