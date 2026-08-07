package com.consultorio.repository;

import com.consultorio.domain.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {
    Optional<Doctor> findByUsuarioId(Long usuarioId);
    Optional<Doctor> findByUsuarioEmail(String email);
    List<Doctor> findByEspecialidadesId(Long especialidadId);
    boolean existsByMatricula(String matricula);
}
