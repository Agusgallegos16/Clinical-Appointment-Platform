package com.consultorio.security;

import com.consultorio.domain.Paciente;
import com.consultorio.domain.Rol;
import com.consultorio.domain.Usuario;
import com.consultorio.repository.PacienteRepository;
import com.consultorio.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class JwtCuentaBloqueadaSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PacienteRepository pacienteRepository;

    @Autowired
    private JwtUtils jwtUtils;

    @Test
    @DisplayName("Petición con token JWT válido pero cuenta bloqueada debe retornar HTTP 403 Forbidden")
    void testCuentaBloqueadaConTokenValidoRetorna403() throws Exception {
        String email = "bloqueado_" + UUID.randomUUID() + "@test.com";
        Usuario usuarioBloqueado = usuarioRepository.save(Usuario.builder()
                .email(email)
                .password("password123")
                .rol(Rol.PACIENTE)
                .activo(true)
                .bloqueado(true)
                .emailVerificado(true)
                .build());

        pacienteRepository.save(Paciente.builder()
                .usuario(usuarioBloqueado)
                .nombre("Bloqueado")
                .apellido("Test")
                .dni(40111000L + (long) (Math.random() * 90000))
                .build());

        String token = jwtUtils.generarToken(email, "PACIENTE");

        mockMvc.perform(get("/api/pacientes/mi-perfil")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.mensaje").value("La cuenta de usuario se encuentra temporalmente bloqueada."));
    }

    @Test
    @DisplayName("Petición con token JWT válido pero cuenta inactiva/desactivada debe retornar HTTP 403 Forbidden")
    void testCuentaInactivaConTokenValidoRetorna403() throws Exception {
        String email = "inactivo_" + UUID.randomUUID() + "@test.com";
        Usuario usuarioInactivo = usuarioRepository.save(Usuario.builder()
                .email(email)
                .password("password123")
                .rol(Rol.PACIENTE)
                .activo(false)
                .bloqueado(false)
                .emailVerificado(true)
                .build());

        pacienteRepository.save(Paciente.builder()
                .usuario(usuarioInactivo)
                .nombre("Inactivo")
                .apellido("Test")
                .dni(40333000L + (long) (Math.random() * 90000))
                .build());

        String token = jwtUtils.generarToken(email, "PACIENTE");

        mockMvc.perform(get("/api/pacientes/mi-perfil")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.mensaje").value("La cuenta de usuario está desactivada o inhabilitada."));
    }

    @Test
    @DisplayName("Petición con token JWT válido y cuenta activa no bloqueada debe retornar HTTP 200 OK")
    void testCuentaActivaConTokenValidoRetorna200() throws Exception {
        String email = "valido_" + UUID.randomUUID() + "@test.com";
        Usuario usuarioValido = usuarioRepository.save(Usuario.builder()
                .email(email)
                .password("password123")
                .rol(Rol.PACIENTE)
                .activo(true)
                .bloqueado(false)
                .emailVerificado(true)
                .build());

        pacienteRepository.save(Paciente.builder()
                .usuario(usuarioValido)
                .nombre("Valido")
                .apellido("Test")
                .dni(40555000L + (long) (Math.random() * 90000))
                .build());

        String token = jwtUtils.generarToken(email, "PACIENTE");

        mockMvc.perform(get("/api/pacientes/mi-perfil")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Valido"));
    }
}
