package com.consultorio.security.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2AuthenticationFailureHandler.class);

    private final HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String defaultFrontendUrl;

    @Autowired
    public OAuth2AuthenticationFailureHandler(HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository) {
        this.httpCookieOAuth2AuthorizationRequestRepository = httpCookieOAuth2AuthorizationRequestRepository;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        log.warn("Fallo en la autenticación OAuth2: {}", exception.getMessage());

        httpCookieOAuth2AuthorizationRequestRepository.removeAuthorizationRequestCookies(request, response);

        String errorCode = "oauth_failed";
        if (exception.getMessage() != null && exception.getMessage().toLowerCase().contains("access_denied")) {
            errorCode = "access_denied";
        }

        String cleanFrontendUrl = (defaultFrontendUrl != null && defaultFrontendUrl.trim().endsWith("/"))
                ? defaultFrontendUrl.trim().substring(0, defaultFrontendUrl.trim().length() - 1)
                : (defaultFrontendUrl != null ? defaultFrontendUrl.trim() : "http://localhost:5173");

        String targetUrl = UriComponentsBuilder.fromUriString(cleanFrontendUrl + "/login")
                .queryParam("error", errorCode)
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
