package com.company.alop.iam.application.service;

import com.company.alop.framework.common.messaging.InternalEventContext;
import com.company.alop.framework.mq.RabbitEventEnvelope;
import com.company.alop.framework.outbox.JdbcOutboxStore;
import com.company.alop.framework.outbox.OutboxEvent;
import com.company.alop.iam.api.event.IamTenantProvisioningCompleted;
import com.company.alop.iam.api.event.IamTenantProvisioningFailed;
import com.company.alop.iam.application.service.TenantProvisioningEventParser.Request;
import com.company.alop.iam.infrastructure.persistence.repository.JdbcTenantProvisioningRepository;
import com.company.alop.iam.infrastructure.persistence.repository.JdbcTenantProvisioningRepository.Provisioned;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.time.Instant;
import java.util.UUID;
import java.util.function.LongSupplier;

public final class TenantProvisioningService {
    public static final String SERVICE = "alop-iam";
    private static final ObjectMapper JSON = new ObjectMapper();
    private final TenantProvisioningEventParser parser = new TenantProvisioningEventParser();
    private final JdbcTenantProvisioningRepository repository;
    private final JdbcOutboxStore outbox;
    private final LongSupplier ids;
    private final InternalEventContext.Provider provider;

    public TenantProvisioningService(JdbcTenantProvisioningRepository repository, JdbcOutboxStore outbox,
            LongSupplier ids, InternalEventContext.Provider provider) {
        this.repository = repository;
        this.outbox = outbox;
        this.ids = ids;
        this.provider = provider;
    }

    /** Called inside handleInternal's Inbox transaction; SQL failures always escape for retry. */
    public void provision(Connection db, RabbitEventEnvelope envelope) throws SQLException {
        if (db.getAutoCommit()) throw new IllegalStateException("Provisioning requires an explicit transaction");
        provider.context().requireService(SERVICE);
        Request request = parser.parse(envelope);
        if (!request.valid()) {
            failed(db, envelope, request, "TENANT_PROVISIONING_INVALID_EVENT");
            return;
        }
        Savepoint effects = db.setSavepoint();
        Provisioned result;
        try {
            result = repository.provision(db, request);
        } catch (TenantProvisioningRejected rejected) {
            if (rejected.getSuppressed().length > 0) {
                throw new SQLException("Provisioning resource cleanup failed", rejected);
            }
            db.rollback(effects);
            db.releaseSavepoint(effects);
            failed(db, envelope, request, rejected.errorCode());
            return;
        }
        db.releaseSavepoint(effects);
        var data = new IamTenantProvisioningCompleted(Long.toString(request.tenantId()), request.adminUserId(),
                result.membershipId(), result.roleId(), request.requestId());
        publish(db, envelope, request, "iam.tenant.provisioning.completed.v1", JSON.valueToTree(data));
    }

    private void failed(Connection db, RabbitEventEnvelope envelope, Request request, String errorCode) throws SQLException {
        repository.recordFailure(db, request, envelope.eventId(), errorCode);
        var data = new IamTenantProvisioningFailed(Long.toString(request.tenantId()), request.adminUserId(),
                request.requestId(), errorCode, false);
        publish(db, envelope, request, "iam.tenant.provisioning.failed.v1", JSON.valueToTree(data));
    }

    private void publish(Connection db, RabbitEventEnvelope original, Request request, String type, JsonNode data) throws SQLException {
        String eventId = UUID.nameUUIDFromBytes((SERVICE + ":" + original.eventId() + ":" + type)
                .getBytes(StandardCharsets.UTF_8)).toString();
        String tenant = Long.toString(request.tenantId());
        ObjectNode event = JSON.createObjectNode().put("eventId", eventId).put("eventType", type)
                .put("eventVersion", "1.0").put("tenantId", tenant).put("aggregateType", "Tenant")
                .put("aggregateId", tenant).put("aggregateVersion", original.aggregateVersion())
                .put("traceId", request.traceId()).put("occurredAt", Instant.now().toString());
        if (request.correlationId() != null) event.put("correlationId", request.correlationId());
        event.set("data", data);
        outbox.enqueue(db, provider.context(), SERVICE, new OutboxEvent(ids.getAsLong(), request.tenantId(),
                eventId, "Tenant", tenant, original.aggregateVersion(), type, "1.0", event.toString()));
    }
}
