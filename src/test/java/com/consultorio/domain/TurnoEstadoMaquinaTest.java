package com.consultorio.domain;

import com.consultorio.exception.ReglaNegocioTurnoException;
import com.consultorio.exception.TransicionEstadoTurnoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class TurnoEstadoMaquinaTest {

    @Test
    @DisplayName("Transición PENDIENTE -> CONFIRMADO con fecha futura debe ser exitosa")
    void testConfirmarTurnoPendienteExitoso() {
        Turno turno = Turno.builder()
                .estado(EstadoTurno.PENDIENTE)
                .fechaHora(LocalDateTime.now().plusDays(2))
                .build();

        turno.confirmar();

        assertEquals(EstadoTurno.CONFIRMADO, turno.getEstado());
    }

    @Test
    @DisplayName("Confirmar turno con fecha pasada debe lanzar ReglaNegocioTurnoException")
    void testConfirmarTurnoConFechaPasadaFalla() {
        Turno turno = Turno.builder()
                .estado(EstadoTurno.PENDIENTE)
                .fechaHora(LocalDateTime.now().minusHours(1))
                .build();

        assertThrows(ReglaNegocioTurnoException.class, turno::confirmar);
    }

    @Test
    @DisplayName("Confirmar turno que no está en PENDIENTE debe lanzar TransicionEstadoTurnoException")
    void testConfirmarTurnoNoPendienteFalla() {
        Turno turno = Turno.builder()
                .estado(EstadoTurno.CONFIRMADO)
                .fechaHora(LocalDateTime.now().plusDays(1))
                .build();

        assertThrows(TransicionEstadoTurnoException.class, turno::confirmar);
    }

    @Test
    @DisplayName("Transición CONFIRMADO -> CANCELADO por paciente debe ser exitosa")
    void testCancelarTurnoConfirmadoExitoso() {
        Turno turno = Turno.builder()
                .estado(EstadoTurno.CONFIRMADO)
                .fechaHora(LocalDateTime.now().plusDays(3))
                .build();

        turno.cancelar(null, false);

        assertEquals(EstadoTurno.CANCELADO, turno.getEstado());
        assertNull(turno.getMotivoCancelacion());
    }

    @Test
    @DisplayName("Transición CANCELADO -> CANCELADO (intento de re-cancelación) debe lanzar TransicionEstadoTurnoException")
    void testCancelarTurnoCanceladoFalla() {
        Turno turno = Turno.builder()
                .estado(EstadoTurno.CANCELADO)
                .fechaHora(LocalDateTime.now().plusDays(1))
                .build();

        assertThrows(TransicionEstadoTurnoException.class, () -> turno.cancelar("motivo", false));
    }

    @Test
    @DisplayName("Transición COMPLETADO -> CANCELADO debe lanzar TransicionEstadoTurnoException")
    void testCancelarTurnoCompletadoFalla() {
        Turno turno = Turno.builder()
                .estado(EstadoTurno.COMPLETADO)
                .fechaHora(LocalDateTime.now().minusDays(1))
                .build();

        assertThrows(TransicionEstadoTurnoException.class, () -> turno.cancelar("motivo", false));
    }

    @Test
    @DisplayName("Cancelación por médico sin motivo obligatorio debe lanzar ReglaNegocioTurnoException")
    void testCancelarPorDoctorSinMotivoFalla() {
        Turno turno = Turno.builder()
                .estado(EstadoTurno.CONFIRMADO)
                .fechaHora(LocalDateTime.now().plusDays(1))
                .build();

        assertThrows(ReglaNegocioTurnoException.class, () -> turno.cancelar("   ", true));
    }

    @Test
    @DisplayName("Cancelación por médico con motivo válido debe registrar estado y motivo")
    void testCancelarPorDoctorConMotivoExitoso() {
        Turno turno = Turno.builder()
                .estado(EstadoTurno.CONFIRMADO)
                .fechaHora(LocalDateTime.now().plusDays(1))
                .build();

        turno.cancelar("Urgencia en quirófano", true);

        assertEquals(EstadoTurno.CANCELADO, turno.getEstado());
        assertEquals("Urgencia en quirófano", turno.getMotivoCancelacion());
    }

    @Test
    @DisplayName("Transición CONFIRMADO -> COMPLETADO para turno pasado debe ser exitosa")
    void testCompletarTurnoConfirmadoConFechaPasadaExitoso() {
        Turno turno = Turno.builder()
                .estado(EstadoTurno.CONFIRMADO)
                .fechaHora(LocalDateTime.now().minusHours(2))
                .build();

        turno.completar();

        assertEquals(EstadoTurno.COMPLETADO, turno.getEstado());
    }

    @Test
    @DisplayName("Completar un turno cuya fecha y hora es futura debe lanzar ReglaNegocioTurnoException")
    void testCompletarTurnoConFechaFuturaFalla() {
        Turno turno = Turno.builder()
                .estado(EstadoTurno.CONFIRMADO)
                .fechaHora(LocalDateTime.now().plusDays(2))
                .build();

        assertThrows(ReglaNegocioTurnoException.class, turno::completar);
    }

    @Test
    @DisplayName("Completar un turno no confirmado (ej. PENDIENTE) debe lanzar TransicionEstadoTurnoException")
    void testCompletarTurnoNoConfirmadoFalla() {
        Turno turno = Turno.builder()
                .estado(EstadoTurno.PENDIENTE)
                .fechaHora(LocalDateTime.now().minusHours(1))
                .build();

        assertThrows(TransicionEstadoTurnoException.class, turno::completar);
    }

    @Test
    @DisplayName("Transición a AUSENTE para cita pasada debe ser exitosa")
    void testMarcarAusenteTurnoConFechaPasadaExitoso() {
        Turno turno = Turno.builder()
                .estado(EstadoTurno.CONFIRMADO)
                .fechaHora(LocalDateTime.now().minusMinutes(45))
                .build();

        turno.marcarAusente();

        assertEquals(EstadoTurno.AUSENTE, turno.getEstado());
    }

    @Test
    @DisplayName("Marcar ausente en una cita antes de su horario debe lanzar ReglaNegocioTurnoException")
    void testMarcarAusenteTurnoConFechaFuturaFalla() {
        Turno turno = Turno.builder()
                .estado(EstadoTurno.CONFIRMADO)
                .fechaHora(LocalDateTime.now().plusHours(3))
                .build();

        assertThrows(ReglaNegocioTurnoException.class, turno::marcarAusente);
    }

    @Test
    @DisplayName("Cualquier transición desde un estado terminal (CANCELADO, COMPLETADO, AUSENTE) debe ser rechazada")
    void testTransicionarADesdeEstadoTerminalFalla() {
        Turno cancelado = Turno.builder().estado(EstadoTurno.CANCELADO).build();
        Turno completado = Turno.builder().estado(EstadoTurno.COMPLETADO).build();
        Turno ausente = Turno.builder().estado(EstadoTurno.AUSENTE).build();

        assertThrows(TransicionEstadoTurnoException.class, () -> cancelado.transicionarA(EstadoTurno.CONFIRMADO));
        assertThrows(TransicionEstadoTurnoException.class, () -> completado.transicionarA(EstadoTurno.PENDIENTE));
        assertThrows(TransicionEstadoTurnoException.class, () -> ausente.transicionarA(EstadoTurno.CANCELADO));
    }

    @Test
    @DisplayName("Transicionar al mismo estado actual debe lanzar TransicionEstadoTurnoException")
    void testTransicionarAMismoEstadoFalla() {
        Turno confirmado = Turno.builder().estado(EstadoTurno.CONFIRMADO).build();
        assertThrows(TransicionEstadoTurnoException.class, () -> confirmado.transicionarA(EstadoTurno.CONFIRMADO));
    }
}
