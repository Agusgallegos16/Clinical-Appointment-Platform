package com.consultorio.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Optional;

public final class CookieUtils {

    private static final String HMAC_ALGO = "HmacSHA256";

    private CookieUtils() {
    }

    public static Optional<Cookie> getCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    return Optional.of(cookie);
                }
            }
        }
        return Optional.empty();
    }

    public static void addCookie(HttpServletRequest request, HttpServletResponse response, String name, String value, int maxAge) {
        addCookie(response, name, value, maxAge, isSecureRequest(request));
    }

    public static void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
        addCookie(response, name, value, maxAge, false);
    }

    public static void addCookie(HttpServletResponse response, String name, String value, int maxAge, boolean secure) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .path("/")
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .maxAge(maxAge)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public static void deleteCookie(HttpServletRequest request, HttpServletResponse response, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            boolean secure = isSecureRequest(request);
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    ResponseCookie cleared = ResponseCookie.from(name, "")
                            .path("/")
                            .httpOnly(true)
                            .secure(secure)
                            .sameSite("Lax")
                            .maxAge(0)
                            .build();
                    response.addHeader(HttpHeaders.SET_COOKIE, cleared.toString());
                }
            }
        }
    }

    public static String signPayload(byte[] payloadBytes, byte[] secretKey) {
        String encodedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(payloadBytes);
        byte[] hmac = computeHmac(encodedPayload.getBytes(StandardCharsets.UTF_8), secretKey);
        String encodedHmac = Base64.getUrlEncoder().withoutPadding().encodeToString(hmac);
        return encodedPayload + "." + encodedHmac;
    }

    public static Optional<byte[]> verifyAndExtractPayload(String signedValue, byte[] secretKey) {
        if (signedValue == null || signedValue.isBlank()) {
            return Optional.empty();
        }
        int dotIdx = signedValue.indexOf('.');
        if (dotIdx <= 0 || dotIdx != signedValue.lastIndexOf('.') || dotIdx == signedValue.length() - 1) {
            return Optional.empty();
        }
        String encodedPayload = signedValue.substring(0, dotIdx);
        String encodedHmac = signedValue.substring(dotIdx + 1);
        try {
            byte[] expectedHmac = computeHmac(encodedPayload.getBytes(StandardCharsets.UTF_8), secretKey);
            String canonicalExpectedHmac = Base64.getUrlEncoder().withoutPadding().encodeToString(expectedHmac);
            if (!MessageDigest.isEqual(
                    canonicalExpectedHmac.getBytes(StandardCharsets.UTF_8),
                    encodedHmac.getBytes(StandardCharsets.UTF_8))) {
                return Optional.empty();
            }
            return Optional.of(Base64.getUrlDecoder().decode(encodedPayload));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static boolean isSecureRequest(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        return request.isSecure() || "https".equalsIgnoreCase(forwardedProto);
    }

    private static byte[] computeHmac(byte[] data, byte[] secretKey) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(new SecretKeySpec(secretKey, HMAC_ALGO));
            return mac.doFinal(data);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo calcular la firma HMAC de la cookie", e);
        }
    }
}
