package com.consultorio.domain;

import com.consultorio.exception.ReglaNegocioTurnoException;
import com.consultorio.exception.TransicionEstadoTurnoException;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "turnos", indexes = {
    @Index(name = "idx_turnos_doctor_fecha", columnList = "doctor_id, fecha_hora"),
    @Index(name = "idx_turnos_paciente_fecha", columnList = "paciente_id, fecha_hora"),
    @Index(name = "idx_turnos_estado", columnList = "estado")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Turno {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "especialidad_id", nullable = false)
    private Especialidad especialidad;

    @NotNull(message = "La fecha y hora son obligatorias")
    @Column(nullable = false)
    private LocalDateTime fechaHora;

    @NotNull(message = "El estado es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private EstadoTurno estado = EstadoTurno.PENDIENTE;

    @Column(length = 500)
    private String motivoConsulta;

    @Column(length = 500)
    private String motivoCancelacion;

    @Builder.Default
    private boolean tieneObraSocial = false;

    @Column(length = 150)
    private String obraSocial;

    @Builder.Default
    private boolean recordatorio48hsEnviado = false;

    @Column(name = "google_event_id")
    private String googleEventId;

    @Column(name = "google_event_id_doctor")
    private String googleEventIdDoctor;

    @Column(name = "google_event_id_secretaria")
    private String googleEventIdSecretaria;

    public void confirmar() {
        validarNoTerminal();
        if (this.estado != EstadoTurno.PENDIENTE) {
            throw new TransicionEstadoTurnoException("Solo se pueden confirmar turnos en estado PENDIENTE. Estado actual: " + this.estado);
        }
        if (this.fechaHora != null && this.fechaHora.isBefore(LocalDateTime.now())) {
            throw new ReglaNegocioTurnoException("No se puede confirmar un turno cuya fecha y hora ya transcurrió.");
        }
        this.estado = EstadoTurno.CONFIRMADO;
    }

    public void cancelar(String motivo, boolean porDoctor) {
        validarNoTerminal();
        if (porDoctor && (motivo == null || motivo.isBlank())) {
            throw new ReglaNegocioTurnoException("Debe ingresar una justificación obligatoria para la cancelación médica.");
        }
        this.estado = EstadoTurno.CANCELADO;
        if (motivo != null && !motivo.isBlank()) {
            this.motivoCancelacion = motivo.trim();
        }
    }

    public void completar() {
        validarNoTerminal();
        if (this.estado != EstadoTurno.CONFIRMADO) {
            throw new TransicionEstadoTurnoException("Solo se pueden completar turnos que hayan sido previamente confirmados. Estado actual: " + this.estado);
        }
        if (this.fechaHora != null && this.fechaHora.isAfter(LocalDateTime.now())) {
            throw new ReglaNegocioTurnoException("No se puede completar un turno antes de la fecha y hora de la cita programada.");
        }
        this.estado = EstadoTurno.COMPLETADO;
    }

    public void marcarAusente() {
        validarNoTerminal();
        if (this.estado != EstadoTurno.CONFIRMADO && this.estado != EstadoTurno.PENDIENTE) {
            throw new TransicionEstadoTurnoException("Solo turnos agendados pueden marcarse como AUSENTE. Estado actual: " + this.estado);
        }
        if (this.fechaHora != null && this.fechaHora.isAfter(LocalDateTime.now())) {
            throw new ReglaNegocioTurnoException("No se puede registrar ausencia antes de la fecha y hora de la cita programada.");
        }
        this.estado = EstadoTurno.AUSENTE;
    }

    public void transicionarA(EstadoTurno nuevoEstado) {
        if (nuevoEstado == null) {
            throw new IllegalArgumentException("El nuevo estado no puede ser nulo.");
        }
        if (this.estado == nuevoEstado) {
            throw new TransicionEstadoTurnoException("El turno ya se encuentra en estado " + nuevoEstado + ".");
        }
        validarNoTerminal();

        switch (nuevoEstado) {
            case CONFIRMADO -> confirmar();
            case CANCELADO -> cancelar(this.motivoCancelacion, false);
            case COMPLETADO -> completar();
            case AUSENTE -> marcarAusente();
            case PENDIENTE -> throw new TransicionEstadoTurnoException("No se puede regresar un turno al estado PENDIENTE.");
        }
    }

    private void validarNoTerminal() {
        if (this.estado == EstadoTurno.CANCELADO) {
            throw new TransicionEstadoTurnoException("No se puede modificar un turno que ya ha sido CANCELADO.");
        }
        if (this.estado == EstadoTurno.COMPLETADO) {
            throw new TransicionEstadoTurnoException("No se puede modificar un turno que ya ha sido COMPLETADO.");
        }
        if (this.estado == EstadoTurno.AUSENTE) {
            throw new TransicionEstadoTurnoException("No se puede modificar un turno que ya ha sido marcado como AUSENTE.");
        }
    }
}
