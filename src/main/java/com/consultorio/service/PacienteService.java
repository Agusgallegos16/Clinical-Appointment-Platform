package com.consultorio.service;

import com.consultorio.domain.Paciente;
import com.consultorio.domain.Usuario;
import com.consultorio.dto.PacienteMenorResponseDTO;
import com.consultorio.dto.RegistroMenorDTO;
import com.consultorio.dto.RegistroPacienteDTO;
import com.consultorio.repository.PacienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PacienteService {

    private final PacienteRepository pacienteRepository;
    private final UsuarioService usuarioService;
    private final EmailService emailService;

    @Autowired
    public PacienteService(PacienteRepository pacienteRepository,
                           UsuarioService usuarioService,
                           EmailService emailService) {
        this.pacienteRepository = pacienteRepository;
        this.usuarioService = usuarioService;
        this.emailService = emailService;
    }

    @Transactional
    public Paciente registrarPaciente(RegistroPacienteDTO dto) {
        if (dto.getConfirmarPassword() != null && !dto.getPassword().equals(dto.getConfirmarPassword())) {
            throw new IllegalArgumentException("Las contraseñas no coinciden. Por favor verifíquelas.");
        }
        if (usuarioService.existePorEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Ya existe un usuario registrado con el email: " + dto.getEmail());
        }

        java.util.Optional<Paciente> pacienteExistenteOpt = pacienteRepository.findByDni(dto.getDni());
        if (pacienteExistenteOpt.isPresent()) {
            Paciente existente = pacienteExistenteOpt.get();
            if (existente.getUsuario() != null) {
                throw new IllegalArgumentException("Ya existe un paciente registrado con el DNI: " + dto.getDni());
            }
        }

        String tokenVerificacion = UUID.randomUUID().toString();
        Usuario usuario = usuarioService.crearUsuarioParaPaciente(dto.getEmail(), dto.getPassword(), tokenVerificacion);

        Paciente paciente;
        if (pacienteExistenteOpt.isPresent()) {
            paciente = pacienteExistenteOpt.get();
            paciente.setUsuario(usuario);
            paciente.setNombre(dto.getNombre());
            paciente.setApellido(dto.getApellido());
            paciente.setTelefono(dto.getTelefono());
            if (dto.getFechaNacimiento() != null) {
                paciente.setFechaNacimiento(dto.getFechaNacimiento());
            }
            paciente.setTutor(null);
        } else {
            paciente = Paciente.builder()
                    .usuario(usuario)
                    .nombre(dto.getNombre())
                    .apellido(dto.getApellido())
                    .dni(dto.getDni())
                    .telefono(dto.getTelefono())
                    .fechaNacimiento(dto.getFechaNacimiento())
                    .build();
        }

        Paciente guardado = pacienteRepository.save(paciente);
        emailService.enviarEmailVerificacion(guardado.getUsuario().getEmail(), guardado.getNombre(), tokenVerificacion);

        return guardado;
    }

    @Transactional
    public boolean confirmarEmail(String token) {
        Usuario usuario = usuarioService.activarCuentaPaciente(token);
        pacienteRepository.findByUsuarioEmail(usuario.getEmail())
                .ifPresent(p -> emailService.enviarEmailBienvenida(usuario.getEmail(), p.getNombre()));
        return true;
    }

    public Paciente obtenerPorId(UUID id) {
        return pacienteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Paciente no encontrado con id: " + id));
    }

    public Paciente obtenerPorUsuarioEmail(String email) {
        return pacienteRepository.findByUsuarioEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Paciente no encontrado con email: " + email));
    }

    public Optional<Paciente> obtenerPorUsuarioId(Long usuarioId) {
        return pacienteRepository.findByUsuarioId(usuarioId);
    }

    @org.springframework.context.event.EventListener
    @Transactional
    public void alEliminarUsuario(com.consultorio.event.UsuarioEliminadoEvent event) {
        pacienteRepository.findByUsuarioId(event.usuarioId())
                .ifPresent(pacienteRepository::delete);
    }

    @Transactional
    public void eliminarPaciente(Paciente paciente) {
        if (paciente != null) {
            pacienteRepository.delete(paciente);
        }
    }

    @Transactional
    public Paciente vincularOCrearPacienteParaUsuario(Usuario usuario, Long dni, String nombre, String apellido, String telefono, java.time.LocalDate fechaNacimiento) {
        Paciente pacienteExistente = null;
        if (dni != null) {
            pacienteExistente = pacienteRepository.findByDni(dni).orElse(null);
        }
        if (pacienteExistente != null) {
            pacienteExistente.setUsuario(usuario);
            pacienteExistente.setNombre(nombre != null ? nombre.trim() : "");
            pacienteExistente.setApellido(apellido != null ? apellido.trim() : "");
            if (telefono != null) pacienteExistente.setTelefono(telefono.trim());
            if (fechaNacimiento != null) pacienteExistente.setFechaNacimiento(fechaNacimiento);
            return pacienteRepository.save(pacienteExistente);
        } else {
            Paciente nuevoPaciente = Paciente.builder()
                    .usuario(usuario)
                    .dni(dni)
                    .nombre(nombre != null ? nombre.trim() : "")
                    .apellido(apellido != null ? apellido.trim() : "")
                    .telefono(telefono != null ? telefono.trim() : null)
                    .fechaNacimiento(fechaNacimiento)
                    .build();
            return pacienteRepository.save(nuevoPaciente);
        }
    }

    @Transactional(readOnly = true)
    public Optional<Paciente> obtenerPorDniOpt(Long dni) {
        if (dni == null)
            return Optional.empty();
        return pacienteRepository.findByDni(dni);
    }

    public List<Paciente> listarTodos() {
        return pacienteRepository.findAll();
    }

    @Transactional
    public Paciente obtenerORegistrarPacienteExpress(String nombre, String apellido, Long dni, String telefono, String email, java.time.LocalDate fechaNacimiento) {
        Optional<Paciente> pacienteExistenteOpt = pacienteRepository.findByDni(dni);
        if (pacienteExistenteOpt.isPresent()) {
            return pacienteExistenteOpt.get();
        }
        Paciente nuevo = Paciente.builder()
                .nombre(nombre != null ? nombre.trim() : "")
                .apellido(apellido != null ? apellido.trim() : "")
                .dni(dni)
                .telefono(telefono != null && !telefono.trim().isEmpty() ? telefono.trim() : null)
                .email(email != null && !email.trim().isEmpty() ? email.trim() : null)
                .fechaNacimiento(fechaNacimiento)
                .usuario(null)
                .build();
        return pacienteRepository.save(nuevo);
    }


    @Transactional
    public PacienteMenorResponseDTO registrarMenor(UUID tutorPacienteId,
                                                   RegistroMenorDTO dto) {
        Paciente tutor = pacienteRepository.findById(tutorPacienteId)
                .orElseThrow(
                        () -> new IllegalArgumentException("Paciente tutor no encontrado con id: " + tutorPacienteId));

        if (dto.getFechaNacimiento() != null) {
            int edadCalculada = java.time.Period.between(dto.getFechaNacimiento(), java.time.LocalDate.now())
                    .getYears();
            if (edadCalculada >= 18) {
                throw new IllegalArgumentException(
                        "No es posible vincular a una persona mayor o igual a 18 años como menor a cargo.");
            }
        }

        java.util.Optional<Paciente> pacienteExistenteOpt = pacienteRepository.findByDni(dto.getDni());
        Paciente menor;

        if (pacienteExistenteOpt.isPresent()) {
            Paciente existente = pacienteExistenteOpt.get();

            // Si ya posee una cuenta propia independiente con Usuario, no se puede vincular
            // como menor sin cuenta
            if (existente.getUsuario() != null) {
                throw new IllegalArgumentException(
                        "El DNI ingresado pertenece a un paciente registrado con cuenta propia independiente.");
            }

            // Si ya está vinculado a este mismo tutor
            if (existente.getTutor() != null && existente.getTutor().getId().equals(tutorPacienteId)) {
                throw new IllegalArgumentException("El menor ya se encuentra vinculado a tu cuenta.");
            }

            // Si está vinculado a otro tutor activo (menor de 18 años)
            if (existente.getTutor() != null && (existente.getEdad() == null || existente.getEdad() < 18)) {
                throw new IllegalArgumentException("El menor ya se encuentra vinculado a otro tutor a cargo.");
            }

            // Revincular o actualizar los datos del menor existente que estaba desvinculado
            menor = existente;
            menor.setTutor(tutor);
            menor.setNombre(dto.getNombre());
            menor.setApellido(dto.getApellido());
            if (dto.getFechaNacimiento() != null) {
                menor.setFechaNacimiento(dto.getFechaNacimiento());
            }
        } else {
            menor = Paciente.builder()
                    .tutor(tutor)
                    .nombre(dto.getNombre())
                    .apellido(dto.getApellido())
                    .dni(dto.getDni())
                    .fechaNacimiento(dto.getFechaNacimiento())
                    .usuario(null) // Menor a cargo no posee credenciales de acceso
                    .build();
        }

        Paciente guardado = pacienteRepository.save(menor);
        return mapearMenorResponseDTO(guardado);
    }

    @Transactional
    public List<PacienteMenorResponseDTO> listarMenoresDeTutor(UUID tutorPacienteId) {
        List<Paciente> menores = pacienteRepository.findByTutorId(tutorPacienteId);

        List<Paciente> validos = new java.util.ArrayList<>();
        for (Paciente m : menores) {
            if (m.getEdad() != null && m.getEdad() >= 18) {
                // Desvinculación automática por mayoría de edad (>= 18 años)
                m.setTutor(null);
                pacienteRepository.save(m);
            } else {
                validos.add(m);
            }
        }

        return validos.stream()
                .map(this::mapearMenorResponseDTO)
                .collect(java.util.stream.Collectors.toList());
    }

    @Transactional
    public boolean desvincularMenor(UUID tutorPacienteId, UUID menorId) {
        Paciente menor = pacienteRepository.findById(menorId)
                .orElseThrow(() -> new IllegalArgumentException("Paciente menor no encontrado con id: " + menorId));

        if (menor.getTutor() == null || !menor.getTutor().getId().equals(tutorPacienteId)) {
            throw new IllegalArgumentException("El menor seleccionado no pertenece a la tutela de este paciente.");
        }

        menor.setTutor(null);
        pacienteRepository.save(menor);
        return true;
    }

    @org.springframework.scheduling.annotation.Scheduled(cron = "0 0 1 * * ?")
    @Transactional
    public void desvincularMenoresMayoresDeEdad() {
        List<Paciente> vinculados = pacienteRepository.findByTutorIsNotNull();
        for (Paciente p : vinculados) {
            if (p.getEdad() != null && p.getEdad() >= 18) {
                p.setTutor(null);
                pacienteRepository.save(p);
            }
        }
    }

    private com.consultorio.dto.PacienteMenorResponseDTO mapearMenorResponseDTO(Paciente menor) {
        return com.consultorio.dto.PacienteMenorResponseDTO.builder()
                .id(menor.getId())
                .nombre(menor.getNombre())
                .apellido(menor.getApellido())
                .dni(menor.getDni())
                .fechaNacimiento(menor.getFechaNacimiento())
                .edad(menor.getEdad())
                .telefono(menor.getTelefono()) // Hereda teléfono del tutor
                .tutorId(menor.getTutor() != null ? menor.getTutor().getId() : null)
                .tutorNombre(
                        menor.getTutor() != null ? menor.getTutor().getNombre() + " " + menor.getTutor().getApellido()
                                : null)
                .build();
    }

    @Transactional
    public Paciente actualizarPerfilPaciente(UUID id, com.consultorio.dto.ActualizarPerfilPacienteDTO dto) {
        Paciente paciente = pacienteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Paciente no encontrado con id: " + id));

        paciente.setNombre(dto.getNombre().trim());
        paciente.setApellido(dto.getApellido().trim());
        if (dto.getTelefono() != null) {
            paciente.setTelefono(dto.getTelefono().trim());
        }

        Usuario usuario = paciente.getUsuario();
        if (usuario != null) {
            usuarioService.actualizarCredenciales(usuario.getId(), dto.getEmail(), dto.getPassword());
        }

        return pacienteRepository.save(paciente);
    }
}
