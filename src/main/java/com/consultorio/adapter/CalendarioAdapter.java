package com.consultorio.adapter;

import com.consultorio.domain.Turno;

public interface CalendarioAdapter {

    /**
     * Agenda una nueva cita médica presencial en Google Calendar notificando al Doctor y Paciente.
     *
     * @param turno El turno médico agendado
     * @return ID del evento generado en Google Calendar (o ID simulado)
     */
    String agendarEvento(Turno turno);

    /**
     * Cancela o elimina una cita médica de Google Calendar mediante su ID de evento.
     *
     * @param googleEventId ID del evento en Google Calendar
     */
    void cancelarEvento(String googleEventId);
}
