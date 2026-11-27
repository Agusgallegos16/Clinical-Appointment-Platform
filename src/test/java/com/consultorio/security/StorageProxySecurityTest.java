package com.consultorio.security;

import com.consultorio.service.SupabaseStorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class StorageProxySecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SupabaseStorageService storageService;

    private static final byte[] SVG_PAYLOAD = "<svg xmlns=\"http://www.w3.org/2000/svg\" onload=\"alert(1)\"></svg>"
            .getBytes(StandardCharsets.UTF_8);

    private static final byte[] PNG_HEADER = new byte[]{
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D
    };

    private static final byte[] JPEG_HEADER = new byte[]{
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10
    };

    private static final byte[] WEBP_HEADER = new byte[]{
            'R', 'I', 'F', 'F', 0x24, 0x00, 0x00, 0x00, 'W', 'E', 'B', 'P', 'V', 'P', '8', ' '
    };

    @Test
    @DisplayName("Unauthenticated requests to storage mutation endpoints are rejected (401)")
    void unauthenticatedStorageMutationsRejected() throws Exception {
        MockMultipartFile validPng = new MockMultipartFile("file", "avatar.png", "image/png", PNG_HEADER);

        mockMvc.perform(multipart("/api/storage/doctores/avatar").file(validPng))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(multipart("/api/storage/institucion").file(validPng))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/api/storage/institucion/galeria_123.png"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PACIENTE and DOCTOR are forbidden from unauthorized storage endpoints")
    void unauthorizedRolesRejected() throws Exception {
        MockMultipartFile validPng = new MockMultipartFile("file", "avatar.png", "image/png", PNG_HEADER);

        mockMvc.perform(multipart("/api/storage/doctores/avatar")
                        .file(validPng)
                        .with(user("pac@test.com").roles("PACIENTE")))
                .andExpect(status().isForbidden());

        mockMvc.perform(multipart("/api/storage/institucion")
                        .file(validPng)
                        .with(user("doc@test.com").roles("DOCTOR")))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/storage/institucion/galeria_123.png")
                        .with(user("doc@test.com").roles("DOCTOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("SVG and arbitrary non-raster uploads are rejected even when renamed to .png")
    void svgUploadsRejectedWith400() throws Exception {
        MockMultipartFile rawSvg = new MockMultipartFile("file", "xss.svg", "image/svg+xml", SVG_PAYLOAD);
        MockMultipartFile spoofedPng = new MockMultipartFile("file", "spoofed.png", "image/png", SVG_PAYLOAD);
        MockMultipartFile htmlAsJpg = new MockMultipartFile("file", "fake.jpg", "image/jpeg",
                "<html><script>alert(1)</script></html>".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/storage/doctores/avatar")
                        .file(rawSvg)
                        .with(user("doc@test.com").roles("DOCTOR")))
                .andExpect(status().isBadRequest());

        mockMvc.perform(multipart("/api/storage/doctores/avatar")
                        .file(spoofedPng)
                        .with(user("doc@test.com").roles("DOCTOR")))
                .andExpect(status().isBadRequest());

        mockMvc.perform(multipart("/api/storage/institucion")
                        .file(spoofedPng)
                        .with(user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isBadRequest());

        mockMvc.perform(multipart("/api/storage/institucion")
                        .file(htmlAsJpg)
                        .with(user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Path traversal filenames on gallery deletion are rejected with 400")
    void pathTraversalDeleteRejected() throws Exception {
        mockMvc.perform(delete("/api/storage/institucion/..evil.png")
                        .with(user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isBadRequest());

        mockMvc.perform(delete("/api/storage/institucion/.hidden")
                        .with(user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Authorized DOCTOR/ADMIN valid JPEG/WEBP uploads and ADMIN gallery deletes proxy cleanly to Supabase")
    void validRasterUploadsAndDeletesSucceed() throws Exception {
        MockRestServiceServer mockServer = MockRestServiceServer.bindTo(storageService.getRestTemplate()).build();

        mockServer.expect(requestTo(startsWith("https://test-project.supabase.co/storage/v1/object/fotos-doctores/avatars/doctor_self_")))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-service-role-key"))
                .andExpect(header("apikey", "test-service-role-key"))
                .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_JPEG_VALUE))
                .andRespond(withSuccess("{\"Key\":\"fotos-doctores/avatars/ok.jpg\"}", MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo(startsWith("https://test-project.supabase.co/storage/v1/object/fotos-institucion/galeria_")))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-service-role-key"))
                .andExpect(header(HttpHeaders.CONTENT_TYPE, "image/webp"))
                .andRespond(withSuccess("{\"Key\":\"fotos-institucion/galeria.webp\"}", MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo("https://test-project.supabase.co/storage/v1/object/fotos-institucion/galeria_123.webp"))
                .andExpect(method(HttpMethod.DELETE))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-service-role-key"))
                .andRespond(withSuccess("{\"message\":\"Successfully deleted\"}", MediaType.APPLICATION_JSON));

        // Even if client sends wrong extension (.png) on JPEG bytes, canonical extension/MIME comes from magic bytes
        MockMultipartFile validJpeg = new MockMultipartFile("file", "photo.png", "application/octet-stream", JPEG_HEADER);
        mockMvc.perform(multipart("/api/storage/doctores/avatar")
                        .file(validJpeg)
                        .param("doctorId", "../../etc/passwd")
                        .with(user("doc@test.com").roles("DOCTOR")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.url", startsWith("https://test-project.supabase.co/storage/v1/object/public/fotos-doctores/avatars/doctor_self_")))
                .andExpect(jsonPath("$.path", containsString(".jpg")));

        MockMultipartFile validWebp = new MockMultipartFile("file", "clinic.webp", "image/webp", WEBP_HEADER);
        mockMvc.perform(multipart("/api/storage/institucion")
                        .file(validWebp)
                        .with(user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.url", startsWith("https://test-project.supabase.co/storage/v1/object/public/fotos-institucion/galeria_")))
                .andExpect(jsonPath("$.path", containsString(".webp")));

        mockMvc.perform(delete("/api/storage/institucion/galeria_123.webp")
                        .with(user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isNoContent());

        mockServer.verify();
    }
}
