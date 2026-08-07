package com.consultorio.repository;

import com.consultorio.domain.PlantillaAgenda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlantillaAgendaRepository extends JpaRepository<PlantillaAgenda, Long> {
    List<PlantillaAgenda> findByDoctorId(Long doctorId);
}
