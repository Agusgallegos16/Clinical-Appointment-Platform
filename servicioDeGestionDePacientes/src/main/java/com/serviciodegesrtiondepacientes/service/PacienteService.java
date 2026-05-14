package com.serviciodegesrtiondepacientes.service;

import com.serviciodegesrtiondepacientes.domain.pacientes.Paciente;
import com.serviciodegesrtiondepacientes.repository.PacienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PacienteService {

    private final PacienteRepository pacienteRepository;

    @Autowired
    public PacienteService(PacienteRepository pacienteRepository) {
        this.pacienteRepository = pacienteRepository;
    }

    public List<Paciente> obtenerTodosLosPacientes() {
        return pacienteRepository.findAll();
    }

    public Optional<Paciente> obtenerPacientePorId(Long id) {
        return pacienteRepository.findById(id);
    }

    public Paciente guardarPaciente(Paciente paciente) {
        return pacienteRepository.save(paciente);
    }

    public void eliminarPacientePorId(Long id) {
        pacienteRepository.deleteById(id);
    }

    public Paciente actualizarPaciente(Long id, Paciente paciente) {
        return pacienteRepository.findById(id).map(p -> {
            p.setNombre(paciente.getNombre());
            p.setApellido(paciente.getApellido());
            p.setDni(paciente.getDni());
            p.setSexo(paciente.getSexo());
            p.setMail(paciente.getMail());
            p.setNumeroDeTelefono(paciente.getNumeroDeTelefono());
            p.setTipoDeSangre(paciente.getTipoDeSangre());
            p.setObraSocial(paciente.getObraSocial());
            p.setNumeroObraSocial(paciente.getNumeroObraSocial());
            return pacienteRepository.save(p);
        }).orElseThrow(() -> new RuntimeException("Paciente no encontrado con id: " + id));
    }
}
