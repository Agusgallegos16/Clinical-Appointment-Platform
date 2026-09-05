package com.consultorio.service;

import com.consultorio.domain.Doctor;
import com.consultorio.domain.Paciente;
import com.consultorio.domain.Rol;
import com.consultorio.domain.Usuario;
import com.consultorio.dto.RegistroUsuarioAdminDTO;
import com.consultorio.dto.UsuarioAdminDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminUsuarioFacadeTest {

    @Mock
    private UsuarioService usuarioService;

    @Mock
    private DoctorService doctorService;

    @Mock
    private PacienteService pacienteService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AdminUsuarioFacade adminUsuarioFacade;

    private Usuario usuarioDoctor;
    private Usuario usuarioPaciente;

    @BeforeEach
    void setUp() {
        usuarioDoctor = Usuario.builder()
                .id(1L)
                .email("doc@test.com")
                .rol(Rol.DOCTOR)
                .activo(true)
                .build();

        usuarioPaciente = Usuario.builder()
                .id(2L)
                .email("pac@test.com")
                .rol(Rol.PACIENTE)
                .activo(true)
                .build();
    }

    @Test
    @DisplayName("buscarUsuariosPaginados debe enriquecer nombres y apellidos desde servicios correspondientes")
    void testBuscarUsuariosPaginados() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Usuario> page = new PageImpl<>(List.of(usuarioDoctor, usuarioPaciente));

        when(usuarioService.buscarUsuariosPaginadosEntidad("test", pageable)).thenReturn(page);

        Doctor doctor = Doctor.builder().id(UUID.randomUUID()).nombre("Gregory").apellido("House").build();
        when(doctorService.obtenerPorUsuarioId(1L)).thenReturn(Optional.of(doctor));

        Paciente paciente = Paciente.builder().id(UUID.randomUUID()).nombre("John").apellido("Doe").build();
        when(pacienteService.obtenerPorUsuarioId(2L)).thenReturn(Optional.of(paciente));

        Page<UsuarioAdminDTO> result = adminUsuarioFacade.buscarUsuariosPaginados("test", pageable);

        assertNotNull(result);
        assertEquals(2, result.getContent().size());

        UsuarioAdminDTO docDto = result.getContent().get(0);
        assertEquals("doc@test.com", docDto.getEmail());
        assertEquals("Gregory", docDto.getNombre());
        assertEquals("House", docDto.getApellido());

        UsuarioAdminDTO pacDto = result.getContent().get(1);
        assertEquals("pac@test.com", pacDto.getEmail());
        assertEquals("John", pacDto.getNombre());
        assertEquals("Doe", pacDto.getApellido());
    }

    @Test
    @DisplayName("registrarUsuarioPorAdmin con rol DOCTOR delega en doctorService y envía email")
    void testRegistrarDoctor() {
        RegistroUsuarioAdminDTO dto = new RegistroUsuarioAdminDTO();
        dto.setEmail("newdoc@test.com");
        dto.setNombre("Lisa");
        dto.setApellido("Cuddy");
        dto.setRol(Rol.DOCTOR);

        when(usuarioService.buscarPorEmailIgnoreCase("newdoc@test.com")).thenReturn(Optional.empty());

        Usuario userSaved = Usuario.builder()
                .id(10L)
                .email("newdoc@test.com")
                .rol(Rol.DOCTOR)
                .activo(true)
                .build();
        when(usuarioService.crearUsuarioParaAdmin(eq("newdoc@test.com"), eq(Rol.DOCTOR), any())).thenReturn(userSaved);

        UsuarioAdminDTO res = adminUsuarioFacade.registrarUsuarioPorAdmin(dto);

        assertNotNull(res);
        assertEquals("newdoc@test.com", res.getEmail());
        assertEquals("Lisa", res.getNombre());
        assertEquals("Cuddy", res.getApellido());
        verify(doctorService).crearDoctorParaUsuario(eq(userSaved), eq("Lisa"), eq("Cuddy"), any(), any());
        verify(emailService).enviarEmailActivacionDoctor(eq("newdoc@test.com"), eq("Lisa"), any());
    }
}
