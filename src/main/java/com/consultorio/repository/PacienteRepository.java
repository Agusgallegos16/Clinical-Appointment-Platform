package com.consultorio.repository;

import com.consultorio.domain.Paciente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PacienteRepository extends JpaRepository<Paciente, Long> {
    Optional<Paciente> findByUsuarioId(Long usuarioId);
    Optional<Paciente> findByUsuarioEmail(String email);
    Optional<Paciente> findByDni(Long dni);
    boolean existsByDni(Long dni);
    java.util.List<Paciente> findByTutorId(Long tutorId);
    java.util.List<Paciente> findByTutorIsNotNull();
}
