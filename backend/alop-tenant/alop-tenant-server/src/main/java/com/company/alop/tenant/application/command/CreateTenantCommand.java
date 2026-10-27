package com.company.alop.tenant.application.command;

import com.company.alop.tenant.domain.tenant.TenantDefaults;
import java.nio.charset.StandardCharsets;

public record CreateTenantCommand(String idempotencyKey, String tenantCreateRequestId,
        String tenantCode, String tenantName, String tenantType, String packageCode,
        long adminUserId, String timezone, String currency) {
    public CreateTenantCommand {
        idempotencyKey = TenantDefaults.requireIdempotencyKey(idempotencyKey);
        tenantCreateRequestId = TenantDefaults.requireRequestId(tenantCreateRequestId);
        tenantCode = TenantDefaults.requireTenantCode(tenantCode);
        tenantName = TenantDefaults.requireTenantName(tenantName);
        tenantType = TenantDefaults.requireTenantType(tenantType);
        packageCode = TenantDefaults.requirePackageCode(packageCode);
        if (adminUserId <= 0) {
            throw new IllegalArgumentException("Admin user ID must be positive");
        }
        timezone = TenantDefaults.normalizeTimezone(timezone);
        currency = TenantDefaults.normalizeCurrency(currency);
    }

    public String canonicalForm() {
        StringBuilder canonical = new StringBuilder();
        append(canonical, idempotencyKey);
        append(canonical, tenantCreateRequestId);
        append(canonical, tenantCode);
        append(canonical, tenantName);
        append(canonical, tenantType);
        append(canonical, packageCode);
        append(canonical, Long.toString(adminUserId));
        append(canonical, timezone);
        append(canonical, currency);
        return canonical.toString();
    }

    private static void append(StringBuilder canonical, String value) {
        canonical.append(value.getBytes(StandardCharsets.UTF_8).length).append(':').append(value);
    }
}
