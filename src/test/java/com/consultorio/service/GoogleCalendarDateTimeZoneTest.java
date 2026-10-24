package com.consultorio.service;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.EventDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.*;

class GoogleCalendarDateTimeZoneTest {

    private TimeZone originalTimeZone;

    @BeforeEach
    void setUp() {
        originalTimeZone = TimeZone.getDefault();
    }

    @AfterEach
    void tearDown() {
        TimeZone.setDefault(originalTimeZone);
    }

    @Test
    @DisplayName("Debe construir EventDateTime con zona horaria America/Argentina/Buenos_Aires y offset -03:00 incluso en servidores con JVM en UTC")
    void testEventDateTimeZonaHorariaArgentinaEnServidorUtc() {
        // Simular entorno de servidor en UTC (e.g. contenedor Docker / Cloud)
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        LocalDateTime fechaHoraTurno = LocalDateTime.of(2026, 10, 24, 8, 0, 0);

        ZoneId zoneId = ZoneId.of("America/Argentina/Buenos_Aires");
        ZonedDateTime inicioZoned = fechaHoraTurno.atZone(zoneId);
        ZonedDateTime finZoned = inicioZoned.plusMinutes(30);

        DateTime startDateTime = new DateTime(inicioZoned.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        DateTime endDateTime = new DateTime(finZoned.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

        EventDateTime startEvent = new EventDateTime().setDateTime(startDateTime).setTimeZone(zoneId.getId());
        EventDateTime endEvent = new EventDateTime().setDateTime(endDateTime).setTimeZone(zoneId.getId());

        // Verificar zona horaria explícita
        assertEquals("America/Argentina/Buenos_Aires", startEvent.getTimeZone());
        assertEquals("America/Argentina/Buenos_Aires", endEvent.getTimeZone());

        // Verificar que el offset es -03:00 (-180 minutos) y NO UTC (0)
        assertEquals(-180, startDateTime.getTimeZoneShift());
        assertEquals(-180, endDateTime.getTimeZoneShift());

        // Verificar que el formato RFC3339 preserva las 08:00 hs con el offset -03:00
        assertTrue(startDateTime.toStringRfc3339().startsWith("2026-10-24T08:00:00"));
        assertTrue(startDateTime.toStringRfc3339().endsWith("-03:00"));

        assertTrue(endDateTime.toStringRfc3339().startsWith("2026-10-24T08:30:00"));
        assertTrue(endDateTime.toStringRfc3339().endsWith("-03:00"));
    }
}
