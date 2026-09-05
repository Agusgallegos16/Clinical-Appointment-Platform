package com.consultorio.repository;

import com.consultorio.domain.PlantillaAgenda;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PlantillaAgendaRepository extends JpaRepository<PlantillaAgenda, Long> {
    @EntityGraph(attributePaths = {"detalles", "detalles.especialidad"})
    List<PlantillaAgenda> findByDoctorId(UUID doctorId);
}
