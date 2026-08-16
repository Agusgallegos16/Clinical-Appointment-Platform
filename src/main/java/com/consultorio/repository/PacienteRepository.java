package com.consultorio.repository;

import com.consultorio.domain.Paciente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PacienteRepository extends JpaRepository<Paciente, UUID> {
    Optional<Paciente> findByUsuarioId(Long usuarioId);
    Optional<Paciente> findByUsuarioEmail(String email);
    Optional<Paciente> findByDni(Long dni);
    boolean existsByDni(Long dni);
    java.util.List<Paciente> findByTutorId(UUID tutorId);
    java.util.List<Paciente> findByTutorIsNotNull();
}
