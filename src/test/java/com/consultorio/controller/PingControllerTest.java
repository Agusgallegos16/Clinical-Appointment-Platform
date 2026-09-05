package com.consultorio.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/public/ping sin autenticación debe retornar HTTP 200 OK")
    void testPingPublicGetEndpointWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/public/ping")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("HEAD /api/public/ping sin autenticación debe retornar HTTP 200 OK")
    void testPingPublicHeadEndpointWithoutAuth() throws Exception {
        mockMvc.perform(head("/api/public/ping"))
                .andExpect(status().isOk());
    }
}
