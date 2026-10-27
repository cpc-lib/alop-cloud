package com.company.alop.tenant.domain.tenant;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Locale;

/** Shared CreateTenant value rules and V7 defaults. */
public final class TenantDefaults {
    public static final String TIMEZONE = "Asia/Shanghai";
    public static final String CURRENCY = "CNY";

    private TenantDefaults() {
    }

    public static String normalizeTimezone(String value) {
        if (value == null) {
            return TIMEZONE;
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

    public static String normalizeCurrency(String value) {
        if (value == null) {
            return CURRENCY;
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

    public static String requireIdempotencyKey(String value) {
        return requireLength(value, "Idempotency key", 8, 128);
    }

    public static String requireRequestId(String value) {
        return requireLength(value, "Tenant create request ID", 8, 128);
    }

    public static String requireTenantCode(String value) {
        return requireLength(value, "Tenant code", 1, 64);
    }

    public static String requireTenantName(String value) {
        return requireLength(value, "Tenant name", 1, 128);
    }

    public static String requireTenantType(String value) {
        return requireLength(value, "Tenant type", 1, 32);
    }

    public static String requirePackageCode(String value) {
        return requireLength(value, "Package code", 1, 64);
    }

    private static String requireLength(String value, String label, int minimum, int maximum) {
        if (value == null || value.isBlank() || value.length() < minimum || value.length() > maximum) {
            throw new IllegalArgumentException(label + " must be " + minimum + "-" + maximum
                    + " characters");
        }
        return value;
    }
}
