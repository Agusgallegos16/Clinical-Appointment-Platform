package com.consultorio.domain;

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

    @Column(length = 100)
    private String email;

    private java.time.LocalDate fechaNacimiento;

    public String getTelefono() {
        if (this.telefono != null && !this.telefono.trim().isEmpty()) {
            return this.telefono;
        }
        return this.tutor != null ? this.tutor.getTelefono() : null;
    }

    public String getEmail() {
        if (this.email != null && !this.email.trim().isEmpty()) {
            return this.email.trim();
        }
        if (this.usuario != null && this.usuario.getEmail() != null && !this.usuario.getEmail().trim().isEmpty()) {
            return this.usuario.getEmail().trim();
        }
        if (this.tutor != null && this.tutor.getEmail() != null) {
            return this.tutor.getEmail();
        }
        return null;
    }

    public Integer getEdad() {
        if (this.fechaNacimiento == null) return null;
        return java.time.Period.between(this.fechaNacimiento, java.time.LocalDate.now()).getYears();
    }

}
