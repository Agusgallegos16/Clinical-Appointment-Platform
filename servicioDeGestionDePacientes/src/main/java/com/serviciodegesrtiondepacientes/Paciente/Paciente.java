package com.serviciodegesrtiondepacientes.Paciente;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "pacientes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Paciente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;
    private String apellido;
    
    @Column(unique = true)
    private Long dni;
    
    @Enumerated(EnumType.STRING)
    private Sexo sexo;
    
    private String mail;
    private String numeroDeTelefono;
    private String tipoDeSangre;

    @OneToMany(mappedBy = "paciente", cascade = CascadeType.ALL)
    private java.util.List<com.serviciodegesrtiondepacientes.Receta.Receta> recetas;
}
