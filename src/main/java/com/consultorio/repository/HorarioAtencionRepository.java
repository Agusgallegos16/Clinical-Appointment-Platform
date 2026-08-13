package com.consultorio.repository;

import com.consultorio.domain.DiaSemana;
import com.consultorio.domain.HorarioAtencion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface HorarioAtencionRepository extends JpaRepository<HorarioAtencion, Long> {
    List<HorarioAtencion> findByDoctorId(UUID doctorId);
    List<HorarioAtencion> findByDoctorIdAndFecha(UUID doctorId, LocalDate fecha);
    List<HorarioAtencion> findByDoctorIdAndDiaSemanaAndFechaIsNull(UUID doctorId, DiaSemana diaSemana);
    void deleteByDoctorIdAndFecha(UUID doctorId, LocalDate fecha);
    void deleteByDoctorIdAndDiaSemanaAndFechaIsNull(UUID doctorId, DiaSemana diaSemana);
}
