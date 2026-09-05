package com.consultorio.email;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component("brevoEmailSender")
public class BrevoEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(BrevoEmailSender.class);
    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";

    private final RestTemplate restTemplate;

    @Value("${mail.brevo.api-key:}")
    private String apiKey;

    @Value("${app.mail.from:notificaciones@consultorio.com}")
    private String mailFrom;

    @Value("${app.clinic.name:Instituto Médico Consultorios}")
    private String clinicName;

    public BrevoEmailSender() {
        this.restTemplate = new RestTemplate();
    }

    @Override
    public String getProviderName() {
        return "Brevo";
    }

    @Override
    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.warn("⚠️ API Key de Brevo no configurada (mail.brevo.api-key). Conmutando a respaldo...");
            throw new EmailProviderException("Brevo", "API Key no configurada", true, 401);
        }

        String brevoApiKey = apiKey.trim();

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        headers.set("api-key", brevoApiKey);

        Map<String, Object> requestBody = Map.of(
                "sender", Map.of("name", clinicName, "email", mailFrom),
                "to", List.of(Map.of("email", to)),
                "subject", subject,
                "htmlContent", htmlContent
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(BREVO_API_URL, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ Correo HTML enviado exitosamente vía API REST de Brevo a: {}", to);
            } else {
                throw new EmailProviderException("Brevo", "Respuesta no exitosa de Brevo", true, response.getStatusCode().value());
            }
        } catch (HttpClientErrorException e) {
            int statusCode = e.getStatusCode().value();
            boolean retryable = (statusCode == 429 || statusCode == 408 || statusCode == 422 || statusCode == 401);
            log.warn("⚠️ Error HTTP client en Brevo [{}]: {} (Reintentable: {})", statusCode, e.getResponseBodyAsString(), retryable);
            throw new EmailProviderException("Brevo", "Error en llamada API Brevo: " + e.getMessage(), retryable, statusCode);
        } catch (HttpServerErrorException e) {
            log.warn("⚠️ Error de servidor en Brevo [{}]: {}", e.getStatusCode().value(), e.getResponseBodyAsString());
            throw new EmailProviderException("Brevo", "Error 5xx en servidor Brevo", true, e.getStatusCode().value());
        } catch (Exception e) {
            log.warn("⚠️ Error de conexión o timeout al enviar correo vía Brevo: {}", e.getMessage());
            throw new EmailProviderException("Brevo", "Fallo de conexión o timeout en Brevo", e, true);
        }
    }
}
