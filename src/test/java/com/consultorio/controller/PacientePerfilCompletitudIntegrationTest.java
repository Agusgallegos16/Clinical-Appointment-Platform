package com.consultorio.controller;

import com.consultorio.domain.Paciente;
import com.consultorio.domain.Rol;
import com.consultorio.domain.Usuario;
import com.consultorio.dto.CompletarPerfilPacienteDTO;
import com.consultorio.repository.PacienteRepository;
import com.consultorio.repository.UsuarioRepository;
import com.consultorio.security.JwtUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PacientePerfilCompletitudIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PacienteRepository pacienteRepository;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /api/pacientes/mi-perfil/estado-completitud y PATCH /api/pacientes/mi-perfil/completar flujo exitoso")
    void testFlujoCompletarPerfilPaciente() throws Exception {
        String email = "google_user_" + UUID.randomUUID() + "@test.com";

        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .email(email)
                .password("encodedRandomPassword")
                .rol(Rol.PACIENTE)
                .activo(true)
                .emailVerificado(true)
                .bloqueado(false)
                .build());

        Paciente paciente = pacienteRepository.save(Paciente.builder()
                .usuario(usuario)
                .nombre("Martin")
                .apellido("Palermo")
                .email(email)
                .dni(null)
                .telefono(null)
                .fechaNacimiento(null)
                .build());

        String token = jwtUtils.generarToken(email, Rol.PACIENTE.name());

        // 1. Verificar estado inicial: incompleto
        mockMvc.perform(get("/api/pacientes/mi-perfil/estado-completitud")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perfilCompleto").value(false))
                .andExpect(jsonPath("$.camposFaltantes").isArray())
                .andExpect(jsonPath("$.nombre").value("Martin"))
                .andExpect(jsonPath("$.apellido").value("Palermo"));

        // 2. Completar perfil vía PATCH
        CompletarPerfilPacienteDTO dto = CompletarPerfilPacienteDTO.builder()
                .dni(33445566L)
                .telefono("+54 11 9988-7766")
                .fechaNacimiento(LocalDate.of(1985, 5, 20))
                .build();

        mockMvc.perform(patch("/api/pacientes/mi-perfil/completar")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dni").value(33445566L))
                .andExpect(jsonPath("$.telefono").value("+54 11 9988-7766"))
                .andExpect(jsonPath("$.fechaNacimiento").value("1985-05-20"));

        // 3. Verificar estado posterior: completo
        mockMvc.perform(get("/api/pacientes/mi-perfil/estado-completitud")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perfilCompleto").value(true))
                .andExpect(jsonPath("$.camposFaltantes").isEmpty());
    }

    @Test
    @DisplayName("PATCH /api/pacientes/mi-perfil/completar con DNI duplicado de otro paciente retorna 400 Bad Request")
    void testCompletarPerfilConDniDuplicadoRetorna400() throws Exception {
        Long dniExistente = 20111222L;

        // Paciente existente con ese DNI
        Usuario usuarioA = usuarioRepository.save(Usuario.builder()
                .email("pacienteA_" + UUID.randomUUID() + "@test.com")
                .password("passA")
                .rol(Rol.PACIENTE)
                .activo(true)
                .emailVerificado(true)
                .build());

        pacienteRepository.save(Paciente.builder()
                .usuario(usuarioA)
                .nombre("Juan")
                .apellido("Perez")
                .dni(dniExistente)
                .build());

        // Nuevo usuario de Google
        String emailB = "google_b_" + UUID.randomUUID() + "@test.com";
        Usuario usuarioB = usuarioRepository.save(Usuario.builder()
                .email(emailB)
                .password("passB")
                .rol(Rol.PACIENTE)
                .activo(true)
                .emailVerificado(true)
                .build());

        pacienteRepository.save(Paciente.builder()
                .usuario(usuarioB)
                .nombre("Esteban")
                .apellido("Quito")
                .email(emailB)
                .dni(null)
                .build());

        String tokenB = jwtUtils.generarToken(emailB, Rol.PACIENTE.name());

        CompletarPerfilPacienteDTO dto = CompletarPerfilPacienteDTO.builder()
                .dni(dniExistente) // Intentar usar el DNI del Paciente A
                .telefono("+54 11 1234-5678")
                .fechaNacimiento(LocalDate.of(1992, 10, 10))
                .build();

        mockMvc.perform(patch("/api/pacientes/mi-perfil/completar")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("ya se encuentra registrado")));
    }
}
