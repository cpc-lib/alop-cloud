package com.company.alop.framework.inbox;

import com.company.alop.framework.common.id.SnowflakeIdGenerator;
import com.company.alop.framework.common.messaging.InternalEventContext;
import com.company.alop.framework.common.tenant.TenantContext;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.Objects;

/**
 * Inbox guard for at-least-once consumers.
 * Persist the inbox row AND all business effects using the exact SAME Connection.
 * The caller must COMMIT before acknowledging the broker delivery.
 */
public final class JdbcInboxDeduplicator {
    @FunctionalInterface public interface SqlEffect {
        void apply(Connection connection) throws SQLException;
    }
    private final SnowflakeIdGenerator ids;
    public JdbcInboxDeduplicator(SnowflakeIdGenerator ids) {
        this.ids = Objects.requireNonNull(ids, "ids");
    }

    /**
     * @return true when this delivery caused new business effects, false if already completed
     */
    public boolean consume(Connection connection, TenantContext context, long eventTenantId,
            String consumerGroup, String eventId, Long aggregateVersion, SqlEffect effect)
            throws SQLException {
        Objects.requireNonNull(connection, "connection");
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(effect, "effect");
        context.requireTenant(eventTenantId);
        return consume(connection, eventTenantId, consumerGroup, eventId, aggregateVersion, effect);
    }

    /**
     * Consume an event for a trusted background operation without fabricating tenant membership.
     * Inbox state and the supplied effect still share the caller-owned transaction.
     */
    public boolean consume(Connection connection, InternalEventContext context, long eventTenantId,
            String consumerGroup, String eventId, Long aggregateVersion, SqlEffect effect)
            throws SQLException {
        Objects.requireNonNull(connection, "connection");
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(effect, "effect");
        requireTenantId(eventTenantId);
        return consume(connection, eventTenantId, consumerGroup, eventId, aggregateVersion, effect);
    }

    private boolean consume(Connection connection, long eventTenantId, String consumerGroup,
            String eventId, Long aggregateVersion, SqlEffect effect) throws SQLException {
        if (connection.getAutoCommit()) throw new IllegalStateException("Inbox requires an explicit transaction");
        require(consumerGroup, 128, "consumerGroup");
        require(eventId, 64, "eventId");
        if (aggregateVersion != null && aggregateVersion < 0) {
            throw new IllegalArgumentException("Invalid aggregate version");
        }

        Savepoint savepoint = connection.setSavepoint();
        try {
            String insert = """
                    INSERT INTO mq_inbox
                      (id,tenant_id,consumer_group,event_id,aggregate_version,status)
                    VALUES (?,?,?,?,?,'PROCESSING')
                    """;
            try (PreparedStatement statement = connection.prepareStatement(insert)) {
                statement.setLong(1, ids.nextId());
                statement.setLong(2, eventTenantId);
                statement.setString(3, consumerGroup);
                statement.setString(4, eventId);
                if (aggregateVersion == null) statement.setNull(5, java.sql.Types.BIGINT);
                else statement.setLong(5, aggregateVersion);
                statement.executeUpdate();
            } catch (SQLException failure) {
                // MySQL duplicate-key 1062 may be from either the PK or the Inbox unique key.
                // Never treat an unrelated Snowflake ID collision as successful deduplication.
                if (failure.getErrorCode() != 1062) throw failure;
                String sql = """
                        SELECT status FROM mq_inbox
                        WHERE tenant_id=? AND consumer_group=? AND event_id=?
                        """;
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setLong(1, eventTenantId);
                    statement.setString(2, consumerGroup);
                    statement.setString(3, eventId);
                    try (ResultSet result = statement.executeQuery()) {
                        if (result.next() && "PROCESSED".equals(result.getString(1))) {
                            connection.releaseSavepoint(savepoint);
                            return false;
                        }
                    }
                }
                throw failure;
            }

            effect.apply(connection);
            String update = """
                    UPDATE mq_inbox SET status='PROCESSED',processed_at=UTC_TIMESTAMP(3)
                    WHERE tenant_id=? AND consumer_group=? AND event_id=? AND status='PROCESSING'
                    """;
            try (PreparedStatement statement = connection.prepareStatement(update)) {
                statement.setLong(1, eventTenantId);
                statement.setString(2, consumerGroup);
                statement.setString(3, eventId);
                if (statement.executeUpdate() != 1) {
                    throw new SQLException("Inbox completion state was lost");
                }
            }
            connection.releaseSavepoint(savepoint);
            return true;
        } catch (SQLException | RuntimeException exception) {
            // Protect from accidental partial commits if the application catches an exception.
            connection.rollback(savepoint);
            throw exception;
        }
    }

    private static void require(String value, int length, String name) {
        if (value == null || value.isBlank() || value.length() > length) {
            throw new IllegalArgumentException("Invalid " + name);
        }
    }

    private static void requireTenantId(long tenantId) {
        if (tenantId <= 0) throw new IllegalArgumentException("Inbox event tenant ID must be positive");
    }
}
