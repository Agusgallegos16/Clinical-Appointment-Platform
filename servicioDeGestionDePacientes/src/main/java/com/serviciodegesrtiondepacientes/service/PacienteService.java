package com.serviciodegesrtiondepacientes.service;

import com.serviciodegesrtiondepacientes.domain.pacientes.Paciente;
import com.serviciodegesrtiondepacientes.exception.DniDuplicadoException;
import com.serviciodegesrtiondepacientes.exception.PacienteNotFoundException;
import com.serviciodegesrtiondepacientes.repository.PacienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@Service
public class PacienteService {

    private final PacienteRepository pacienteRepository;

    @Autowired
    public PacienteService(PacienteRepository pacienteRepository) {
        this.pacienteRepository = pacienteRepository;
    }

    // --- Listado paginado de pacientes activos ---
    public Page<Paciente> obtenerTodosLosPacientes(Pageable pageable) {
        return pacienteRepository.findAll(pageable);
    }

    // --- Obtener paciente por ID ---
    public Paciente obtenerPacientePorId(Long id) {
        return pacienteRepository.findById(id)
                .orElseThrow(() -> new PacienteNotFoundException(id));
    }

    // --- Búsqueda por DNI ---
    public Paciente obtenerPacientePorDni(Long dni) {
        return pacienteRepository.findByDni(dni)
                .orElseThrow(() -> new PacienteNotFoundException("Paciente no encontrado con DNI: " + dni));
    }

    // --- Búsqueda por nombre o apellido (parcial, paginada) ---
    public Page<Paciente> buscarPorNombreOApellido(String termino, Pageable pageable) {
        return pacienteRepository.findByNombreContainingIgnoreCaseOrApellidoContainingIgnoreCase(
                termino, termino, pageable);
    }

    // --- Crear paciente con validación de DNI único ---
    @Transactional
    public Paciente guardarPaciente(Paciente paciente) {
        if (pacienteRepository.existsByDni(paciente.getDni())) {
            throw new DniDuplicadoException(paciente.getDni());
        }
        paciente.setActivo(true);
        return pacienteRepository.save(paciente);
    }

    // --- Actualizar paciente ---
    @Transactional
    public Paciente actualizarPaciente(Long id, Paciente pacienteActualizado) {
        Paciente pacienteExistente = pacienteRepository.findById(id)
                .orElseThrow(() -> new PacienteNotFoundException(id));

        // Si cambió el DNI, verificar que el nuevo DNI no esté en uso por otro paciente
        if (!pacienteExistente.getDni().equals(pacienteActualizado.getDni())
                && pacienteRepository.existsByDni(pacienteActualizado.getDni())) {
            throw new DniDuplicadoException(pacienteActualizado.getDni());
        }

        pacienteExistente.setNombre(pacienteActualizado.getNombre());
        pacienteExistente.setApellido(pacienteActualizado.getApellido());
        pacienteExistente.setDni(pacienteActualizado.getDni());
        pacienteExistente.setSexo(pacienteActualizado.getSexo());
        pacienteExistente.setMail(pacienteActualizado.getMail());
        pacienteExistente.setNumeroDeTelefono(pacienteActualizado.getNumeroDeTelefono());
        pacienteExistente.setObraSocial(pacienteActualizado.getObraSocial());
        pacienteExistente.setNumeroObraSocial(pacienteActualizado.getNumeroObraSocial());

        return pacienteRepository.save(pacienteExistente);
    }

    // --- Borrado lógico (soft delete) ---
    @Transactional
    public void desactivarPaciente(Long id) {
        if (!pacienteRepository.existsById(id)) {
            throw new PacienteNotFoundException(id);
        }
        pacienteRepository.desactivarPaciente(id);
    }
}
