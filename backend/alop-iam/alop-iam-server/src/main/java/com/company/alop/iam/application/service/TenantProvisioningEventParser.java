package com.company.alop.iam.application.service;

import com.company.alop.framework.mq.RabbitEventEnvelope;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Set;

public final class TenantProvisioningEventParser {
    public static final String REQUESTED = "tenant.provisioning.requested.v1";
    private static final ObjectMapper JSON = new ObjectMapper().enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
    private static final Set<String> ENVELOPE_FIELDS = Set.of("eventId", "eventType", "eventVersion",
            "tenantId", "aggregateType", "aggregateId", "aggregateVersion", "traceId", "correlationId", "occurredAt", "data");
    private static final Set<String> DATA_FIELDS = Set.of("tenantId", "tenantCreateRequestId", "adminUserId",
            "tenantPackageId", "timezone", "currency");

    public record Request(long tenantId, long adminUserId, String requestId, String traceId,
            String correlationId, boolean valid) {}

    public Request parse(RabbitEventEnvelope envelope) {
        JsonNode root;
        try {
            root = JSON.readTree(envelope.json());
        } catch (IOException invalid) {
            throw new IllegalArgumentException("Invalid provisioning event");
        }
        JsonNode data = root.path("data");
        // Without these identities the strict failed schema cannot describe a failure.
        // Reject to the broker's quarantine path rather than fabricate a user or request.
        if (!positiveId(root.path("tenantId")) || root.path("tenantId").asLong() != envelope.tenantId()
                || !positiveLong(data.path("adminUserId")) || !text(data.path("tenantCreateRequestId"))
                || !text(root.path("traceId"))) {
            throw new IllegalArgumentException("Unaddressable provisioning event");
        }
        JsonNode correlation = root.path("correlationId");
        boolean valid = only(root, ENVELOPE_FIELDS) && only(data, DATA_FIELDS)
                && REQUESTED.equals(root.path("eventType").asText())
                && "1.0".equals(root.path("eventVersion").asText())
                && "Tenant".equals(root.path("aggregateType").asText())
                && root.path("tenantId").equals(root.path("aggregateId"))
                && root.path("tenantId").equals(data.path("tenantId"))
                && positiveLong(data.path("tenantPackageId")) && text(data.path("timezone"))
                && data.path("currency").isTextual() && data.path("currency").asText().matches("[A-Z]{3}")
                && (!root.has("correlationId") || correlation.isNull() || correlation.isTextual())
                && root.path("aggregateVersion").isIntegralNumber()
                && root.path("aggregateVersion").canConvertToLong() && root.path("aggregateVersion").longValue() >= 0;
        try {
            OffsetDateTime.parse(root.path("occurredAt").asText());
        } catch (java.time.format.DateTimeParseException invalidTime) {
            valid = false;
        }
        return new Request(envelope.tenantId(), data.path("adminUserId").longValue(),
                data.path("tenantCreateRequestId").textValue(), root.path("traceId").textValue(),
                correlation.isTextual() ? correlation.textValue() : null, valid);
    }

    private static boolean only(JsonNode node, Set<String> fields) {
        if (!node.isObject()) return false;
        var names = node.fieldNames();
        while (names.hasNext()) if (!fields.contains(names.next())) return false;
        return true;
    }

    private static boolean text(JsonNode node) { return node.isTextual() && !node.textValue().isBlank(); }
    private static boolean positiveLong(JsonNode node) {
        return node.isIntegralNumber() && node.canConvertToLong() && node.longValue() > 0;
    }
    private static boolean positiveId(JsonNode node) {
        if (!node.isTextual() || !node.textValue().matches("[1-9][0-9]*")) return false;
        try { return Long.parseLong(node.textValue()) > 0; }
        catch (NumberFormatException invalid) { return false; }
    }
}
