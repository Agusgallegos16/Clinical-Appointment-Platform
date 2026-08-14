package com.consultorio.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "pacientes", indexes = {
    @Index(name = "idx_pacientes_dni", columnList = "dni"),
    @Index(name = "idx_pacientes_usuario", columnList = "usuario_id"),
    @Index(name = "idx_pacientes_tutor", columnList = "tutor_id")
})
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Paciente {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", referencedColumnName = "id", nullable = true)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tutor_id")
    private Paciente tutor;

    @NotBlank(message = "El nombre es obligatorio")
    @Column(nullable = false, length = 100)
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    @Column(nullable = false, length = 100)
    private String apellido;

    @NotNull(message = "El DNI es obligatorio")
    @Min(value = 1000000, message = "El DNI debe ser válido")
    @Column(unique = true, nullable = false)
    private Long dni;

    @Column(length = 50)
    private String telefono;

    private java.time.LocalDate fechaNacimiento;

    public String getTelefono() {
        if (this.telefono != null && !this.telefono.trim().isEmpty()) {
            return this.telefono;
        }
        return this.tutor != null ? this.tutor.getTelefono() : null;
    }

    public Integer getEdad() {
        if (this.fechaNacimiento == null) return null;
        return java.time.Period.between(this.fechaNacimiento, java.time.LocalDate.now()).getYears();
    }

    public boolean esMenor() {
        Integer edad = getEdad();
        return edad != null && edad < 18;
    }
}
