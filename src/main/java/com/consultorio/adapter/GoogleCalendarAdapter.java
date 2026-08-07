package com.consultorio.adapter;

import com.consultorio.domain.Turno;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.EventDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

@Component
@ConditionalOnProperty(name = "app.google.calendar.enabled", havingValue = "true")
public class GoogleCalendarAdapter implements CalendarioAdapter {

    private static final Logger log = LoggerFactory.getLogger(GoogleCalendarAdapter.class);

    private final Calendar googleCalendarClient;

    @Autowired(required = false)
    public GoogleCalendarAdapter(Calendar googleCalendarClient) {
        this.googleCalendarClient = googleCalendarClient;
    }

    @Override
    public String agendarEvento(Turno turno) {
        if (googleCalendarClient == null) {
            log.warn("El cliente de Google Calendar no está inicializado. Se omite el agendamiento remoto.");
            return null;
        }

        try {
            Event event = new Event();

            // 1. Título y Descripción del Evento Presencial
            String titulo = String.format("Turno Médico (%s) - Dr/a. %s",
                    turno.getEspecialidad().getNombre(),
                    turno.getDoctor().getApellido());

            String descripcion = String.format(
                    "Consulta Médica Presencial Confirmada.\n\n" +
                    "📌 Información de la Cita:\n" +
                    " - Profesional: Dr/a. %s %s (%s)\n" +
                    " - Paciente: %s %s (%s)\n" +
                    " - Especialidad: %s\n" +
                    " - Motivo: %s\n",
                    turno.getDoctor().getNombre(), turno.getDoctor().getApellido(), turno.getDoctor().getUsuario().getEmail(),
                    turno.getPaciente().getNombre(), turno.getPaciente().getApellido(), turno.getPaciente().getUsuario().getEmail(),
                    turno.getEspecialidad().getNombre(),
                    turno.getMotivoConsulta() != null ? turno.getMotivoConsulta() : "Consulta General"
            );

            event.setSummary(titulo);
            event.setDescription(descripcion);

            // 2. Tiempos de Inicio y Fin (Duración por defecto 30 min)
            ZoneId zoneId = ZoneId.systemDefault();
            ZonedDateTime inicioZoned = turno.getFechaHora().atZone(zoneId);
            ZonedDateTime finZoned = inicioZoned.plusMinutes(30);

            DateTime startDateTime = new DateTime(inicioZoned.toInstant().toEpochMilli());
            DateTime endDateTime = new DateTime(finZoned.toInstant().toEpochMilli());

            event.setStart(new EventDateTime().setDateTime(startDateTime).setTimeZone(zoneId.getId()));
            event.setEnd(new EventDateTime().setDateTime(endDateTime).setTimeZone(zoneId.getId()));

            // 3. Agregar como Asistentes al Doctor y Paciente (Notificación por Email)
            EventAttendee doctorAttendee = new EventAttendee().setEmail(turno.getDoctor().getUsuario().getEmail());
            EventAttendee pacienteAttendee = new EventAttendee().setEmail(turno.getPaciente().getUsuario().getEmail());

            event.setAttendees(List.of(doctorAttendee, pacienteAttendee));

            // 4. Insertar evento en Google Calendar y notificar a los asistentes
            Event createdEvent = googleCalendarClient.events()
                    .insert("primary", event)
                    .setSendUpdates("all")
                    .execute();

            log.info("📅 Evento presencial agendado exitosamente en Google Calendar con ID: {}", createdEvent.getId());
            return createdEvent.getId();

        } catch (Exception e) {
            log.error("Error al agendar evento presencial en Google Calendar: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public void cancelarEvento(String googleEventId) {
        if (googleCalendarClient == null || googleEventId == null) {
            return;
        }

        try {
            googleCalendarClient.events()
                    .delete("primary", googleEventId)
                    .setSendUpdates("all")
                    .execute();

            log.info("📅 Evento con ID {} cancelado exitosamente en Google Calendar.", googleEventId);
        } catch (Exception e) {
            log.error("Error al cancelar evento {} en Google Calendar: {}", googleEventId, e.getMessage());
        }
    }
}
