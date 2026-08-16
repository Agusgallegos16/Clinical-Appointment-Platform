package com.consultorio.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component("fallbackEmailSender")
public class FallbackEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(FallbackEmailSender.class);

    private final EmailSender primarySender;
    private final EmailSender fallbackSender;

    @Autowired
    public FallbackEmailSender(@Qualifier("brevoEmailSender") EmailSender primarySender,
                               @Qualifier("resendEmailSender") EmailSender fallbackSender) {
        this.primarySender = primarySender;
        this.fallbackSender = fallbackSender;
    }

    @Override
    public String getProviderName() {
        return "FallbackRouter(Brevo -> Resend)";
    }

    @Override
    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            primarySender.sendHtmlEmail(to, subject, htmlContent);
        } catch (EmailProviderException e) {
            if (e.isRetryable()) {
                log.warn("⚠️ Fallo en proveedor primario {} (HTTP {}). Conmutando inmediatamente a proveedor de respaldo {}...",
                        primarySender.getProviderName(), e.getStatusCode(), fallbackSender.getProviderName());
                try {
                    fallbackSender.sendHtmlEmail(to, subject, htmlContent);
                } catch (Exception fallbackEx) {
                    log.error("❌ Falló también el envío vía proveedor de respaldo {}: {}",
                            fallbackSender.getProviderName(), fallbackEx.getMessage());
                    throw fallbackEx;
                }
            } else {
                log.error("❌ Error no reintentable en proveedor primario {} (solicitud inválida): {}",
                        primarySender.getProviderName(), e.getMessage());
                throw e;
            }
        }
    }
}
