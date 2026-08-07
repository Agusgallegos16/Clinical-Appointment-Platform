package com.consultorio.adapter;

import com.consultorio.domain.Turno;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "app.google.calendar.enabled", havingValue = "false", matchIfMissing = true)
public class SimuladoCalendarioAdapter implements CalendarioAdapter {

    private static final Logger log = LoggerFactory.getLogger(SimuladoCalendarioAdapter.class);

    @Override
    public String agendarEvento(Turno turno) {
        String eventId = "gcal-simulated-" + UUID.randomUUID().toString().substring(0, 8);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        log.info("\n📅 === [SIMULADOR GOOGLE CALENDAR ADAPTER] ===" +
                        "\n📌 Cita Presencial Agendada con Éxito" +
                        "\n - Event ID: {}" +
                        "\n - Título: Turno Médico ({})" +
                        "\n - Fecha y Hora: {} hs" +
                        "\n - Doctor: Dr/a. {} {} ({})" +
                        "\n - Paciente: {} {} ({})" +
                        "\n - Motivo: {}" +
                        "\n==========================================",
                eventId,
                turno.getEspecialidad().getNombre(),
                turno.getFechaHora().format(formatter),
                turno.getDoctor().getNombre(), turno.getDoctor().getApellido(), turno.getDoctor().getUsuario().getEmail(),
                turno.getPaciente().getNombre(), turno.getPaciente().getApellido(), turno.getPaciente().getUsuario().getEmail(),
                turno.getMotivoConsulta() != null ? turno.getMotivoConsulta() : "Consulta General"
        );

        return eventId;
    }

    @Override
    public void cancelarEvento(String googleEventId) {
        log.info("\n📅 === [SIMULADOR GOOGLE CALENDAR ADAPTER] ===" +
                        "\n❌ Cita Eliminada / Cancelada en Google Calendar" +
                        "\n - Event ID: {}" +
                        "\n==========================================",
                googleEventId != null ? googleEventId : "N/A"
        );
    }
}
