package com.consultorio.repository;

import com.consultorio.domain.EstadoTurno;
import com.consultorio.domain.Turno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface TurnoRepository extends JpaRepository<Turno, UUID> {

    List<Turno> findByPacienteIdOrderByFechaHoraDesc(UUID pacienteId);

    List<Turno> findByDoctorIdAndFechaHoraBetweenOrderByFechaHoraAsc(
            UUID doctorId, LocalDateTime desde, LocalDateTime hasta);

    List<Turno> findByDoctorIdAndFechaHoraBetweenAndEstadoNot(
            UUID doctorId, LocalDateTime desde, LocalDateTime hasta, EstadoTurno estadoNoDeseado);

    List<Turno> findByDoctorIdAndFechaHoraBetweenAndEstadoNotOrderByFechaHoraAsc(
            UUID doctorId, LocalDateTime desde, LocalDateTime hasta, EstadoTurno estadoNoDeseado);

    boolean existsByDoctorIdAndFechaHoraAndEstadoNot(
            UUID doctorId, LocalDateTime fechaHora, EstadoTurno estadoNoDeseado);

    List<Turno> findByFechaHoraBetweenAndEstadoInAndRecordatorio48hsEnviadoFalse(
            LocalDateTime desde, LocalDateTime hasta, List<EstadoTurno> estados);
}
