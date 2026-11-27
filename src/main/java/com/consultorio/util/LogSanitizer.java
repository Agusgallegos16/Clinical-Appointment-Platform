package com.consultorio.util;

import java.util.regex.Pattern;

public final class LogSanitizer {

    private static final Pattern SENSITIVE_QUERY_PARAM = Pattern.compile(
            "((?:token|code|password|secret|apikey|api_key|authorization)=)([^&\\s'\"]+)",
            Pattern.CASE_INSENSITIVE
    );

    private LogSanitizer() {
    }

    public static String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return "[empty]";
        }
        String trimmed = email.trim();
        int at = trimmed.indexOf('@');
        if (at <= 0) {
            return "***";
        }
        String local = trimmed.substring(0, at);
        String domain = trimmed.substring(at);
        if (local.length() == 1) {
            return "*" + domain;
        }
        return local.charAt(0) + "***" + domain;
    }

    public static String redactTokens(String input) {
        if (input == null) {
            return null;
        }
        return SENSITIVE_QUERY_PARAM.matcher(input).replaceAll("$1[REDACTED]");
    }
}
