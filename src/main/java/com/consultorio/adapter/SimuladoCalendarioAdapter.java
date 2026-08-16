package com.consultorio.adapter;

import com.consultorio.domain.Turno;
import com.consultorio.domain.Usuario;
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
    public String agendarEventoParaUsuario(Turno turno, Usuario usuario) {
        String eventId = "gcal-simulated-" + UUID.randomUUID().toString().substring(0, 8);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        log.info("\n📅 === [SIMULADOR GOOGLE CALENDAR ADAPTER] ===" +
                        "\n📌 Cita Presencial Agendada para Usuario: {}" +
                        "\n - Event ID: {}" +
                        "\n - Título: Turno Médico ({})" +
                        "\n - Fecha y Hora: {} hs" +
                        "\n - Doctor: Dr/a. {} {} ({})" +
                        "\n - Paciente: {} {} ({})" +
                        "\n - Motivo: {}" +
                        "\n==========================================",
                usuario != null ? usuario.getEmail() : "Desconocido",
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
    public void cancelarEventoParaUsuario(String googleEventId, Usuario usuario) {
        log.info("\n📅 === [SIMULADOR GOOGLE CALENDAR ADAPTER] ===" +
                        "\n❌ Cita Eliminada / Cancelada para Usuario: {}" +
                        "\n - Event ID: {}" +
                        "\n==========================================",
                usuario != null ? usuario.getEmail() : "Desconocido",
                googleEventId != null ? googleEventId : "N/A"
        );
    }
}
