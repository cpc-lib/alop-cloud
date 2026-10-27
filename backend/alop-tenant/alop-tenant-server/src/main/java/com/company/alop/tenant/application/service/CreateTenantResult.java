package com.company.alop.tenant.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.company.alop.tenant.domain.tenant.TenantDefaults;
import com.company.alop.tenant.domain.tenant.TenantStatus;

public record CreateTenantResult(long tenantId, String tenantCreateRequestId,
        TenantStatus status, long version) {
    private static final ObjectMapper JSON = new ObjectMapper();

    public CreateTenantResult {
        if (tenantId <= 0) {
            throw new IllegalArgumentException("Tenant ID must be positive");
        }
        tenantCreateRequestId = TenantDefaults.requireRequestId(tenantCreateRequestId);
        if (status != TenantStatus.CREATING) {
            throw new IllegalArgumentException("CreateTenant result status must be CREATING");
        }
        if (version < 0) {
            throw new IllegalArgumentException("Tenant version must not be negative");
        }
    }

    public String responseBody() {
        try {
            return JSON.writeValueAsString(this);
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException("CreateTenant result must be JSON serializable", failure);
        }
    }

    public static CreateTenantResult fromResponseBody(String body) {
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("Stored CreateTenant response is invalid");
        }
        try {
            CreateTenantResult result = JSON.readValue(body, CreateTenantResult.class);
            if (result == null) {
                throw new IllegalArgumentException("Stored CreateTenant response is invalid");
            }
            return result;
        } catch (JsonProcessingException | IllegalArgumentException failure) {
            throw new IllegalArgumentException("Stored CreateTenant response is invalid", failure);
        }
    }
}
