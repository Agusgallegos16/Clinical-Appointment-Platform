package com.consultorio.security;

import com.consultorio.domain.Rol;
import com.consultorio.domain.Usuario;
import com.consultorio.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class GoogleTokenEncryptionSecurityTest {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("Google OAuth access and refresh tokens are encrypted at rest with AES-256-GCM (enc:v1:) and decrypt transparently")
    void googleTokensAreEncryptedInDatabaseAndDecryptedOnRead() {
        String rawAccessToken = "ya29.a0AfH6SM_test_access_token_value";
        String rawRefreshToken = "1//0g_test_refresh_token_secret_value";

        Usuario usuario = usuarioRepository.saveAndFlush(Usuario.builder()
                .email("gcal_crypto_" + UUID.randomUUID() + "@test.com")
                .password("encoded-pass")
                .rol(Rol.DOCTOR)
                .activo(true)
                .emailVerificado(true)
                .googleAccessToken(rawAccessToken)
                .googleRefreshToken(rawRefreshToken)
                .build());

        Map<String, Object> rawRow = jdbcTemplate.queryForMap(
                "SELECT google_access_token, google_refresh_token FROM usuarios WHERE id = ?",
                usuario.getId()
        );

        String storedAccess = (String) rawRow.get("google_access_token");
        String storedRefresh = (String) rawRow.get("google_refresh_token");

        assertThat(storedAccess).isNotNull().startsWith("enc:v1:").doesNotContain(rawAccessToken);
        assertThat(storedRefresh).isNotNull().startsWith("enc:v1:").doesNotContain(rawRefreshToken);

        // Encrypting the same plaintext in another row must produce a distinct ciphertext (random IV)
        Usuario usuario2 = usuarioRepository.saveAndFlush(Usuario.builder()
                .email("gcal_crypto2_" + UUID.randomUUID() + "@test.com")
                .password("encoded-pass")
                .rol(Rol.DOCTOR)
                .activo(true)
                .emailVerificado(true)
                .googleAccessToken(rawAccessToken)
                .build());
        String storedAccess2 = jdbcTemplate.queryForObject(
                "SELECT google_access_token FROM usuarios WHERE id = ?",
                String.class,
                usuario2.getId()
        );
        assertThat(storedAccess2).startsWith("enc:v1:").isNotEqualTo(storedAccess);

        // Clear persistence context and verify JPA decrypts back to original plaintext
        entityManager.clear();
        Usuario reloaded = usuarioRepository.findById(usuario.getId()).orElseThrow();
        assertThat(reloaded.getGoogleAccessToken()).isEqualTo(rawAccessToken);
        assertThat(reloaded.getGoogleRefreshToken()).isEqualTo(rawRefreshToken);
    }

    @Test
    @DisplayName("Legacy plaintext Google tokens already in DB remain readable for zero-downtime migration")
    void legacyPlaintextTokenStillReadable() {
        Usuario usuario = usuarioRepository.saveAndFlush(Usuario.builder()
                .email("gcal_legacy_" + UUID.randomUUID() + "@test.com")
                .password("encoded-pass")
                .rol(Rol.DOCTOR)
                .activo(true)
                .emailVerificado(true)
                .build());

        jdbcTemplate.update(
                "UPDATE usuarios SET google_access_token = ?, google_refresh_token = ? WHERE id = ?",
                "legacy-plain-access",
                "legacy-plain-refresh",
                usuario.getId()
        );
        entityManager.clear();

        Usuario reloaded = usuarioRepository.findById(usuario.getId()).orElseThrow();
        assertThat(reloaded.getGoogleAccessToken()).isEqualTo("legacy-plain-access");
        assertThat(reloaded.getGoogleRefreshToken()).isEqualTo("legacy-plain-refresh");
    }

    @Test
    @DisplayName("Tampered AES-256-GCM ciphertext fails closed on read")
    void tamperedCiphertextFailsClosed() {
        Usuario usuario = usuarioRepository.saveAndFlush(Usuario.builder()
                .email("gcal_tamper_" + UUID.randomUUID() + "@test.com")
                .password("encoded-pass")
                .rol(Rol.DOCTOR)
                .activo(true)
                .emailVerificado(true)
                .googleAccessToken("secret-access-token")
                .build());

        jdbcTemplate.update(
                "UPDATE usuarios SET google_access_token = ? WHERE id = ?",
                "enc:v1:AAAAAAAAAAAAAAAAAAAAtamperedciphertext==",
                usuario.getId()
        );
        entityManager.clear();

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> usuarioRepository.findById(usuario.getId()))
                .hasRootCauseInstanceOf(javax.crypto.AEADBadTagException.class);
    }
}
