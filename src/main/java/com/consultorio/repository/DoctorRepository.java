package com.consultorio.repository;

import com.consultorio.domain.Doctor;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000")})
    @Query("SELECT d FROM Doctor d WHERE d.id = :id")
    Optional<Doctor> findByIdWithLock(@Param("id") UUID id);

    @EntityGraph(attributePaths = {"usuario", "especialidades"})
    List<Doctor> findAll();

    @EntityGraph(attributePaths = {"usuario", "especialidades"})
    Optional<Doctor> findByUsuarioId(Long usuarioId);

    @EntityGraph(attributePaths = {"usuario", "especialidades"})
    Optional<Doctor> findByUsuarioEmail(String email);

    @EntityGraph(attributePaths = {"usuario", "especialidades"})
    Optional<Doctor> findByUsuarioEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = {"usuario", "especialidades"})
    List<Doctor> findByEspecialidadesId(Long especialidadId);

    @EntityGraph(attributePaths = {"usuario", "especialidades"})
    List<Doctor> findByDisponibleParaTurnosTrue();

    @EntityGraph(attributePaths = {"usuario", "especialidades"})
    List<Doctor> findByEspecialidadesIdAndDisponibleParaTurnosTrue(Long especialidadId);
}
