package com.consultorio.repository;

import com.consultorio.domain.BloqueoHorario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BloqueoHorarioRepository extends JpaRepository<BloqueoHorario, Long> {
    List<BloqueoHorario> findByDoctorId(Long doctorId);
    List<BloqueoHorario> findByDoctorIdAndFecha(Long doctorId, LocalDate fecha);
    void deleteByDoctorIdAndFecha(Long doctorId, LocalDate fecha);
}
