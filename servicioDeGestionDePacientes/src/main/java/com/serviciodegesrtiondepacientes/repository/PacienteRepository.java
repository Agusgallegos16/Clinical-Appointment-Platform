package com.serviciodegesrtiondepacientes.repository;

import com.serviciodegesrtiondepacientes.domain.pacientes.Paciente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PacienteRepository extends JpaRepository<Paciente, Long> {
}
