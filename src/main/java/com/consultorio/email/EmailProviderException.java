package com.consultorio.email;

public class EmailProviderException extends RuntimeException {

    private final String providerName;
    private final boolean retryable;
    private final int statusCode;

    public EmailProviderException(String providerName, String message, boolean retryable, int statusCode) {
        super(String.format("[%s] %s (HTTP %d)", providerName, message, statusCode));
        this.providerName = providerName;
        this.retryable = retryable;
        this.statusCode = statusCode;
    }

    public EmailProviderException(String providerName, String message, Throwable cause, boolean retryable) {
        super(String.format("[%s] %s", providerName, message), cause);
        this.providerName = providerName;
        this.retryable = retryable;
        this.statusCode = 500;
    }

    public String getProviderName() {
        return providerName;
    }

    public boolean isRetryable() {
        return retryable;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
