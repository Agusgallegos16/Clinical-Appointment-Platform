package com.serviciodegesrtiondepacientes.repository;

import com.serviciodegesrtiondepacientes.domain.pacientes.Paciente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PacienteRepository extends JpaRepository<Paciente, Long> {

    // --- Búsqueda por DNI ---
    Optional<Paciente> findByDni(Long dni);

    // --- Verificar existencia por DNI (para validación de unicidad) ---
    boolean existsByDni(Long dni);

    // --- Búsqueda por nombre y/o apellido (parcial, case-insensitive) ---
    Page<Paciente> findByNombreContainingIgnoreCaseOrApellidoContainingIgnoreCase(
            String nombre, String apellido, Pageable pageable);

    // --- Listado paginado (ya filtra los inactivos gracias a @SQLRestriction) ---
    Page<Paciente> findAll(Pageable pageable);

    // --- Borrado lógico: necesitamos un query nativo para ignorar el filtro @SQLRestriction ---
    @Modifying
    @Query("UPDATE Paciente p SET p.activo = false WHERE p.id = :id")
    void desactivarPaciente(@Param("id") Long id);
}
