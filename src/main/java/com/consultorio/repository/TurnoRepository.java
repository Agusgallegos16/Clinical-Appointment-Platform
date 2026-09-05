package com.consultorio.repository;

import com.consultorio.domain.EstadoTurno;
import com.consultorio.domain.Turno;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface TurnoRepository extends JpaRepository<Turno, UUID> {

    @EntityGraph(attributePaths = {"paciente", "doctor", "especialidad"})
    List<Turno> findByPacienteIdOrderByFechaHoraDesc(UUID pacienteId);

    @EntityGraph(attributePaths = {"paciente", "doctor", "especialidad"})
    List<Turno> findByDoctorIdAndFechaHoraBetweenOrderByFechaHoraAsc(
            UUID doctorId, LocalDateTime desde, LocalDateTime hasta);

    @EntityGraph(attributePaths = {"paciente", "doctor", "especialidad"})
    List<Turno> findByDoctorIdAndFechaHoraBetweenAndEstadoNot(
            UUID doctorId, LocalDateTime desde, LocalDateTime hasta, EstadoTurno estadoNoDeseado);

    @EntityGraph(attributePaths = {"paciente", "doctor", "especialidad"})
    List<Turno> findByDoctorIdAndFechaHoraBetweenAndEstadoNotOrderByFechaHoraAsc(
            UUID doctorId, LocalDateTime desde, LocalDateTime hasta, EstadoTurno estadoNoDeseado);

    boolean existsByDoctorIdAndFechaHoraAndEstadoNot(
            UUID doctorId, LocalDateTime fechaHora, EstadoTurno estadoNoDeseado);

    boolean existsByPacienteIdAndFechaHoraAndEstadoNot(
            UUID pacienteId, LocalDateTime fechaHora, EstadoTurno estadoNoDeseado);

    @EntityGraph(attributePaths = {"paciente", "doctor", "especialidad"})
    List<Turno> findByFechaHoraBetweenAndEstadoInAndRecordatorio48hsEnviadoFalse(
            LocalDateTime desde, LocalDateTime hasta, List<EstadoTurno> estados);
}
