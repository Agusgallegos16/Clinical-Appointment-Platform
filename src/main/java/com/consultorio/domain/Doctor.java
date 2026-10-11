package com.consultorio.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "doctores", indexes = {
    @Index(name = "idx_doctores_usuario", columnList = "usuario_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", referencedColumnName = "id", nullable = false)
    private Usuario usuario;

    @NotBlank(message = "El nombre es obligatorio")
    @Column(nullable = false, length = 100)
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    @Column(nullable = false, length = 100)
    private String apellido;

    @Column(name = "foto_url", length = 500)
    private String fotoUrl;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "doctor_especialidades",
        joinColumns = @JoinColumn(name = "doctor_id"),
        inverseJoinColumns = @JoinColumn(name = "especialidad_id")
    )
    @Builder.Default
    private Set<Especialidad> especialidades = new HashSet<>();

    @Builder.Default
    @Column(name = "disponible_para_turnos", columnDefinition = "boolean default true")
    private Boolean disponibleParaTurnos = true;

    public boolean isDisponibleParaTurnos() {
        return Boolean.TRUE.equals(this.disponibleParaTurnos);
    }

    @Builder.Default
    @Column(name = "tiene_advertencia_bloqueante", columnDefinition = "boolean default false")
    private Boolean tieneAdvertenciaBloqueante = false;

    @Column(name = "mensaje_advertencia_bloqueante", length = 1000)
    private String mensajeAdvertenciaBloqueante;

    public boolean isTieneAdvertenciaBloqueante() {
        return Boolean.TRUE.equals(this.tieneAdvertenciaBloqueante);
    }

    @Builder.Default
    @Column(name = "tiene_advertencia_informativa", columnDefinition = "boolean default false")
    private Boolean tieneAdvertenciaInformativa = false;

    @Column(name = "mensaje_advertencia_informativa", length = 1000)
    private String mensajeAdvertenciaInformativa;

    public boolean isTieneAdvertenciaInformativa() {
        return Boolean.TRUE.equals(this.tieneAdvertenciaInformativa);
    }
}
