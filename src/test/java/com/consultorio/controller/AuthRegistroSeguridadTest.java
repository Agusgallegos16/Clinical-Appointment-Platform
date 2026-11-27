package com.consultorio.controller;

import com.consultorio.domain.AuthProvider;
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

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthRegistroSeguridadTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PacienteRepository pacienteRepository;

    @Test
    @DisplayName("CRIT-02: POST /api/auth/registro-paciente debe retornar DTO sin filtrar password ni tokenVerificacionEmail")
    void registroPacienteNoDebeExponerSecretosDeUsuarioEnRespuestaJson() throws Exception {
        String payload = """
                {
                  "nombre": "Lucia",
                  "apellido": "Fernandez",
                  "email": "lucia.seguridad@test.com",
                  "password": "PasswordSegura123!",
                  "confirmarPassword": "PasswordSegura123!",
                  "dni": 45123456,
                  "telefono": "+54 11 5555-1234",
                  "fechaNacimiento": "1995-04-15"
                }
                """;

        mockMvc.perform(post("/api/auth/registro-paciente")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.dni").value(45123456))
                .andExpect(jsonPath("$.usuario.email").value("lucia.seguridad@test.com"))
                .andExpect(jsonPath("$.usuario.password").doesNotExist())
                .andExpect(jsonPath("$.usuario.tokenVerificacionEmail").doesNotExist())
                .andExpect(jsonPath("$.usuario.googleRefreshToken").doesNotExist())
                .andExpect(jsonPath("$.usuario.nuevaPasswordPendiente").doesNotExist());
    }

    @Test
    @DisplayName("CRIT-02: POST /api/auth/registro-paciente no debe apropiarse ni alterar fichas de menores o pacientes express por DNI")
    void registroPacienteNoDebeApropiarseDeMenorNiFichaExpressPorDni() throws Exception {
        Usuario tutorUsuario = usuarioRepository.save(Usuario.builder()
                .email("tutor.padre@test.com")
                .password("$2a$10$abcdefghijklmnopqrstuv1234567890123456789012")
                .rol(Rol.PACIENTE)
                .activo(true)
                .emailVerificado(true)
                .authProvider(AuthProvider.LOCAL)
                .build());

        Paciente tutor = pacienteRepository.save(Paciente.builder()
                .usuario(tutorUsuario)
                .nombre("Roberto")
                .apellido("Gomez")
                .dni(30999888L)
                .telefono("+54 11 4444-0001")
                .fechaNacimiento(LocalDate.of(1985, 3, 20))
                .build());

        Paciente menor = pacienteRepository.save(Paciente.builder()
                .usuario(null)
                .tutor(tutor)
                .nombre("Mateo")
                .apellido("Gomez")
                .dni(52999888L)
                .fechaNacimiento(LocalDate.of(2016, 5, 10))
                .build());

        Paciente fichaExpress = pacienteRepository.save(Paciente.builder()
                .usuario(null)
                .tutor(null)
                .nombre("Laura")
                .apellido("Perez")
                .dni(41999888L)
                .telefono("+54 11 4444-0002")
                .fechaNacimiento(LocalDate.of(1992, 8, 12))
                .build());

        String intentoSecuestroMenor = """
                {
                  "nombre": "Atacante",
                  "apellido": "Externo",
                  "email": "atacante.menor@test.com",
                  "password": "Password123!",
                  "confirmarPassword": "Password123!",
                  "dni": 52999888,
                  "telefono": "+54 11 9999-9999",
                  "fechaNacimiento": "1990-01-01"
                }
                """;

        mockMvc.perform(post("/api/auth/registro-paciente")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(intentoSecuestroMenor))
                .andExpect(status().isBadRequest());

        Paciente menorRecargado = pacienteRepository.findById(menor.getId()).orElseThrow();
        assertNotNull(menorRecargado.getTutor(), "El menor no debe perder el vínculo con su tutor");
        assertEquals(tutor.getId(), menorRecargado.getTutor().getId());
        assertNull(menorRecargado.getUsuario(), "El menor no debe quedar vinculado a una cuenta externa");
        assertEquals("Mateo", menorRecargado.getNombre(), "El nombre del menor no debe sobrescribirse");

        String intentoSecuestroExpress = """
                {
                  "nombre": "Atacante",
                  "apellido": "Dos",
                  "email": "atacante.express@test.com",
                  "password": "Password123!",
                  "confirmarPassword": "Password123!",
                  "dni": 41999888,
                  "telefono": "+54 11 8888-8888",
                  "fechaNacimiento": "1990-01-01"
                }
                """;

        mockMvc.perform(post("/api/auth/registro-paciente")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(intentoSecuestroExpress))
                .andExpect(status().isBadRequest());

        Paciente expressRecargado = pacienteRepository.findById(fichaExpress.getId()).orElseThrow();
        assertNull(expressRecargado.getUsuario(), "La ficha express no debe vincularse sin validación de identidad");
        assertEquals("Laura", expressRecargado.getNombre(), "Los datos de la ficha express no deben sobrescribirse");
    }
}
