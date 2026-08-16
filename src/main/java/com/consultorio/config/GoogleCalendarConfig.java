package com.consultorio.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GoogleCalendarConfig {

    @Value("${app.google.calendar.enabled:true}")
    private boolean enabled;

    @Value("${app.google.calendar.client-id:}")
    private String clientId;

    @Value("${app.google.calendar.client-secret:}")
    private String clientSecret;

    @Value("${app.google.calendar.redirect-uri:http://localhost:8080/api/google-calendar/callback}")
    private String redirectUri;

    public boolean isEnabled() {
        return enabled;
    }

    public String getClientId() {
        if (clientId != null && !clientId.isBlank()) {
            return cleanValue(clientId);
        }
        String env = System.getenv("GOOGLE_CLIENT_ID");
        if (env == null || env.isBlank()) {
            env = System.getenv("APP_GOOGLE_CALENDAR_CLIENT_ID");
        }
        return env != null ? cleanValue(env) : "";
    }

    public String getClientSecret() {
        if (clientSecret != null && !clientSecret.isBlank()) {
            return cleanValue(clientSecret);
        }
        String env = System.getenv("GOOGLE_CLIENT_SECRET");
        if (env == null || env.isBlank()) {
            env = System.getenv("APP_GOOGLE_CALENDAR_CLIENT_SECRET");
        }
        return env != null ? cleanValue(env) : "";
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    private String cleanValue(String val) {
        if (val == null) return "";
        return val.replace("\"", "").replace("'", "").trim();
    }
}
