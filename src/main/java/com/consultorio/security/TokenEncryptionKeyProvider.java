package com.consultorio.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Component
public class TokenEncryptionKeyProvider {

    private static final String EXPLICIT_KEY_DOMAIN = "app-token-encryption-key-v1:";
    private static final String JWT_FALLBACK_DOMAIN = "google-oauth-token-aes256gcm-v1:";

    private final SecretKey secretKey;

    public TokenEncryptionKeyProvider(
            @Value("${app.security.token-encryption-key:}") String explicitKey,
            @Value("${app.jwt.secret:}") String jwtSecret) {
        this.secretKey = deriveAes256Key(explicitKey, jwtSecret);
        EncryptedStringAttributeConverter.configureKey(this.secretKey);
    }

    public SecretKey getSecretKey() {
        return secretKey;
    }

    private SecretKey deriveAes256Key(String explicitKey, String jwtSecret) {
        String material;
        if (explicitKey != null && !explicitKey.isBlank()) {
            material = EXPLICIT_KEY_DOMAIN + explicitKey.trim();
        } else if (jwtSecret != null && !jwtSecret.isBlank()) {
            material = JWT_FALLBACK_DOMAIN + jwtSecret.trim();
        } else {
            throw new IllegalStateException(
                    "Neither app.security.token-encryption-key (APP_TOKEN_ENCRYPTION_KEY) nor app.jwt.secret (JWT_SECRET) is configured.");
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = digest.digest(material.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(keyBytes, "AES");
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable for AES key derivation.", ex);
        }
    }
}
