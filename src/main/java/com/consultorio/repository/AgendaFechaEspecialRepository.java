package com.consultorio.repository;

import com.consultorio.domain.AgendaFechaEspecial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AgendaFechaEspecialRepository extends JpaRepository<AgendaFechaEspecial, Long> {
    Optional<AgendaFechaEspecial> findByDoctorIdAndFecha(UUID doctorId, LocalDate fecha);
}
