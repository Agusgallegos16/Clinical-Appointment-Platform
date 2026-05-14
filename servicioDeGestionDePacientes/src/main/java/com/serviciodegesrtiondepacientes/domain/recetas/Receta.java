package com.serviciodegesrtiondepacientes.domain.recetas;

import com.serviciodegesrtiondepacientes.domain.pacientes.Paciente;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Entity
@Table(name = "recetas")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Receta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private Medicamento nombreMedicamento;

    private String descripcion;

    @Temporal(TemporalType.DATE)
    private Date diaEmision;

    @Temporal(TemporalType.DATE)
    private Date diaDeCaducidad;

    // Solo guardamos el ID del doctor que la emitió
    private Long emitidaPorDoctorId;

    @ManyToOne
    @JoinColumn(name = "paciente_id")
    private Paciente paciente;
}
