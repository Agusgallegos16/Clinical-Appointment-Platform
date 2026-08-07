package com.consultorio.repository;

import com.consultorio.domain.EstadoTurno;
import com.consultorio.domain.Turno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TurnoRepository extends JpaRepository<Turno, Long> {

    List<Turno> findByPacienteIdOrderByFechaHoraDesc(Long pacienteId);

    List<Turno> findByDoctorIdAndFechaHoraBetweenOrderByFechaHoraAsc(
            Long doctorId, LocalDateTime desde, LocalDateTime hasta);

    List<Turno> findByDoctorIdAndFechaHoraBetweenAndEstadoNot(
            Long doctorId, LocalDateTime desde, LocalDateTime hasta, EstadoTurno estadoNoDeseado);

    boolean existsByDoctorIdAndFechaHoraAndEstadoNot(
            Long doctorId, LocalDateTime fechaHora, EstadoTurno estadoNoDeseado);
}
