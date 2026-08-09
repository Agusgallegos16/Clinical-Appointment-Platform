package com.consultorio.repository;

import com.consultorio.domain.SlotHorario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SlotHorarioRepository extends JpaRepository<SlotHorario, Long> {
    List<SlotHorario> findByDoctorId(Long doctorId);
    List<SlotHorario> findByDoctorIdAndFecha(Long doctorId, LocalDate fecha);
    List<SlotHorario> findByDoctorIdAndFechaBetween(Long doctorId, LocalDate desde, LocalDate hasta);
    void deleteByDoctorIdAndFechaBetween(Long doctorId, LocalDate desde, LocalDate hasta);
    void deleteByDoctorIdAndFecha(Long doctorId, LocalDate fecha);
}
