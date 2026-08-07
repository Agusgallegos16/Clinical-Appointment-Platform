package com.consultorio.controller;

import com.consultorio.domain.Doctor;
import com.consultorio.domain.Paciente;
import com.consultorio.dto.RegistroDoctorDTO;
import com.consultorio.dto.RegistroPacienteDTO;
import com.consultorio.service.DoctorService;
import com.consultorio.service.PacienteService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final PacienteService pacienteService;
    private final DoctorService doctorService;

    @Autowired
    public AuthController(PacienteService pacienteService, DoctorService doctorService) {
        this.pacienteService = pacienteService;
        this.doctorService = doctorService;
    }

    @PostMapping("/registro-paciente")
    public ResponseEntity<Paciente> registrarPaciente(@Valid @RequestBody RegistroPacienteDTO dto) {
        Paciente paciente = pacienteService.registrarPaciente(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(paciente);
    }

    @PostMapping("/registro-doctor")
    public ResponseEntity<Doctor> registrarDoctor(@Valid @RequestBody RegistroDoctorDTO dto) {
        Doctor doctor = doctorService.registrarDoctor(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(doctor);
    }
}
