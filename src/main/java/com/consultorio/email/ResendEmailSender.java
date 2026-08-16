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

@Component("resendEmailSender")
public class ResendEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(ResendEmailSender.class);
    private static final String RESEND_API_URL = "https://api.resend.com/emails";

    private final RestTemplate restTemplate;

    @Value("${mail.resend.api-key:}")
    private String apiKey;

    @Value("${mail.resend.from:${app.mail.from:onboarding@resend.dev}}")
    private String mailFrom;

    @Value("${app.clinic.name:Instituto Médico Consultorios}")
    private String clinicName;

    public ResendEmailSender() {
        this.restTemplate = new RestTemplate();
    }

    @PostConstruct
    public void init() {
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            String cleanKey = apiKey.trim();
            String maskedKey = cleanKey.length() > 8
                    ? cleanKey.substring(0, 4) + "...." + cleanKey.substring(cleanKey.length() - 4)
                    : "****";
            log.info("🔑 Resend API Key cargada correctamente: [{}] (From: {})", maskedKey, mailFrom);
        } else {
            log.warn("⚠️ Resend API Key NO configurada (propiedad 'mail.resend.api-key').");
        }
    }

    @Override
    public String getProviderName() {
        return "Resend";
    }

    @Override
    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.warn("⚠️ API Key de Resend no configurada (mail.resend.api-key).");
            throw new EmailProviderException("Resend", "API Key de Resend no configurada", false, 401);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        headers.set("Authorization", "Bearer " + apiKey.trim());

        // Dirección de remitente parametrizable (onboarding@resend.dev para pruebas o dominio propio verificado en producción)
        String fromAddress = (mailFrom != null && !mailFrom.isBlank()) ? mailFrom.trim() : "onboarding@resend.dev";
        String fromFormatted = fromAddress.contains("@resend.dev") 
                ? "onboarding@resend.dev"
                : (clinicName != null ? clinicName + " <" + fromAddress + ">" : fromAddress);

        Map<String, Object> requestBody = Map.of(
                "from", fromFormatted,
                "to", List.of(to),
                "subject", subject,
                "html", htmlContent
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(RESEND_API_URL, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ Correo HTML enviado exitosamente vía API REST de Resend a: {}", to);
            } else {
                throw new EmailProviderException("Resend", "Respuesta no exitosa de Resend", false, response.getStatusCode().value());
            }
        } catch (HttpClientErrorException e) {
            int statusCode = e.getStatusCode().value();
            log.warn("⚠️ Error HTTP client en Resend [{}]: {}", statusCode, e.getResponseBodyAsString());
            throw new EmailProviderException("Resend", "Error en llamada API Resend: " + e.getMessage(), false, statusCode);
        } catch (HttpServerErrorException e) {
            log.warn("⚠️ Error de servidor en Resend [{}]: {}", e.getStatusCode().value(), e.getResponseBodyAsString());
            throw new EmailProviderException("Resend", "Error 5xx en servidor Resend", false, e.getStatusCode().value());
        } catch (Exception e) {
            log.warn("⚠️ Error de conexión al enviar correo vía Resend: {}", e.getMessage());
            throw new EmailProviderException("Resend", "Fallo de conexión en Resend", e, false);
        }
    }
}
