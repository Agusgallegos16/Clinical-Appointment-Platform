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

    @Transactional
    public Especialidad crearEspecialidad(Especialidad especialidad) {
        if (especialidadRepository.existsByNombreIgnoreCase(especialidad.getNombre())) {
            throw new IllegalArgumentException("Ya existe una especialidad con el nombre: " + especialidad.getNombre());
        }
        return especialidadRepository.save(especialidad);
    }

    @Transactional
    public void eliminarEspecialidad(Long id) {
        if (!especialidadRepository.existsById(id)) {
            throw new IllegalArgumentException("Especialidad no encontrada con ID: " + id);
        }
        especialidadRepository.deleteById(id);
    }
}
