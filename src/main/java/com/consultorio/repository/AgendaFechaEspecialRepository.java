package com.consultorio.repository;

import com.consultorio.domain.AgendaFechaEspecial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface AgendaFechaEspecialRepository extends JpaRepository<AgendaFechaEspecial, Long> {
    Optional<AgendaFechaEspecial> findByDoctorIdAndFecha(Long doctorId, LocalDate fecha);
}
