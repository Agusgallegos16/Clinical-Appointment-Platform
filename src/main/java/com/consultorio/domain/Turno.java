package com.consultorio.domain;

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
}
