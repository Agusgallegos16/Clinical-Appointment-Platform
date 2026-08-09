package com.consultorio.adapter;

import com.consultorio.domain.Turno;
import com.consultorio.domain.Usuario;

public interface CalendarioAdapter {

    /**
     * Agenda una cita médica presencial en el Google Calendar personal del usuario especificado.
     *
     * @param turno   El turno médico agendado
     * @param usuario El usuario (Paciente o Doctor) en cuyo calendario se creará el evento
     * @return ID del evento generado en Google Calendar (o ID simulado)
     */
    String agendarEventoParaUsuario(Turno turno, Usuario usuario);

    /**
     * Cancela o elimina una cita médica del Google Calendar personal del usuario especificado.
     *
     * @param googleEventId ID del evento en Google Calendar
     * @param usuario       El usuario de cuyo calendario se eliminará el evento
     */
    void cancelarEventoParaUsuario(String googleEventId, Usuario usuario);
}
