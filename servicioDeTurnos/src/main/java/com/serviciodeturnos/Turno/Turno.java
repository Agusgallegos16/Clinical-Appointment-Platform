package com.serviciodeturnos.Turno;

import com.serviciodeturnos.Doctor.Doctor;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "turnos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Turno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate dia;
    private LocalTime horario;
    
    // Asumiremos que la duración es en minutos
    private Integer duracionMinutos;

    // Relación con el Doctor en este mismo microservicio
    @ManyToOne
    @JoinColumn(name = "doctor_id")
    private Doctor doctor;

    // Relación "mágica": guardamos solo el ID del paciente, 
    // porque el objeto Paciente vive en otro microservicio.
    private Long pacienteId;
}
