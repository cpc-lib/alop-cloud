package com.company.alop.framework.mq;

import com.company.alop.framework.common.messaging.InternalEventContext;
import com.company.alop.framework.common.tenant.TenantContext;
import com.company.alop.framework.inbox.JdbcInboxDeduplicator;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Delivery;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import javax.sql.DataSource;

/**
 * Manual broker acknowledgement after durable Inbox + business effect commit.
 * Requires a trusted tenant resolver backed by authoritative business identity.
 * Unverified headers/body alone MUST NOT create a TenantContext.
 */
public final class RabbitInboxDeliveryHandler {
    @FunctionalInterface public interface TrustedTenantResolver {
        TenantContext resolve(Connection db, RabbitEventEnvelope envelope) throws SQLException;
    }
    @FunctionalInterface public interface BusinessEffect {
        void apply(Connection db, RabbitEventEnvelope envelope) throws SQLException;
    }
    @FunctionalInterface public interface InternalEventResolver {
        InternalEventContext resolve(Connection db, RabbitEventEnvelope envelope) throws SQLException;
    }
    @FunctionalInterface private interface TransactionEffect {
        void apply(Connection db, RabbitEventEnvelope envelope) throws SQLException;
    }

    private final DataSource dataSource;
    private final JdbcInboxDeduplicator inbox;
    private final TrustedTenantResolver resolver;
    private final BusinessEffect effect;
    private final String group;

    public RabbitInboxDeliveryHandler(DataSource dataSource, JdbcInboxDeduplicator inbox,
            TrustedTenantResolver resolver, BusinessEffect effect, String group) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
        this.inbox = Objects.requireNonNull(inbox, "inbox");
        this.resolver = Objects.requireNonNull(resolver, "resolver");
        this.effect = Objects.requireNonNull(effect, "effect");
        if (group == null || group.isBlank() || group.length() > 128) {
            throw new IllegalArgumentException("Invalid consumer group");
        }
        this.group = group;
    }

    /**
     * On poison or failed business processing: nack without requeue, requiring a
     * configured DLX/quarantine policy. Never ACK before local commit.
     * On ACK IO failure after commit the broker may redeliver; Inbox deduplicates it.
     */
    public boolean handle(Channel channel, Delivery delivery) throws IOException {
        return handleDelivery(channel, delivery, (db, envelope) -> {
            // Trusted resolver MUST verify tenant using authenticated, persisted business facts.
            TenantContext verified = Objects.requireNonNull(resolver.resolve(db, envelope),
                    "TrustedTenantResolver returned null");
            verified.requireTenant(envelope.tenantId());
            inbox.consume(db, verified, envelope.tenantId(), group, envelope.eventId(),
                    envelope.aggregateVersion(), connection -> effect.apply(connection, envelope));
        }, false);
    }

    /**
     * Handle a trusted background event without creating a fake tenant membership.
     * The broker is acknowledged only after the Inbox and business effect commit.
     */
    public boolean handleInternal(Channel channel, Delivery delivery,
            InternalEventResolver resolver, BusinessEffect effect) throws IOException {
        Objects.requireNonNull(resolver, "resolver");
        Objects.requireNonNull(effect, "effect");
        return handleDelivery(channel, delivery, (db, envelope) -> {
            InternalEventContext trusted = Objects.requireNonNull(resolver.resolve(db, envelope),
                    "InternalEventResolver returned null");
            inbox.consume(db, trusted, envelope.tenantId(), group, envelope.eventId(),
                    envelope.aggregateVersion(), connection -> effect.apply(connection, envelope));
        }, true);
    }

    private boolean handleDelivery(Channel channel, Delivery delivery,
            TransactionEffect transactionEffect, boolean requeueOnFailure) throws IOException {
        Objects.requireNonNull(channel, "channel");
        Objects.requireNonNull(delivery, "delivery");
        Objects.requireNonNull(transactionEffect, "transactionEffect");
        long tag = delivery.getEnvelope().getDeliveryTag();
        RabbitEventEnvelope envelope;
        try {
            envelope = validate(delivery);
        } catch (IOException | RuntimeException invalid) {
            channel.basicNack(tag, false, false);
            return false;
        }
        try (Connection db = dataSource.getConnection()) {
            db.setAutoCommit(false);
            try {
                transactionEffect.apply(db, envelope);
                db.commit();
            } catch (SQLException | RuntimeException error) {
                db.rollback();
                channel.basicNack(tag, false, requeueOnFailure);
                return false;
            }
        } catch (SQLException failure) {
            channel.basicNack(tag, false, requeueOnFailure);
            return false;
        }
        channel.basicAck(tag, false);
        return true;
    }

    private static RabbitEventEnvelope validate(Delivery delivery) throws IOException {
        RabbitEventEnvelope envelope = RabbitEventEnvelope.parse(delivery.getBody());
        envelope.checkHeaders(delivery.getProperties().getHeaders());
        if (delivery.getProperties().getMessageId() == null
                || !delivery.getProperties().getMessageId().equals(envelope.eventId())
                || !envelope.eventType().equals(delivery.getProperties().getType())) {
            throw new IOException("AMQP message identity mismatch");
        }
        return envelope;
    }

    public String subscribe(Channel channel, String queue) throws IOException {
        Objects.requireNonNull(channel, "channel");
        if (queue == null || queue.isBlank()) throw new IllegalArgumentException("Queue required");
        channel.basicQos(10);
        return channel.basicConsume(queue, false,
                (consumerTag, delivery) -> handle(channel, delivery),
                cancelled -> { /* broker cancellation is visible to the connection owner */ });
    }
}
