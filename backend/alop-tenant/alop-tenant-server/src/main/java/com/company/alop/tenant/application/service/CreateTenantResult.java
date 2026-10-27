package com.company.alop.tenant.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
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
        if (version != 0) {
            throw new IllegalArgumentException("CreateTenant result version must be 0");
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
            JsonNode root = JSON.readTree(body);
            if (root == null || !root.isObject()) {
                throw new IllegalArgumentException("Stored CreateTenant response is invalid");
            }
            JsonNode tenantId = required(root, "tenantId");
            JsonNode requestId = required(root, "tenantCreateRequestId");
            JsonNode status = required(root, "status");
            JsonNode version = required(root, "version");
            if (!tenantId.isIntegralNumber() || !tenantId.canConvertToLong() || tenantId.longValue() <= 0) {
                throw new IllegalArgumentException("Stored CreateTenant tenantId is invalid");
            }
            if (!requestId.isTextual()) {
                throw new IllegalArgumentException("Stored CreateTenant request ID is invalid");
            }
            if (!status.isTextual() || !TenantStatus.CREATING.name().equals(status.textValue())) {
                throw new IllegalArgumentException("Stored CreateTenant status is invalid");
            }
            if (!version.isIntegralNumber() || !version.canConvertToLong() || version.longValue() != 0) {
                throw new IllegalArgumentException("Stored CreateTenant version is invalid");
            }
            return new CreateTenantResult(tenantId.longValue(), requestId.textValue(),
                    TenantStatus.CREATING, version.longValue());
        } catch (JsonProcessingException | IllegalArgumentException failure) {
            throw new IllegalArgumentException("Stored CreateTenant response is invalid", failure);
        }
    }

    private static JsonNode required(JsonNode root, String field) {
        JsonNode value = root.get(field);
        if (value == null || value.isNull()) {
            throw new IllegalArgumentException("Stored CreateTenant field is missing: " + field);
        }
        return value;
    }
}
