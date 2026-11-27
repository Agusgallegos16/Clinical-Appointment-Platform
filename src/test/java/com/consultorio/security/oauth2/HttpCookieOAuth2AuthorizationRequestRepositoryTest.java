package com.consultorio.security.oauth2;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.util.SerializationUtils;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringJUnitConfig(classes = HttpCookieOAuth2AuthorizationRequestRepository.class)
@TestPropertySource(properties = "app.jwt.secret=test-oauth2-cookie-hmac-secret-key-256-bits-minimum")
class HttpCookieOAuth2AuthorizationRequestRepositoryTest {

    @Autowired
    private HttpCookieOAuth2AuthorizationRequestRepository repository;

    private OAuth2AuthorizationRequest sampleRequest() {
        return OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .clientId("google-client-id")
                .redirectUri("https://api.consultorio.com/login/oauth2/code/google")
                .scopes(Set.of("openid", "profile", "email"))
                .state("random-csrf-state-123")
                .additionalParameters(Map.of("prompt", "select_account"))
                .attributes(Map.of("registration_id", "google"))
                .build();
    }

    @Test
    @DisplayName("CRIT-01: No debe usar ObjectInputStream, debe firmar con HMAC, fijar SameSite/Secure y rechazar cookies sin firma o alteradas")
    void debeRechazarCookiesSinFirmaHmacOTampereadasYFijarAtributosSeguros() {
        MockHttpServletRequest httpsSaveReq = new MockHttpServletRequest();
        httpsSaveReq.setSecure(true);
        MockHttpServletResponse saveRes = new MockHttpServletResponse();

        OAuth2AuthorizationRequest original = sampleRequest();
        repository.saveAuthorizationRequest(original, httpsSaveReq, saveRes);

        List<String> setCookieHeaders = saveRes.getHeaders(HttpHeaders.SET_COOKIE);
        assertFalse(setCookieHeaders.isEmpty(), "Debe emitir cabecera Set-Cookie");
        String authCookieHeader = setCookieHeaders.stream()
                .filter(h -> h.startsWith(HttpCookieOAuth2AuthorizationRequestRepository.OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME + "="))
                .findFirst()
                .orElseThrow();

        assertTrue(authCookieHeader.contains("HttpOnly"), "La cookie debe incluir HttpOnly");
        assertTrue(authCookieHeader.contains("SameSite=Lax"), "La cookie debe incluir SameSite=Lax");
        assertTrue(authCookieHeader.contains("Secure"), "En HTTPS la cookie debe incluir Secure");

        Cookie savedCookie = saveRes.getCookie(HttpCookieOAuth2AuthorizationRequestRepository.OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
        assertNotNull(savedCookie);
        assertFalse(savedCookie.getValue().startsWith("rO0AB"), "No debe emitir flujo binario de Java ObjectOutputStream (rO0AB)");
        assertTrue(savedCookie.getValue().contains("."), "Debe contener separador de firma HMAC");

        // 1. Cookie serializada con Java SerializationUtils sin firma HMAC debe ser rechazada
        String legacySerialized = Base64.getUrlEncoder().encodeToString(SerializationUtils.serialize(original));
        MockHttpServletRequest forgedJavaStreamReq = new MockHttpServletRequest();
        forgedJavaStreamReq.setCookies(new Cookie(
                HttpCookieOAuth2AuthorizationRequestRepository.OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME,
                legacySerialized));
        assertNull(repository.loadAuthorizationRequest(forgedJavaStreamReq),
                "Debe rechazar payloads de serializacion nativa de Java sin firma HMAC");

        // 2. Cookie con firma alterada debe ser rechazada
        String validVal = savedCookie.getValue();
        int idx = validVal.length() - 5;
        char targetChar = validVal.charAt(idx);
        String tamperedVal = validVal.substring(0, idx) + (targetChar == 'A' ? 'B' : 'A') + validVal.substring(idx + 1);
        MockHttpServletRequest tamperedReq = new MockHttpServletRequest();
        tamperedReq.setCookies(new Cookie(
                HttpCookieOAuth2AuthorizationRequestRepository.OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME,
                tamperedVal));
        assertNull(repository.loadAuthorizationRequest(tamperedReq),
                "Debe rechazar cookies cuya firma HMAC haya sido alterada");
    }

    @Test
    @DisplayName("CRIT-01: Solicitud OAuth2 legítima firmada conserva todos sus campos en round-trip y se limpia al remover")
    void debePreservarRoundTripYLimpiezaDeSolicitudLegitima() {
        MockHttpServletRequest saveReq = new MockHttpServletRequest();
        saveReq.setParameter(HttpCookieOAuth2AuthorizationRequestRepository.REDIRECT_URI_PARAM_COOKIE_NAME,
                "http://localhost:5173/oauth2/redirect");
        MockHttpServletResponse saveRes = new MockHttpServletResponse();

        OAuth2AuthorizationRequest original = sampleRequest();
        repository.saveAuthorizationRequest(original, saveReq, saveRes);

        Cookie authCookie = saveRes.getCookie(HttpCookieOAuth2AuthorizationRequestRepository.OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
        Cookie redirectCookie = saveRes.getCookie(HttpCookieOAuth2AuthorizationRequestRepository.REDIRECT_URI_PARAM_COOKIE_NAME);
        assertNotNull(authCookie);
        assertNotNull(redirectCookie);

        MockHttpServletRequest loadReq = new MockHttpServletRequest();
        loadReq.setCookies(authCookie, redirectCookie);

        OAuth2AuthorizationRequest loaded = repository.loadAuthorizationRequest(loadReq);
        assertNotNull(loaded);
        assertEquals(original.getAuthorizationUri(), loaded.getAuthorizationUri());
        assertEquals(original.getClientId(), loaded.getClientId());
        assertEquals(original.getRedirectUri(), loaded.getRedirectUri());
        assertEquals(original.getState(), loaded.getState());
        assertEquals(original.getScopes(), loaded.getScopes());
        assertEquals("select_account", loaded.getAdditionalParameters().get("prompt"));
        assertEquals("google", loaded.getAttribute("registration_id"));

        MockHttpServletResponse removeRes = new MockHttpServletResponse();
        OAuth2AuthorizationRequest removed = repository.removeAuthorizationRequest(loadReq, removeRes);
        assertNotNull(removed);
        assertEquals(original.getState(), removed.getState());
        assertTrue(removeRes.getHeaders(HttpHeaders.SET_COOKIE).stream().anyMatch(h -> h.contains("Max-Age=0")));
    }
}
