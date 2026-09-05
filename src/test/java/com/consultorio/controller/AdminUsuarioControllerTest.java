package com.consultorio.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminUsuarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "admin@test.com", roles = {"ADMIN"})
    @DisplayName("GET /api/admin/usuarios con rol ADMIN debe retornar HTTP 200 OK y estructura paginada")
    void testBuscarUsuariosPaginados() throws Exception {
        mockMvc.perform(get("/api/admin/usuarios")
                .param("page", "0")
                .param("size", "15")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.size").value(15))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.totalElements").exists());
    }

    @Test
    @WithMockUser(username = "paciente@test.com", roles = {"PACIENTE"})
    @DisplayName("GET /api/admin/usuarios con rol PACIENTE debe retornar HTTP 403 Forbidden")
    void testBuscarUsuariosSinPermiso() throws Exception {
        mockMvc.perform(get("/api/admin/usuarios")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = {"ADMIN"})
    @DisplayName("GET /api/admin/usuarios?query=Agustín debe filtrar por nombre exitosamente")
    void testBuscarUsuariosPorNombre() throws Exception {
        mockMvc.perform(get("/api/admin/usuarios")
                .param("query", "Agustín")
                .param("page", "0")
                .param("size", "15")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.content[0].nombre").value("Agustín"));
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = {"ADMIN"})
    @DisplayName("GET /api/admin/usuarios?query=Morales debe filtrar por apellido exitosamente")
    void testBuscarUsuariosPorApellido() throws Exception {
        mockMvc.perform(get("/api/admin/usuarios")
                .param("query", "Morales")
                .param("page", "0")
                .param("size", "15")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.content[0].apellido").value("Morales"));
    }
}
