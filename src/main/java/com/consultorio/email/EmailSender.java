package com.consultorio.email;

public interface EmailSender {
    void sendHtmlEmail(String to, String subject, String htmlContent);
    String getProviderName();
}
