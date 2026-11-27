package com.consultorio.repository;

import com.consultorio.domain.SlotHorario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query("SELECT s FROM SlotHorario s LEFT JOIN FETCH s.especialidad " +
           "WHERE s.doctor.id = :doctorId AND s.fecha >= :fechaDesde " +
           "ORDER BY s.fecha ASC, s.horaInicio ASC")
    List<SlotHorario> findFuturosPorDoctorOrdenados(@Param("doctorId") UUID doctorId,
                                                    @Param("fechaDesde") LocalDate fechaDesde);
}
