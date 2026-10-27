package com.company.alop.framework.outbox;

import com.company.alop.framework.common.messaging.InternalEventContext;
import com.company.alop.framework.common.tenant.TenantContext;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Durable Outbox persistence using frozen V7.0 mq_outbox/integration_task tables.
 * All methods require a caller-owned JDBC transaction. This class never commits.
 * Only trusted internal publisher workers may call claim/complete/retry/dead methods.
 */
public final class JdbcOutboxStore {
    public record Claim(long id, long tenantId, String eventId, String eventType,
            String eventVersion, String payloadJson, int retryCount, String claimToken) {}

    public void enqueue(Connection connection, TenantContext context, OutboxEvent event) throws SQLException {
        transaction(connection);
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(event, "event");
        context.requireTenant(event.tenantId());
        insert(connection, event);
    }

    /**
     * Enqueue an event for a trusted background operation without fabricating tenant membership.
     * The caller still owns the explicit transaction and must commit it.
     */
    public void enqueue(Connection connection, InternalEventContext context, OutboxEvent event)
            throws SQLException {
        transaction(connection);
        Objects.requireNonNull(context, "context");
        insert(connection, event);
    }

    private static void insert(Connection connection, OutboxEvent event) throws SQLException {
        Objects.requireNonNull(event, "event");
        if (event.tenantId() <= 0) {
            throw new IllegalArgumentException("Outbox event tenant ID must be positive");
        }
        String sql = """
                INSERT INTO mq_outbox (id,tenant_id,event_id,aggregate_type,aggregate_id,
                aggregate_version,event_type,event_version,payload,status,retry_count,created_at)
                VALUES (?,?,?,?,?,?,?,?,?,'PENDING',0,UTC_TIMESTAMP(3))
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, event.id());
            statement.setLong(2, event.tenantId());
            statement.setString(3, event.eventId());
            statement.setString(4, event.aggregateType());
            statement.setString(5, event.aggregateId());
            statement.setLong(6, event.aggregateVersion());
            statement.setString(7, event.eventType());
            statement.setString(8, event.eventVersion());
            statement.setString(9, event.payloadJson());
            statement.executeUpdate();
        }
    }

    /**
     * Claim a batch, excluding rows currently locked by other publishers.
     * Lease expiry allows recovery after a publisher crashes.
     * Commit the claim transaction before contacting RabbitMQ; only markPublished
     * after a positive publisher confirm. Redelivery is expected (at-least-once).
     */
    public List<Claim> claim(Connection connection, String token, int limit, int leaseSeconds)
            throws SQLException {
        transaction(connection);
        requireToken(token);
        if (token.length() > 90) throw new IllegalArgumentException("Worker label too long");
        // A fresh fencing token per batch is mandatory, even when the same worker retries.
        String claimToken = token + ":" + java.util.UUID.randomUUID();
        if (limit < 1 || limit > 500 || leaseSeconds < 1 || leaseSeconds > 3600) {
            throw new IllegalArgumentException("Invalid claim batch or lease");
        }
        String select = """
                SELECT id,tenant_id,event_id,event_type,event_version,payload,retry_count
                FROM mq_outbox
                WHERE ((status IN ('PENDING','RETRY') AND
                        (next_retry_at IS NULL OR next_retry_at <= UTC_TIMESTAMP(3)))
                       OR (status='CLAIMED' AND next_retry_at <= UTC_TIMESTAMP(3)))
                ORDER BY id ASC LIMIT ? FOR UPDATE SKIP LOCKED
                """;
        List<Claim> claims = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(select)) {
            statement.setInt(1, limit);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    claims.add(new Claim(result.getLong(1), result.getLong(2),
                            result.getString(3), result.getString(4), result.getString(5),
                            result.getString(6), result.getInt(7), claimToken));
                }
            }
        }
        String update = """
                UPDATE mq_outbox SET status='CLAIMED',claim_token=?,
                  next_retry_at=DATE_ADD(UTC_TIMESTAMP(3),INTERVAL ? SECOND)
                WHERE id=?
                """;
        try (PreparedStatement statement = connection.prepareStatement(update)) {
            for (Claim claim : claims) {
                statement.setString(1, claimToken);
                statement.setInt(2, leaseSeconds);
                statement.setLong(3, claim.id());
                if (statement.executeUpdate() != 1) {
                    throw new SQLException("Lost outbox row during claim: " + claim.id());
                }
            }
        }
        return List.copyOf(claims);
    }

    public void markPublished(Connection connection, Claim claim) throws SQLException {
        transaction(connection);
        transition(connection, claim, """
                UPDATE mq_outbox SET status='PUBLISHED',published_at=UTC_TIMESTAMP(3),
                claim_token=NULL,next_retry_at=NULL
                WHERE id=? AND tenant_id=? AND claim_token=? AND status='CLAIMED'
                """, 0);
    }

    /** Schedule a bounded retry. The caller must explicitly transition to DEAD after exhaustion. */
    public void scheduleRetry(Connection connection, Claim claim, int delaySeconds) throws SQLException {
        transaction(connection);
        if (delaySeconds < 1 || delaySeconds > 86400) {
            throw new IllegalArgumentException("Retry delay must be between 1 and 86400 seconds");
        }
        transition(connection, claim, """
                UPDATE mq_outbox SET status='RETRY', retry_count=retry_count+1,
                claim_token=NULL, next_retry_at=DATE_ADD(UTC_TIMESTAMP(3), INTERVAL ? SECOND)
                WHERE id=? AND tenant_id=? AND claim_token=? AND status='CLAIMED'
                """, delaySeconds);
    }

    /**
     * Exhausted delivery must remain visible and actionable.
     * Creates IntegrationTask in the same transaction as the DEAD transition.
     */
    public void markDead(Connection connection, Claim claim, long taskId,
            String errorCode, String message) throws SQLException {
        transaction(connection);
        if (taskId <= 0 || errorCode == null || errorCode.isBlank() ||
                errorCode.length() > 128 || message == null || message.isBlank()) {
            throw new IllegalArgumentException("Invalid failure task or diagnostic");
        }
        java.sql.Savepoint savepoint = connection.setSavepoint();
        try {
        transition(connection, claim, """
                UPDATE mq_outbox SET status='DEAD',retry_count=retry_count+1,
                  claim_token=NULL,next_retry_at=NULL
                WHERE id=? AND tenant_id=? AND claim_token=? AND status='CLAIMED'
                """, 0);
        String insert = """
                INSERT INTO integration_task
                (id,tenant_id,task_type,business_type,event_id,status,retry_count,
                 last_error_code,last_error_message,manual_required,created_at,updated_at)
                VALUES (?,?,'OUTBOX_DELIVERY','OUTBOX',?,'OPEN',?, ?, ?,1,UTC_TIMESTAMP(3),UTC_TIMESTAMP(3))
                """;
        try (PreparedStatement statement = connection.prepareStatement(insert)) {
            statement.setLong(1, taskId);
            statement.setLong(2, claim.tenantId());
            statement.setString(3, claim.eventId());
            statement.setInt(4, claim.retryCount() + 1);
            statement.setString(5, errorCode);
            statement.setString(6, message);
            statement.executeUpdate();
        }
        connection.releaseSavepoint(savepoint);
        } catch (SQLException | RuntimeException failure) {
            // Do not leave a DEAD event without a durable IntegrationTask.
            try {
                connection.rollback(savepoint);
            } catch (SQLException | RuntimeException rollbackFailure) {
                failure.addSuppressed(rollbackFailure);
            }
            throw failure;
        }
    }

    private static void transition(Connection connection, Claim claim, String sql, int delay)
            throws SQLException {
        Objects.requireNonNull(claim, "claim");
        requireToken(claim.claimToken());
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = 1;
            if (delay > 0) statement.setInt(index++, delay);
            statement.setLong(index++, claim.id());
            statement.setLong(index++, claim.tenantId());
            statement.setString(index, claim.claimToken());
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Outbox claim expired or already finalized: " + claim.id());
            }
        }
    }

    private static void requireToken(String token) {
        if (token == null || token.isBlank() || token.length() > 128) {
            throw new IllegalArgumentException("Claim token required (max 128)");
        }
    }

    private static void transaction(Connection connection) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        if (connection.getAutoCommit()) {
            throw new IllegalStateException("Outbox operations require an explicit local transaction");
        }
    }
}
