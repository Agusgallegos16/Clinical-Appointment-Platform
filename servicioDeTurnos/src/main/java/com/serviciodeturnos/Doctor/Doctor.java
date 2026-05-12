package com.serviciodeturnos.Doctor;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.List;

@Entity
@Table(name = "doctores")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;
    private String apellido;

    private LocalTime horarioDeEntrada;
    private LocalTime horarioDeSalida;

    @ElementCollection(targetClass = Especialidad.class)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "doctor_especialidades", joinColumns = @JoinColumn(name = "doctor_id"))
    private List<Especialidad> especialidades;
}
