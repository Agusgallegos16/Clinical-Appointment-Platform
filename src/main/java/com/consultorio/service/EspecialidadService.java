package com.consultorio.service;

import com.consultorio.domain.Especialidad;
import com.consultorio.repository.EspecialidadRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EspecialidadService {

    private final EspecialidadRepository especialidadRepository;

    @Autowired
    public EspecialidadService(EspecialidadRepository especialidadRepository) {
        this.especialidadRepository = especialidadRepository;
    }

    public List<Especialidad> listarTodas() {
        return especialidadRepository.findAll();
    }

    public Especialidad obtenerPorId(Long id) {
        return especialidadRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Especialidad no encontrada con ID: " + id));
    }

    public java.util.Optional<Especialidad> buscarPorId(Long id) {
        if (id == null) {
            return java.util.Optional.empty();
        }
        return especialidadRepository.findById(id);
    }

    public List<Especialidad> obtenerTodasPorIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return especialidadRepository.findAllById(ids);
    }

    @Transactional
    public Especialidad crearEspecialidad(String nombre, String descripcion) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre de la especialidad es obligatorio");
        }
        if (especialidadRepository.existsByNombreIgnoreCase(nombre.trim())) {
            throw new IllegalArgumentException("Ya existe una especialidad con el nombre: " + nombre.trim());
        }
        Especialidad especialidad = Especialidad.builder()
                .nombre(nombre.trim())
                .descripcion(descripcion != null ? descripcion.trim() : null)
                .build();
        return especialidadRepository.save(especialidad);
    }

    @Transactional
    public Especialidad actualizarEspecialidad(Long id, String nombre, String descripcion) {
        Especialidad existente = obtenerPorId(id);

        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre de la especialidad es obligatorio");
        }

        if (especialidadRepository.existsByNombreIgnoreCaseAndIdNot(nombre.trim(), id)) {
            throw new IllegalArgumentException("Ya existe otra especialidad con el nombre: " + nombre.trim());
        }

        existente.setNombre(nombre.trim());
        existente.setDescripcion(descripcion != null ? descripcion.trim() : null);

        return especialidadRepository.save(existente);
    }

    @Transactional
    public void eliminarEspecialidad(Long id) {
        if (!especialidadRepository.existsById(id)) {
            throw new IllegalArgumentException("Especialidad no encontrada con ID: " + id);
        }
        especialidadRepository.deleteById(id);
    }
}
