package com.company.alop.tenant.application.command;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Locale;

public record CreateTenantCommand(String idempotencyKey, String tenantCreateRequestId,
        String tenantCode, String tenantName, String tenantType, String packageCode,
        long adminUserId, String timezone, String currency) {
    private static final String DEFAULT_TIMEZONE = "Asia/Shanghai";
    private static final String DEFAULT_CURRENCY = "CNY";

    public CreateTenantCommand {
        requireLength(idempotencyKey, "Idempotency key");
        requireLength(tenantCreateRequestId, "Tenant create request ID");
        requireText(tenantCode, "Tenant code");
        requireText(tenantName, "Tenant name");
        requireText(tenantType, "Tenant type");
        requireLength(packageCode, "Package code", 1, 64);
        if (adminUserId <= 0) {
            throw new IllegalArgumentException("Admin user ID must be positive");
        }
        timezone = normalizeTimezone(timezone);
        currency = normalizeCurrency(currency);
    }

    public String canonicalForm() {
        return String.join("\u001f", idempotencyKey, tenantCreateRequestId, tenantCode,
                tenantName, tenantType, packageCode, Long.toString(adminUserId), timezone, currency);
    }

    private static void requireLength(String value, String label) {
        if (value == null || value.length() < 8 || value.length() > 128 || value.isBlank()) {
            throw new IllegalArgumentException(label + " must be 8-128 characters");
        }
    }

    private static void requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is required");
        }
    }

    private static void requireLength(String value, String label, int minimum, int maximum) {
        requireText(value, label);
        if (value.length() < minimum || value.length() > maximum) {
            throw new IllegalArgumentException(label + " must be " + minimum + "-" + maximum
                    + " characters");
        }
    }

    private static String normalizeTimezone(String value) {
        if (value == null) {
            return DEFAULT_TIMEZONE;
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException("Timezone is required when provided");
        }
        String normalized = value.trim();
        try {
            ZoneId.of(normalized);
        } catch (DateTimeException failure) {
            throw new IllegalArgumentException("Timezone must be a valid IANA timezone", failure);
        }
        return normalized;
    }

    private static String normalizeCurrency(String value) {
        if (value == null) {
            return DEFAULT_CURRENCY;
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException("Currency is required when provided");
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!normalized.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException("Currency must be a three-letter ISO code");
        }
        return normalized;
    }
}
