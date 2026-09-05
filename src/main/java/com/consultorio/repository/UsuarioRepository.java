package com.consultorio.repository;

import com.consultorio.domain.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);
    java.util.List<Usuario> findByEmailContainingIgnoreCase(String email);
    Page<Usuario> findByEmailContainingIgnoreCase(String email, Pageable pageable);

    @Query(value = "SELECT DISTINCT u FROM Usuario u " +
            "LEFT JOIN Paciente p ON p.usuario = u " +
            "LEFT JOIN Doctor d ON d.usuario = u " +
            "WHERE LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(p.nombre) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(p.apellido) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(CONCAT(p.nombre, ' ', p.apellido)) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(CONCAT(p.apellido, ' ', p.nombre)) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(d.nombre) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(d.apellido) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(CONCAT(d.nombre, ' ', d.apellido)) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(CONCAT(d.apellido, ' ', d.nombre)) LIKE LOWER(CONCAT('%', :query, '%'))",
            countQuery = "SELECT COUNT(DISTINCT u) FROM Usuario u " +
            "LEFT JOIN Paciente p ON p.usuario = u " +
            "LEFT JOIN Doctor d ON d.usuario = u " +
            "WHERE LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(p.nombre) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(p.apellido) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(CONCAT(p.nombre, ' ', p.apellido)) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(CONCAT(p.apellido, ' ', p.nombre)) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(d.nombre) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(d.apellido) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(CONCAT(d.nombre, ' ', d.apellido)) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(CONCAT(d.apellido, ' ', d.nombre)) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Usuario> buscarPorNombreApellidoOEmail(@Param("query") String query, Pageable pageable);

    boolean existsByEmail(String email);
    Optional<Usuario> findByTokenVerificacionEmail(String token);
    Optional<Usuario> findByTokenRestablecimientoPassword(String token);
}
