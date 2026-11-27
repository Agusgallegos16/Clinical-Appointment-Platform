package com.consultorio.security.oauth2;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OAuth2OneTimeCodeService {

    private static final long CODE_TTL_SECONDS = 60L;
    private static final int MAX_PENDING_CODES = 5_000;

    private final SecureRandom secureRandom = new SecureRandom();
    private final ConcurrentHashMap<String, CodeEntry> pendingCodes = new ConcurrentHashMap<>();

    private record CodeEntry(Long userId, Instant expiresAt) {}

    public String issueCode(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID is required to issue an OAuth2 exchange code.");
        }
        evictExpired();
        if (pendingCodes.size() >= MAX_PENDING_CODES) {
            evictOldest();
        }
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String code = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        pendingCodes.put(code, new CodeEntry(userId, Instant.now().plusSeconds(CODE_TTL_SECONDS)));
        return code;
    }

    public Long consumeCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("El código de autenticación OAuth2 es inválido o ha expirado.");
        }
        CodeEntry entry = pendingCodes.remove(code.trim());
        if (entry == null || Instant.now().isAfter(entry.expiresAt())) {
            throw new IllegalArgumentException("El código de autenticación OAuth2 es inválido o ha expirado.");
        }
        return entry.userId();
    }

    private void evictExpired() {
        Instant now = Instant.now();
        pendingCodes.entrySet().removeIf(e -> now.isAfter(e.getValue().expiresAt()));
    }

    private void evictOldest() {
        Iterator<Map.Entry<String, CodeEntry>> it = pendingCodes.entrySet().iterator();
        int removed = 0;
        while (it.hasNext() && removed < 500) {
            it.next();
            it.remove();
            removed++;
        }
    }
}
