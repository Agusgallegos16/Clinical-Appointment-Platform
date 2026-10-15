package com.consultorio.controller;

import com.consultorio.domain.Paciente;
import com.consultorio.domain.Rol;
import com.consultorio.domain.Usuario;
import com.consultorio.repository.PacienteRepository;
import com.consultorio.repository.UsuarioRepository;
import com.consultorio.security.JwtUtils;
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
class AuthControllerMeEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PacienteRepository pacienteRepository;

    @Autowired
    private JwtUtils jwtUtils;

    @Test
    @DisplayName("GET /api/auth/me con Token JWT Bearer válido retorna 200 y detalles del usuario")
    void testObtenerUsuarioActualConTokenValidoRetorna200() throws Exception {
        String email = "me_test_" + UUID.randomUUID() + "@test.com";

        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .email(email)
                .password("hashPassword123")
                .rol(Rol.PACIENTE)
                .activo(true)
                .emailVerificado(true)
                .bloqueado(false)
                .build());

        Paciente paciente = pacienteRepository.save(Paciente.builder()
                .usuario(usuario)
                .nombre("Carlos")
                .apellido("Gomez")
                .email(email)
                .dni(99887766L)
                .build());

        String token = jwtUtils.generarToken(email, Rol.PACIENTE.name());

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(usuario.getId()))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.rol").value("PACIENTE"))
                .andExpect(jsonPath("$.entidadId").value(paciente.getId().toString()))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    @DisplayName("GET /api/auth/me sin token Authorization retorna 401 Unauthorized")
    void testObtenerUsuarioActualSinTokenRetorna401() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}
