package com.consultorio.repository;

import com.consultorio.domain.BloqueoHorario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface BloqueoHorarioRepository extends JpaRepository<BloqueoHorario, Long> {
    List<BloqueoHorario> findByDoctorId(UUID doctorId);
    List<BloqueoHorario> findByDoctorIdAndFecha(UUID doctorId, LocalDate fecha);
    void deleteByDoctorIdAndFecha(UUID doctorId, LocalDate fecha);
}
