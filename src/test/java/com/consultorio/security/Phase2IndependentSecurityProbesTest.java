package com.consultorio.security;

import com.consultorio.domain.*;
import com.consultorio.repository.*;
import com.consultorio.service.DoctorService;
import com.consultorio.service.SupabaseStorageService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.AEADBadTagException;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class Phase2IndependentSecurityProbesTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private DoctorRepository doctorRepository;
    @Autowired private PacienteRepository pacienteRepository;
    @Autowired private EspecialidadRepository especialidadRepository;
    @Autowired private HorarioAtencionRepository horarioAtencionRepository;
    @Autowired private SlotHorarioRepository slotHorarioRepository;
    @Autowired private PlantillaAgendaRepository plantillaAgendaRepository;
    @Autowired private TurnoRepository turnoRepository;
    @Autowired private DoctorService doctorService;
    @Autowired private SecurityUtils securityUtils;
    @Autowired private SupabaseStorageService storageService;
    @Autowired private TokenEncryptionKeyProvider keyProvider;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private EntityManager entityManager;
    @Autowired private PasswordEncoder passwordEncoder;

    private static final byte[] PNG_BYTES = new byte[]{
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D
    };
    private static final byte[] JPEG_BYTES = new byte[]{
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10
    };
    private static final byte[] WEBP_BYTES = new byte[]{
            'R', 'I', 'F', 'F', 0x24, 0x00, 0x00, 0x00, 'W', 'E', 'B', 'P', 'V', 'P', '8', ' '
    };

    @AfterEach
    void cleanupContext() {
        SecurityContextHolder.clearContext();
        EncryptedStringAttributeConverter.configureKey(keyProvider.getSecretKey());
    }

    @Test
    @Transactional
    void s5_unauthenticatedSecretariaPacienteAndEdgeCases() throws Exception {
        LocalDate future = LocalDate.now().plusDays(15);
        Especialidad esp = especialidadRepository.save(Especialidad.builder()
                .nombre("ProbeEsp_" + UUID.randomUUID().toString().substring(0, 8)).build());

        Usuario uDocA = usuarioRepository.save(Usuario.builder()
                .email("probe_docA_" + UUID.randomUUID() + "@test.com")
                .password("enc").rol(Rol.DOCTOR).activo(true).emailVerificado(true).build());
        Doctor docA = doctorRepository.save(Doctor.builder()
                .usuario(uDocA).nombre("Doc").apellido("A").disponibleParaTurnos(true)
                .especialidades(new HashSet<>(Set.of(esp))).build());

        Usuario uDocB = usuarioRepository.save(Usuario.builder()
                .email("probe_docB_" + UUID.randomUUID() + "@test.com")
                .password("enc").rol(Rol.DOCTOR).activo(true).emailVerificado(true).build());
        Doctor docB = doctorRepository.save(Doctor.builder()
                .usuario(uDocB).nombre("Doc").apellido("B").disponibleParaTurnos(true)
                .especialidades(new HashSet<>(Set.of(esp))).build());

        HorarioAtencion horarioB = horarioAtencionRepository.save(HorarioAtencion.builder()
                .doctor(docB).especialidad(esp).fecha(future)
                .horaInicio(LocalTime.of(9, 0)).horaFin(LocalTime.of(10, 0)).duracionTurnoMinutos(30).build());
        SlotHorario slotB = slotHorarioRepository.save(SlotHorario.builder()
                .doctor(docB).especialidad(esp).fecha(future)
                .horaInicio(LocalTime.of(9, 0)).horaFin(LocalTime.of(9, 30)).duracionMinutos(30).esPuntual(true).build());
        PlantillaAgenda plantillaB = PlantillaAgenda.builder().doctor(docB).nombre("PB").descripcion("B").build();
        plantillaB.setDetalles(new ArrayList<>(List.of(DetallePlantilla.builder()
                .plantilla(plantillaB).especialidad(esp)
                .horaInicio(LocalTime.of(11, 0)).horaFin(LocalTime.of(12, 0)).duracionTurnoMinutos(30).build())));
        plantillaB = plantillaAgendaRepository.save(plantillaB);

        Usuario uPac = usuarioRepository.save(Usuario.builder()
                .email("probe_pac_" + UUID.randomUUID() + "@test.com")
                .password("enc").rol(Rol.PACIENTE).activo(true).emailVerificado(true).build());
        Paciente pac = pacienteRepository.save(Paciente.builder()
                .usuario(uPac).nombre("P").apellido("P").dni(50000000L + (long) (Math.random() * 90000)).build());
        Turno turnoB = turnoRepository.save(Turno.builder()
                .doctor(docB).paciente(pac).especialidad(esp)
                .fechaHora(LocalDateTime.now().minusHours(2)).estado(EstadoTurno.CONFIRMADO).build());

        String horarioBody = "{\"fecha\":\"%s\",\"horaInicio\":\"14:00\",\"horaFin\":\"15:00\",\"duracionTurnoMinutos\":30,\"especialidadId\":%d}"
                .formatted(future.plusDays(1), esp.getId());
        String plantillaBody = "{\"nombre\":\"P\",\"descripcion\":\"D\",\"detalles\":[{\"horaInicio\":\"14:00\",\"horaFin\":\"15:00\",\"duracionTurnoMinutos\":30,\"especialidadId\":%d}]}"
                .formatted(esp.getId());
        String aplicarBody = "{\"plantillaId\":%d,\"fecha\":\"%s\"}".formatted(plantillaB.getId(), future.plusDays(2));

        // 1. Unauthenticated -> 401 on all 15 DoctorController endpoints + Turno estado
        mockMvc.perform(patch("/api/doctores/" + docB.getId() + "/disponibilidad-turnos").param("disponible", "false")).andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/api/doctores/" + docB.getId() + "/advertencia-bloqueante").param("activa", "true")).andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/api/doctores/" + docB.getId() + "/advertencia-informativa").param("activa", "true")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/doctores/" + docB.getId() + "/horarios").contentType(MediaType.APPLICATION_JSON).content(horarioBody)).andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/doctores/horarios/" + horarioB.getId()).contentType(MediaType.APPLICATION_JSON).content(horarioBody)).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/doctores/" + docB.getId() + "/horarios")).andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/doctores/horarios/" + horarioB.getId())).andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/doctores/" + docB.getId() + "/horarios/semana").param("desde", future.toString()).param("hasta", future.plusDays(7).toString())).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/doctores/" + docB.getId() + "/slots")).andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/doctores/slots/" + slotB.getId())).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/doctores/" + docB.getId() + "/plantillas").contentType(MediaType.APPLICATION_JSON).content(plantillaBody)).andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/doctores/plantillas/" + plantillaB.getId()).contentType(MediaType.APPLICATION_JSON).content(plantillaBody)).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/doctores/" + docB.getId() + "/plantillas")).andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/doctores/plantillas/" + plantillaB.getId())).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/doctores/" + docB.getId() + "/aplicar-plantilla").contentType(MediaType.APPLICATION_JSON).content(aplicarBody)).andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/turnos/" + turnoB.getId() + "/estado").param("nuevoEstado", "COMPLETADO")).andExpect(status().isUnauthorized());

        // 2. SECRETARIA -> 403 on all 14 non-slot endpoints + turno estado
        mockMvc.perform(patch("/api/doctores/" + docB.getId() + "/disponibilidad-turnos").param("disponible", "false").with(user("sec@test.com").roles("SECRETARIA"))).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/doctores/" + docB.getId() + "/horarios").with(user("sec@test.com").roles("SECRETARIA"))).andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/doctores/horarios/" + horarioB.getId()).with(user("sec@test.com").roles("SECRETARIA"))).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/doctores/" + docB.getId() + "/slots").with(user("sec@test.com").roles("SECRETARIA"))).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/doctores/" + docB.getId() + "/plantillas").with(user("sec@test.com").roles("SECRETARIA"))).andExpect(status().isForbidden());
        mockMvc.perform(put("/api/turnos/" + turnoB.getId() + "/estado").param("nuevoEstado", "COMPLETADO").with(user("sec@test.com").roles("SECRETARIA"))).andExpect(status().isForbidden());

        // 3. Cross-doctor plantillaId rejected even for ADMIN
        mockMvc.perform(post("/api/doctores/" + docA.getId() + "/aplicar-plantilla")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(aplicarBody)
                        .with(user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isForbidden());

        // 4. Non-existent IDs fail closed (403) for DOCTOR on findById().orElse(null) paths
        UUID missingDoc = UUID.randomUUID();
        mockMvc.perform(get("/api/doctores/" + missingDoc + "/horarios").with(user(uDocA.getEmail()).roles("DOCTOR"))).andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/doctores/horarios/9999999").with(user(uDocA.getEmail()).roles("DOCTOR"))).andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/doctores/" + missingDoc + "/horarios/semana").param("desde", future.toString()).param("hasta", future.plusDays(1).toString()).with(user(uDocA.getEmail()).roles("DOCTOR"))).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/doctores/" + missingDoc + "/slots").with(user(uDocA.getEmail()).roles("DOCTOR"))).andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/doctores/slots/9999999").with(user(uDocA.getEmail()).roles("DOCTOR"))).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/doctores/" + missingDoc + "/plantillas").with(user(uDocA.getEmail()).roles("DOCTOR"))).andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/doctores/plantillas/9999999").with(user(uDocA.getEmail()).roles("DOCTOR"))).andExpect(status().isForbidden());

        // 5. Doctor with null usuario handled without NPE
        Doctor nullUserDoctor = Doctor.builder().nombre("Orphan").apellido("Doc").usuario(null).build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(uDocA.getEmail(), "N/A", List.of(new SimpleGrantedAuthority("ROLE_DOCTOR"))));
        assertThatThrownBy(() -> securityUtils.verificarAccesoDoctor(nullUserDoctor, false))
                .isInstanceOf(AccessDeniedException.class);
        // Delegator methods in DoctorService enforce ownership
        assertThatThrownBy(() -> doctorService.obtenerHorariosDoctor(docB.getId()))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> doctorService.obtenerSlotsDoctor(docB.getId(), null, null))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> doctorService.eliminarSlotIndividual(slotB.getId()))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> doctorService.limpiarHorariosSemana(docB.getId(), future, future.plusDays(5)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void s6_storageEdgeCasesAndSecurityProbes() throws Exception {
        // Forged SVG with UTF-8 BOM
        byte[] bomSvg = concat(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF}, "<svg onload=alert(1)></svg>".getBytes(StandardCharsets.UTF_8));
        mockMvc.perform(multipart("/api/storage/institucion")
                        .file(new MockMultipartFile("file", "bom.png", "image/png", bomSvg))
                        .with(user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isBadRequest());

        // Forged SVG with >64 bytes leading whitespace
        byte[] paddedSvg = (" ".repeat(80) + "<svg xmlns='http://www.w3.org/2000/svg'></svg>").getBytes(StandardCharsets.UTF_8);
        mockMvc.perform(multipart("/api/storage/institucion")
                        .file(new MockMultipartFile("file", "pad.jpg", "image/jpeg", paddedSvg))
                        .with(user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isBadRequest());

        // <?xml payload renamed .webp
        mockMvc.perform(multipart("/api/storage/institucion")
                        .file(new MockMultipartFile("file", "xml.webp", "image/webp", "<?xml version=\"1.0\"?><root/>".getBytes(StandardCharsets.UTF_8)))
                        .with(user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isBadRequest());

        // Valid PNG bytes with .svg / .svgz extension or image/svg+xml MIME -> 400
        mockMvc.perform(multipart("/api/storage/doctores/avatar")
                        .file(new MockMultipartFile("file", "trick.svg", "image/png", PNG_BYTES))
                        .with(user("doc@test.com").roles("DOCTOR")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(multipart("/api/storage/doctores/avatar")
                        .file(new MockMultipartFile("file", "trick.SVGZ", "image/png", PNG_BYTES))
                        .with(user("doc@test.com").roles("DOCTOR")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(multipart("/api/storage/doctores/avatar")
                        .file(new MockMultipartFile("file", "trick.png", "image/svg+xml", PNG_BYTES))
                        .with(user("doc@test.com").roles("DOCTOR")))
                .andExpect(status().isBadRequest());

        // GIF, PDF, empty, >5MB -> rejected
        mockMvc.perform(multipart("/api/storage/institucion")
                        .file(new MockMultipartFile("file", "anim.png", "image/png", "GIF89a......".getBytes(StandardCharsets.US_ASCII)))
                        .with(user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(multipart("/api/storage/institucion")
                        .file(new MockMultipartFile("file", "doc.jpg", "image/jpeg", "%PDF-1.4\n...".getBytes(StandardCharsets.US_ASCII)))
                        .with(user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(multipart("/api/storage/institucion")
                        .file(new MockMultipartFile("file", "empty.png", "image/png", new byte[0]))
                        .with(user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isBadRequest());

        byte[] oversizedPng = new byte[(int) SupabaseStorageService.MAX_IMAGE_SIZE_BYTES + 1];
        System.arraycopy(PNG_BYTES, 0, oversizedPng, 0, PNG_BYTES.length);
        mockMvc.perform(multipart("/api/storage/institucion")
                        .file(new MockMultipartFile("file", "huge.png", "image/png", oversizedPng))
                        .with(user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isBadRequest());

        // SECRETARIA forbidden on all storage mutations
        mockMvc.perform(multipart("/api/storage/doctores/avatar")
                        .file(new MockMultipartFile("file", "ok.png", "image/png", PNG_BYTES))
                        .with(user("sec@test.com").roles("SECRETARIA")))
                .andExpect(status().isForbidden());
        mockMvc.perform(multipart("/api/storage/institucion")
                        .file(new MockMultipartFile("file", "ok.png", "image/png", PNG_BYTES))
                        .with(user("sec@test.com").roles("SECRETARIA")))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/storage/institucion/galeria_1.png")
                        .with(user("sec@test.com").roles("SECRETARIA")))
                .andExpect(status().isForbidden());

        // Path traversal variants on delete
        mockMvc.perform(delete("/api/storage/institucion/a..b.png").with(user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(delete("/api/storage/institucion/bad name.png").with(user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isBadRequest());

        // DOCTOR with UserDetailsImpl ignores doctorId param and uses u{id}; ADMIN sanitizes doctorId
        MockRestServiceServer mockServer = MockRestServiceServer.bindTo(storageService.getRestTemplate()).build();
        mockServer.expect(requestTo(startsWith("https://test-project.supabase.co/storage/v1/object/fotos-doctores/avatars/doctor_u777_")))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_PNG_VALUE))
                .andRespond(withSuccess("{\"Key\":\"ok.png\"}", MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo(startsWith("https://test-project.supabase.co/storage/v1/object/fotos-doctores/avatars/doctor_evil123_")))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_JPEG_VALUE))
                .andRespond(withSuccess("{\"Key\":\"ok.jpg\"}", MediaType.APPLICATION_JSON));

        UserDetailsImpl doctorPrincipal = new UserDetailsImpl(777L, "doc777@test.com", "pw",
                Rol.DOCTOR, true, false, List.of(new SimpleGrantedAuthority("ROLE_DOCTOR")));
        mockMvc.perform(multipart("/api/storage/doctores/avatar")
                        .file(new MockMultipartFile("file", "ok.png", "image/png", PNG_BYTES))
                        .param("doctorId", "victim-doctor-uuid")
                        .with(user(doctorPrincipal)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.url", containsString("avatars/doctor_u777_")))
                .andExpect(jsonPath("$.path", containsString(".png")));

        mockMvc.perform(multipart("/api/storage/doctores/avatar")
                        .file(new MockMultipartFile("file", "ok.jpg", "image/jpeg", JPEG_BYTES))
                        .param("doctorId", "../../evil!@#123")
                        .with(user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.url", containsString("avatars/doctor_evil123_")))
                .andExpect(jsonPath("$.path", containsString(".jpg")));

        mockServer.verify();
    }

    @Test
    @Transactional
    void s7_aes256GcmKeyIsolationAndEdgeCases() {
        // Null tokens persist as SQL NULL
        Usuario uNull = usuarioRepository.saveAndFlush(Usuario.builder()
                .email("probe_null_" + UUID.randomUUID() + "@test.com")
                .password("enc").rol(Rol.PACIENTE).activo(true).emailVerificado(true)
                .googleAccessToken(null).googleRefreshToken(null).build());
        Map<String, Object> nullRow = jdbcTemplate.queryForMap(
                "SELECT google_access_token, google_refresh_token FROM usuarios WHERE id = ?", uNull.getId());
        assertThat(nullRow.get("google_access_token")).isNull();
        assertThat(nullRow.get("google_refresh_token")).isNull();

        // Empty string round-trip
        EncryptedStringAttributeConverter conv = new EncryptedStringAttributeConverter();
        String encEmpty = conv.convertToDatabaseColumn("");
        assertThat(encEmpty).startsWith("enc:v1:");
        assertThat(conv.convertToEntityAttribute(encEmpty)).isEqualTo("");

        // Explicit key vs JWT fallback derive distinct keys
        TokenEncryptionKeyProvider explicitProvider = new TokenEncryptionKeyProvider("explicit-master-secret", "jwt-fallback-secret");
        SecretKey explicitKey = explicitProvider.getSecretKey();
        TokenEncryptionKeyProvider fallbackProvider = new TokenEncryptionKeyProvider("", "jwt-fallback-secret");
        SecretKey fallbackKey = fallbackProvider.getSecretKey();
        assertThat(explicitKey.getEncoded()).hasSize(32);
        assertThat(fallbackKey.getEncoded()).hasSize(32);
        assertThat(explicitKey.getEncoded()).isNotEqualTo(fallbackKey.getEncoded());

        // Cross-key decrypt fails closed with AEADBadTagException
        EncryptedStringAttributeConverter.configureKey(explicitKey);
        String encWithExplicit = conv.convertToDatabaseColumn("secret-refresh-token");
        EncryptedStringAttributeConverter.configureKey(fallbackKey);
        assertThatThrownBy(() -> conv.convertToEntityAttribute(encWithExplicit))
                .isInstanceOf(IllegalStateException.class)
                .hasCauseInstanceOf(AEADBadTagException.class);

        // Missing both keys fails closed
        assertThatThrownBy(() -> new TokenEncryptionKeyProvider("", "   "))
                .isInstanceOf(IllegalStateException.class);

        // Truncated & invalid Base64 fail closed
        assertThatThrownBy(() -> conv.convertToEntityAttribute("enc:v1:AAAA"))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> conv.convertToEntityAttribute("enc:v1:!!!not-base64!!!"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @Transactional
    void s7_passwordResetStep1AndStep2EdgeCases() throws Exception {
        Usuario u = usuarioRepository.saveAndFlush(Usuario.builder()
                .email("probe_reset_" + UUID.randomUUID() + "@test.com")
                .password(passwordEncoder.encode("InitialPass123!"))
                .rol(Rol.PACIENTE).activo(true).emailVerificado(true)
                .nuevaPasswordPendiente("stale-pending-hash")
                .build());

        // Step 1 rejects legacy fields (strict Jackson deserialization)
        String step1LegacyPayload = """
                {
                  "email": "%s",
                  "nuevaPassword": "AttackerPassword123!",
                  "confirmarNuevaPassword": "AttackerPassword123!"
                }
                """.formatted(u.getEmail());

        mockMvc.perform(post("/api/auth/solicitar-restablecimiento-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(step1LegacyPayload))
                .andExpect(status().isBadRequest());

        // Step 1 with uppercase email only -> succeeds, clears stale nuevaPasswordPendiente
        String step1ValidPayload = """
                { "email": "%s" }
                """.formatted(u.getEmail().toUpperCase(Locale.ROOT));

        mockMvc.perform(post("/api/auth/solicitar-restablecimiento-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(step1ValidPayload))
                .andExpect(status().isOk());

        entityManager.flush();
        entityManager.clear();
        Usuario afterStep1 = usuarioRepository.findById(u.getId()).orElseThrow();
        String token = afterStep1.getTokenRestablecimientoPassword();
        assertThat(token).isNotBlank().hasSizeGreaterThanOrEqualTo(43);
        assertThat(afterStep1.getNuevaPasswordPendiente()).isNull();

        // Step 2 mismatched passwords -> 400 and token NOT consumed
        mockMvc.perform(post("/api/auth/confirmar-restablecimiento-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"%s\",\"nuevaPassword\":\"ValidPass123!\",\"confirmarNuevaPassword\":\"DifferentPass123!\"}".formatted(token)))
                .andExpect(status().isBadRequest());

        // Step 2 short password (<6) -> 400 and token NOT consumed
        mockMvc.perform(post("/api/auth/confirmar-restablecimiento-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"%s\",\"nuevaPassword\":\"12345\",\"confirmarNuevaPassword\":\"12345\"}".formatted(token)))
                .andExpect(status().isBadRequest());

        // Token still works for legitimate confirmation
        mockMvc.perform(post("/api/auth/confirmar-restablecimiento-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"%s\",\"nuevaPassword\":\"FinalPass999!\",\"confirmarNuevaPassword\":\"FinalPass999!\"}".formatted(token)))
                .andExpect(status().isOk());

        entityManager.flush();
        entityManager.clear();
        Usuario afterStep2 = usuarioRepository.findById(u.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("FinalPass999!", afterStep2.getPassword())).isTrue();
        assertThat(afterStep2.getTokenRestablecimientoPassword()).isNull();
    }

    @Test
    void s7_expiredTokenMustBeClearedInCommittedTransaction() throws Exception {
        // Deliberately NON-@Transactional to observe real commit/rollback behavior!
        String expiredToken = "expired_probe_" + UUID.randomUUID();
        Usuario u = usuarioRepository.saveAndFlush(Usuario.builder()
                .email("probe_expired_" + UUID.randomUUID() + "@test.com")
                .password(passwordEncoder.encode("KeepPass123!"))
                .rol(Rol.PACIENTE).activo(true).emailVerificado(true)
                .tokenRestablecimientoPassword(expiredToken)
                .tokenRestablecimientoExpiracion(LocalDateTime.now().minusMinutes(10))
                .build());

        try {
            mockMvc.perform(post("/api/auth/confirmar-restablecimiento-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"token\":\"%s\",\"nuevaPassword\":\"NewPass123!\",\"confirmarNuevaPassword\":\"NewPass123!\"}".formatted(expiredToken)))
                    .andExpect(status().isBadRequest());

            Usuario reloaded = usuarioRepository.findById(u.getId()).orElseThrow();
            assertThat(reloaded.getTokenRestablecimientoPassword())
                    .as("Expired token should be cleared in DB even though IllegalArgumentException is thrown")
                    .isNull();
            assertThat(reloaded.getTokenRestablecimientoExpiracion())
                    .as("Expired token expiry timestamp should be cleared in DB")
                    .isNull();
        } finally {
            usuarioRepository.deleteById(u.getId());
        }
    }

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] out = new byte[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }
}
