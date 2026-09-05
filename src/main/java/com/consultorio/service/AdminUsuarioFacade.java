package com.consultorio.service;

import com.consultorio.domain.Doctor;
import com.consultorio.domain.Paciente;
import com.consultorio.domain.Rol;
import com.consultorio.domain.Usuario;
import com.consultorio.dto.RegistroUsuarioAdminDTO;
import com.consultorio.dto.UsuarioAdminDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class AdminUsuarioFacade {

    private final UsuarioService usuarioService;
    private final DoctorService doctorService;
    private final PacienteService pacienteService;
    private final EmailService emailService;

    @Autowired
    public AdminUsuarioFacade(UsuarioService usuarioService,
                              DoctorService doctorService,
                              PacienteService pacienteService,
                              EmailService emailService) {
        this.usuarioService = usuarioService;
        this.doctorService = doctorService;
        this.pacienteService = pacienteService;
        this.emailService = emailService;
    }

    public Page<UsuarioAdminDTO> buscarUsuariosPaginados(String query, Pageable pageable) {
        Page<Usuario> paginaUsuarios = usuarioService.buscarUsuariosPaginadosEntidad(query, pageable);
        return paginaUsuarios.map(this::mapToUsuarioAdminDTO);
    }

    private UsuarioAdminDTO mapToUsuarioAdminDTO(Usuario u) {
        String nombre = null;
        String apellido = null;

        if (u.getRol() == Rol.PACIENTE || u.getRol() == Rol.SECRETARIA) {
            Optional<Paciente> p = pacienteService.obtenerPorUsuarioId(u.getId());
            if (p.isPresent()) {
                nombre = p.get().getNombre();
                apellido = p.get().getApellido();
            }
        } else if (u.getRol() == Rol.DOCTOR) {
            Optional<Doctor> d = doctorService.obtenerPorUsuarioId(u.getId());
            if (d.isPresent()) {
                nombre = d.get().getNombre();
                apellido = d.get().getApellido();
            }
        }

        return UsuarioAdminDTO.builder()
                .id(u.getId())
                .email(u.getEmail())
                .rol(u.getRol())
                .activo(u.isActivo())
                .bloqueado(u.isBloqueado())
                .emailVerificado(u.isEmailVerificado())
                .nombre(nombre)
                .apellido(apellido)
                .build();
    }

    @Transactional
    public UsuarioAdminDTO registrarUsuarioPorAdmin(RegistroUsuarioAdminDTO dto) {
        if (dto.getRol() == null) {
            throw new IllegalArgumentException("El rol es obligatorio.");
        }

        String emailLimpio = dto.getEmail() != null ? dto.getEmail().trim().toLowerCase() : "";
        if (emailLimpio.isBlank()) {
            throw new IllegalArgumentException("El correo electrónico es obligatorio.");
        }

        var usuarioExistenteOpt = usuarioService.buscarPorEmailIgnoreCase(emailLimpio);
        if (usuarioExistenteOpt.isPresent()) {
            Usuario exist = usuarioExistenteOpt.get();
            if (!exist.isActivo() && !exist.isEmailVerificado() && exist.getTokenVerificacionExpiracion() != null && exist.getTokenVerificacionExpiracion().isBefore(LocalDateTime.now())) {
                doctorService.obtenerPorUsuarioId(exist.getId()).ifPresent(doctorService::eliminarDoctor);
                pacienteService.obtenerPorUsuarioId(exist.getId()).ifPresent(pacienteService::eliminarPaciente);
                usuarioService.eliminarEntidadDirecta(exist);
            } else {
                throw new IllegalArgumentException("El correo electrónico " + dto.getEmail() + " ya se encuentra registrado por otro usuario.");
            }
        }

        // Si se especificó DNI, verificar que no pertenezca a otra cuenta activa
        if (dto.getDni() != null) {
            Optional<Paciente> pacienteExistenteDni = pacienteService.obtenerPorDniOpt(dto.getDni());
            if (pacienteExistenteDni.isPresent() && pacienteExistenteDni.get().getUsuario() != null) {
                Usuario userDni = pacienteExistenteDni.get().getUsuario();
                if (!userDni.getEmail().equalsIgnoreCase(emailLimpio)) {
                    throw new IllegalArgumentException("El DNI " + dto.getDni() + " ya pertenece a otra cuenta registrada (" + userDni.getEmail() + ").");
                }
            }
        }

        String tokenActivacion = UUID.randomUUID().toString();
        Usuario usuarioGuardado = usuarioService.crearUsuarioParaAdmin(emailLimpio, dto.getRol(), tokenActivacion);

        if (dto.getRol() == Rol.DOCTOR) {
            doctorService.crearDoctorParaUsuario(usuarioGuardado, dto.getNombre(), dto.getApellido(), dto.getFotoUrl(), dto.getEspecialidadIds());
        } else if (dto.getRol() == Rol.PACIENTE || dto.getRol() == Rol.SECRETARIA) {
            pacienteService.vincularOCrearPacienteParaUsuario(usuarioGuardado, dto.getDni(), dto.getNombre(), dto.getApellido(), dto.getTelefono(), dto.getFechaNacimiento());
        }

        emailService.enviarEmailActivacionDoctor(usuarioGuardado.getEmail(), dto.getNombre(), tokenActivacion);

        return UsuarioAdminDTO.builder()
                .id(usuarioGuardado.getId())
                .email(usuarioGuardado.getEmail())
                .rol(usuarioGuardado.getRol())
                .activo(usuarioGuardado.isActivo())
                .bloqueado(usuarioGuardado.isBloqueado())
                .emailVerificado(usuarioGuardado.isEmailVerificado())
                .nombre(dto.getNombre().trim())
                .apellido(dto.getApellido().trim())
                .build();
    }
}
