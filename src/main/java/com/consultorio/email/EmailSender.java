package com.consultorio.email;

/**
 * Interfaz genérica para la estrategia de envío de correos electrónicos HTML.
 */
public interface EmailSender {
    void sendHtmlEmail(String to, String subject, String htmlContent);
    String getProviderName();
}
