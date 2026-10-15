package com.consultorio.security.oauth2;

import com.consultorio.domain.Usuario;
import com.consultorio.security.JwtUtils;
import com.consultorio.service.OAuth2ProvisioningService;
import com.consultorio.util.CookieUtils;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2AuthenticationSuccessHandler.class);

    private final JwtUtils jwtUtils;
    private final OAuth2ProvisioningService oAuth2ProvisioningService;
    private final HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String defaultFrontendUrl;

    @Value("${app.cors.allowed-origins:http://localhost:5173,http://localhost:3000,http://localhost:8080}")
    private List<String> allowedOrigins;

    @Autowired
    public OAuth2AuthenticationSuccessHandler(JwtUtils jwtUtils,
                                              OAuth2ProvisioningService oAuth2ProvisioningService,
                                              HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository) {
        this.jwtUtils = jwtUtils;
        this.oAuth2ProvisioningService = oAuth2ProvisioningService;
        this.httpCookieOAuth2AuthorizationRequestRepository = httpCookieOAuth2AuthorizationRequestRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        String targetUrl = determineTargetUrl(request, response, authentication);

        if (response.isCommitted()) {
            log.debug("Response already committed. Unable to redirect to {}", targetUrl);
            return;
        }

        clearAuthenticationAttributes(request, response);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    protected String determineTargetUrl(HttpServletRequest request,
                                       HttpServletResponse response,
                                       Authentication authentication) {
        Optional<String> redirectUri = CookieUtils.getCookie(request, HttpCookieOAuth2AuthorizationRequestRepository.REDIRECT_URI_PARAM_COOKIE_NAME)
                .map(Cookie::getValue);

        String targetUrl;
        if (redirectUri.isPresent() && isAuthorizedRedirectUri(redirectUri.get())) {
            targetUrl = redirectUri.get();
        } else {
            targetUrl = defaultFrontendUrl + "/oauth2/redirect";
        }

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String email = (String) attributes.get("email");
        String googleId = (String) attributes.get("sub");
        String name = (String) attributes.get("name");
        String givenName = (String) attributes.get("given_name");
        String familyName = (String) attributes.get("family_name");

        try {
            Usuario usuario = oAuth2ProvisioningService.procesarUsuarioOAuth2(email, googleId, name, givenName, familyName);
            String token = jwtUtils.generarToken(usuario.getEmail(), usuario.getRol().name());

            return UriComponentsBuilder.fromUriString(targetUrl)
                    .queryParam("token", token)
                    .build().toUriString();
        } catch (IllegalStateException e) {
            if ("USER_BLOCKED".equals(e.getMessage())) {
                return UriComponentsBuilder.fromUriString(defaultFrontendUrl + "/login")
                        .queryParam("error", "account_blocked")
                        .build().toUriString();
            }
            return UriComponentsBuilder.fromUriString(defaultFrontendUrl + "/login")
                    .queryParam("error", "oauth_error")
                    .build().toUriString();
        } catch (Exception e) {
            log.error("Error al procesar login OAuth2: {}", e.getMessage(), e);
            return UriComponentsBuilder.fromUriString(defaultFrontendUrl + "/login")
                    .queryParam("error", "oauth_error")
                    .build().toUriString();
        }
    }

    protected void clearAuthenticationAttributes(HttpServletRequest request, HttpServletResponse response) {
        super.clearAuthenticationAttributes(request);
        httpCookieOAuth2AuthorizationRequestRepository.removeAuthorizationRequestCookies(request, response);
    }

    private boolean isAuthorizedRedirectUri(String uri) {
        try {
            URI clientRedirectUri = URI.create(uri);
            return allowedOrigins.stream()
                    .anyMatch(authorizedRedirectUri -> {
                        URI authorizedURI = URI.create(authorizedRedirectUri.trim());
                        if (authorizedURI.getHost() != null && authorizedURI.getHost().equalsIgnoreCase(clientRedirectUri.getHost())) {
                            int clientPort = clientRedirectUri.getPort() == -1 ? (clientRedirectUri.getScheme() != null && clientRedirectUri.getScheme().equalsIgnoreCase("https") ? 443 : 80) : clientRedirectUri.getPort();
                            int authorizedPort = authorizedURI.getPort() == -1 ? (authorizedURI.getScheme() != null && authorizedURI.getScheme().equalsIgnoreCase("https") ? 443 : 80) : authorizedURI.getPort();
                            return clientPort == authorizedPort;
                        }
                        return false;
                    });
        } catch (Exception e) {
            return false;
        }
    }
}
