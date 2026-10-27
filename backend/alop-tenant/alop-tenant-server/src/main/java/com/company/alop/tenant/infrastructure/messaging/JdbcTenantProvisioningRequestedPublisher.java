package com.company.alop.tenant.infrastructure.messaging;

import com.company.alop.framework.common.messaging.InternalEventContext;
import com.company.alop.framework.outbox.JdbcOutboxStore;
import com.company.alop.framework.outbox.OutboxEvent;
import com.company.alop.tenant.application.command.CreateTenantCommand;
import com.company.alop.tenant.application.port.TenantProvisioningRequestedPublisher;
import com.company.alop.tenant.domain.tenant.Tenant;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.LongSupplier;

/** JDBC Outbox adapter for the versioned Tenant provisioning request event. */
public final class JdbcTenantProvisioningRequestedPublisher
        implements TenantProvisioningRequestedPublisher {
    private static final String EVENT_TYPE = "tenant.provisioning.requested.v1";
    private static final String EVENT_VERSION = "1.0";
    private static final String EXPECTED_SERVICE = "alop-tenant";
    private static final ObjectMapper JSON = new ObjectMapper();

    private final JdbcOutboxStore outboxStore;
    private final LongSupplier eventIdGenerator;
    private final InternalEventContext.Provider contextProvider;

    public JdbcTenantProvisioningRequestedPublisher(JdbcOutboxStore outboxStore,
            LongSupplier eventIdGenerator, InternalEventContext.Provider contextProvider) {
        this.outboxStore = Objects.requireNonNull(outboxStore, "outboxStore");
        this.eventIdGenerator = Objects.requireNonNull(eventIdGenerator, "eventIdGenerator");
        this.contextProvider = Objects.requireNonNull(contextProvider, "contextProvider");
    }

    @Override
    public void publish(Connection connection, Tenant tenant, CreateTenantCommand command)
            throws SQLException {
        Objects.requireNonNull(tenant, "tenant");
        Objects.requireNonNull(command, "command");
        if (tenant.packageId() == null || tenant.packageId() <= 0) {
            throw new IllegalArgumentException("Tenant package ID is required for provisioning");
        }
        long eventId = eventIdGenerator.getAsLong();
        if (eventId <= 0) {
            throw new IllegalArgumentException("Event ID must be positive");
        }
        String eventIdText = Long.toString(eventId);
        String aggregateId = Long.toString(tenant.id());
        String traceId = UUID.randomUUID().toString();
        String payload = payload(eventIdText, aggregateId, traceId, command, tenant);
        outboxStore.enqueue(connection, contextProvider.context(), EXPECTED_SERVICE,
                new OutboxEvent(eventId, tenant.id(), eventIdText, "Tenant", aggregateId, 0,
                        EVENT_TYPE, EVENT_VERSION, payload));
    }

    private static String payload(String eventId, String aggregateId, String traceId,
            CreateTenantCommand command, Tenant tenant) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("tenantId", aggregateId);
        data.put("tenantCreateRequestId", command.tenantCreateRequestId());
        data.put("adminUserId", command.adminUserId());
        data.put("tenantPackageId", tenant.packageId());
        data.put("timezone", tenant.timezone());
        data.put("currency", tenant.currency());

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("eventId", eventId);
        root.put("eventType", EVENT_TYPE);
        root.put("eventVersion", EVENT_VERSION);
        root.put("aggregateType", "Tenant");
        root.put("aggregateId", aggregateId);
        root.put("aggregateVersion", 0);
        root.put("tenantId", aggregateId);
        root.put("traceId", traceId);
        root.put("occurredAt", Instant.now().toString());
        root.put("data", data);
        root.put("correlationId", command.tenantCreateRequestId());
        try {
            return JSON.writeValueAsString(root);
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException("Tenant provisioning event must be JSON serializable", failure);
        }
    }
}
