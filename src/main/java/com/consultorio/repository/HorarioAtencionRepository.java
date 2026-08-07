package com.consultorio.repository;

import com.consultorio.domain.DiaSemana;
import com.consultorio.domain.HorarioAtencion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface HorarioAtencionRepository extends JpaRepository<HorarioAtencion, Long> {
    List<HorarioAtencion> findByDoctorId(Long doctorId);
    List<HorarioAtencion> findByDoctorIdAndFecha(Long doctorId, LocalDate fecha);
    List<HorarioAtencion> findByDoctorIdAndDiaSemanaAndFechaIsNull(Long doctorId, DiaSemana diaSemana);
    void deleteByDoctorIdAndFecha(Long doctorId, LocalDate fecha);
    void deleteByDoctorIdAndDiaSemanaAndFechaIsNull(Long doctorId, DiaSemana diaSemana);
}
