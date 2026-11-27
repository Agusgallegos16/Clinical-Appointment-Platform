package com.consultorio.security.oauth2;

import com.consultorio.util.CookieUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

@Component
public class HttpCookieOAuth2AuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    public static final String OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME = "oauth2_auth_request";
    public static final String REDIRECT_URI_PARAM_COOKIE_NAME = "redirect_uri";
    private static final int COOKIE_EXPIRE_SECONDS = 180;

    private final byte[] signingKey;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public HttpCookieOAuth2AuthorizationRequestRepository(@Value("${app.jwt.secret}") String signingSecret) {
        if (!StringUtils.hasText(signingSecret)) {
            throw new IllegalStateException("Se requiere 'app.jwt.secret' para firmar cookies de estado OAuth2");
        }
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            this.signingKey = sha256.digest(("oauth2-cookie-v1:" + signingSecret.trim()).getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        return CookieUtils.getCookie(request, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME)
                .flatMap(cookie -> CookieUtils.verifyAndExtractPayload(cookie.getValue(), this.signingKey))
                .map(this::deserializePayload)
                .orElse(null);
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest,
                                         HttpServletRequest request,
                                         HttpServletResponse response) {
        if (authorizationRequest == null) {
            removeAuthorizationRequestCookies(request, response);
            return;
        }

        CookieUtils.addCookie(request,
                response,
                OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME,
                serializeAndSign(authorizationRequest),
                COOKIE_EXPIRE_SECONDS);

        String redirectUriAfterLogin = request.getParameter(REDIRECT_URI_PARAM_COOKIE_NAME);
        if (StringUtils.hasText(redirectUriAfterLogin)) {
            CookieUtils.addCookie(request,
                    response,
                    REDIRECT_URI_PARAM_COOKIE_NAME,
                    redirectUriAfterLogin,
                    COOKIE_EXPIRE_SECONDS);
        }
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request,
                                                                 HttpServletResponse response) {
        OAuth2AuthorizationRequest authRequest = this.loadAuthorizationRequest(request);
        this.removeAuthorizationRequestCookies(request, response);
        return authRequest;
    }

    public void removeAuthorizationRequestCookies(HttpServletRequest request, HttpServletResponse response) {
        CookieUtils.deleteCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
        CookieUtils.deleteCookie(request, response, REDIRECT_URI_PARAM_COOKIE_NAME);
    }

    private String serializeAndSign(OAuth2AuthorizationRequest authRequest) {
        try {
            OAuth2AuthRequestPayload payload = new OAuth2AuthRequestPayload(
                    authRequest.getAuthorizationUri(),
                    authRequest.getClientId(),
                    authRequest.getRedirectUri(),
                    authRequest.getScopes(),
                    authRequest.getState(),
                    authRequest.getAdditionalParameters(),
                    authRequest.getAuthorizationRequestUri(),
                    authRequest.getAttributes()
            );
            byte[] jsonBytes = objectMapper.writeValueAsBytes(payload);
            return CookieUtils.signPayload(jsonBytes, this.signingKey);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo serializar la solicitud OAuth2", e);
        }
    }

    private OAuth2AuthorizationRequest deserializePayload(byte[] jsonBytes) {
        try {
            OAuth2AuthRequestPayload payload = objectMapper.readValue(jsonBytes, OAuth2AuthRequestPayload.class);
            if (!StringUtils.hasText(payload.authorizationUri()) || !StringUtils.hasText(payload.clientId())) {
                return null;
            }
            OAuth2AuthorizationRequest.Builder builder = OAuth2AuthorizationRequest.authorizationCode()
                    .authorizationUri(payload.authorizationUri())
                    .clientId(payload.clientId())
                    .redirectUri(payload.redirectUri())
                    .scopes(payload.scopes() != null ? payload.scopes() : Collections.emptySet())
                    .state(payload.state())
                    .additionalParameters(payload.additionalParameters() != null ? payload.additionalParameters() : Collections.emptyMap())
                    .attributes(payload.attributes() != null ? payload.attributes() : Collections.emptyMap());
            if (StringUtils.hasText(payload.authorizationRequestUri())) {
                builder.authorizationRequestUri(payload.authorizationRequestUri());
            }
            return builder.build();
        } catch (Exception e) {
            return null;
        }
    }

    private record OAuth2AuthRequestPayload(
            String authorizationUri,
            String clientId,
            String redirectUri,
            Set<String> scopes,
            String state,
            Map<String, Object> additionalParameters,
            String authorizationRequestUri,
            Map<String, Object> attributes
    ) {
    }
}
