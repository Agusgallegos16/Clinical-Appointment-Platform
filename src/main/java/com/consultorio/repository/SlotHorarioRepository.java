package com.consultorio.repository;

import com.consultorio.domain.SlotHorario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface SlotHorarioRepository extends JpaRepository<SlotHorario, Long> {
    List<SlotHorario> findByDoctorId(UUID doctorId);
    List<SlotHorario> findByDoctorIdAndFecha(UUID doctorId, LocalDate fecha);
    boolean existsByDoctorIdAndFechaAndHoraInicio(UUID doctorId, LocalDate fecha, LocalTime horaInicio);
    List<SlotHorario> findByDoctorIdAndFechaBetween(UUID doctorId, LocalDate desde, LocalDate hasta);
    void deleteByDoctorIdAndFechaBetween(UUID doctorId, LocalDate desde, LocalDate hasta);
    void deleteByDoctorIdAndFecha(UUID doctorId, LocalDate fecha);
}
