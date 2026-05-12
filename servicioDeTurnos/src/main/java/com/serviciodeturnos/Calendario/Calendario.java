package com.serviciodeturnos.Calendario;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Entity
@Table(name = "calendarios")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Calendario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Temporal(TemporalType.DATE)
    private Date fecha;
    
    // El calendario usualmente está asociado a un Doctor, para saber
    // qué turnos tiene ese doctor en esa fecha.
    private Long doctorId;
}
