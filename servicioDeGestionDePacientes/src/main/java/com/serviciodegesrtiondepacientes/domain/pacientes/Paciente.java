package com.serviciodegesrtiondepacientes.domain.pacientes;

import com.serviciodegesrtiondepacientes.domain.recetas.Receta;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "pacientes")
@SQLRestriction("activo = true")
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Paciente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    @Size(min = 2, max = 100, message = "El apellido debe tener entre 2 y 100 caracteres")
    private String apellido;

    @NotNull(message = "El DNI es obligatorio")
    @Min(value = 1000000, message = "El DNI debe tener al menos 7 dígitos")
    @Max(value = 99999999, message = "El DNI no puede tener más de 8 dígitos")
    @Column(unique = true)
    private Long dni;

    @NotNull(message = "El sexo es obligatorio")
    @Enumerated(EnumType.STRING)
    private Sexo sexo;

    @Email(message = "El formato del email no es válido")
    @NotBlank(message = "El email es obligatorio")
    private String mail;

    @NotBlank(message = "El número de teléfono es obligatorio")
    @Pattern(regexp = "^\\+?[0-9\\-\\s]{7,15}$", message = "El formato del teléfono no es válido")
    private String numeroDeTelefono;

    @Enumerated(EnumType.STRING)
    private ObraSocial obraSocial;

    @Column(unique = true)
    private String numeroObraSocial;

    // --- Borrado lógico ---
    @Column(nullable = false)
    private boolean activo = true;

    // --- Auditoría ---
    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime fechaCreacion;

    @LastModifiedDate
    private LocalDateTime fechaUltimaModificacion;

    @OneToMany(mappedBy = "paciente", cascade = CascadeType.ALL)
    private List<Receta> recetas;
}
