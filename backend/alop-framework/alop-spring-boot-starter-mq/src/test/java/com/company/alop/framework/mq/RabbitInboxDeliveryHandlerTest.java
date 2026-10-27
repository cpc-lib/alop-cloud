package com.company.alop.framework.mq;

import com.company.alop.framework.common.id.SnowflakeIdGenerator;
import com.company.alop.framework.common.messaging.InternalEventContext;
import com.company.alop.framework.common.tenant.TenantContext;
import com.company.alop.framework.inbox.JdbcInboxDeduplicator;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Delivery;
import com.rabbitmq.client.Envelope;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RabbitInboxDeliveryHandlerTest {
    private static final String EVENT_ID = "event-1";
    private static final String EVENT_TYPE = "tenant.provisioning.requested.v1";

    @Test
    void internalPathAcksOnlyAfterInboxEffectAndCommit() throws Exception {
        List<String> calls = new ArrayList<>();
        DataSource dataSource = dataSource(connection(calls));
        RabbitInboxDeliveryHandler handler = new RabbitInboxDeliveryHandler(
                dataSource, new JdbcInboxDeduplicator(new SnowflakeIdGenerator(2)),
                (db, envelope) -> new TenantContext(42, 420, 4200),
                (db, envelope) -> { }, "unused");
        List<Boolean> requeues = new ArrayList<>();
        Channel channel = channel(calls, requeues);

        boolean handled = handler.handleInternal(channel, delivery(headers("42")),
                (db, envelope) -> context("alop-tenant"),
                (db, envelope) -> calls.add("effect"));

        assertTrue(handled);
        assertTrue(requeues.isEmpty());
        assertEquals(List.of("savepoint", "insert", "effect", "update", "release", "commit", "ack"), calls);
    }

    @Test
    void internalPathRollsBackAndNacksWhenEffectFails() throws Exception {
        List<String> calls = new ArrayList<>();
        RabbitInboxDeliveryHandler handler = new RabbitInboxDeliveryHandler(
                dataSource(connection(calls)), new JdbcInboxDeduplicator(new SnowflakeIdGenerator(3)),
                (db, envelope) -> new TenantContext(42, 420, 4200),
                (db, envelope) -> { }, "unused");

        List<Boolean> requeues = new ArrayList<>();
        boolean handled = handler.handleInternal(channel(calls, requeues), delivery(headers("42")),
                (db, envelope) -> context("alop-iam"),
                (db, envelope) -> {
                    calls.add("effect");
                    throw new SQLException("business failure");
                });

        assertFalse(handled);
        assertEquals(List.of(true), requeues);
        assertEquals(List.of("savepoint", "insert", "effect", "rollback-savepoint", "rollback", "nack"), calls);
    }

    @Test
    void internalPathReusesEnvelopeIdentityValidation() throws Exception {
        List<String> calls = new ArrayList<>();
        RabbitInboxDeliveryHandler handler = new RabbitInboxDeliveryHandler(
                dataSource(connection(calls)), new JdbcInboxDeduplicator(new SnowflakeIdGenerator(4)),
                (db, envelope) -> new TenantContext(42, 420, 4200),
                (db, envelope) -> { }, "unused");
        List<Boolean> requeues = new ArrayList<>();
        Channel channel = channel(calls, requeues);

        assertFalse(handler.handleInternal(channel, delivery(headers("99")),
                (db, envelope) -> context("alop-iam"),
                (db, envelope) -> calls.add("effect")));
        assertEquals(List.of(false), requeues);
        assertEquals(List.of("nack"), calls);
    }

    @Test
    void internalCommitFailureNacksForRetry() throws Exception {
        List<String> calls = new ArrayList<>();
        List<Boolean> requeues = new ArrayList<>();
        RabbitInboxDeliveryHandler handler = new RabbitInboxDeliveryHandler(
                dataSource(connection(calls, true, false)),
                new JdbcInboxDeduplicator(new SnowflakeIdGenerator(6)),
                (db, envelope) -> new TenantContext(42, 420, 4200),
                (db, envelope) -> { }, "unused");

        assertFalse(handler.handleInternal(channel(calls, requeues), delivery(headers("42")),
                (db, envelope) -> context("alop-iam"),
                (db, envelope) -> calls.add("effect")));

        assertEquals(List.of(true), requeues);
        assertEquals(List.of("savepoint", "insert", "effect", "update", "release",
                "commit", "rollback", "nack"), calls);
    }

    @Test
    void internalRollbackFailureStillNacksForRetry() throws Exception {
        List<String> calls = new ArrayList<>();
        List<Boolean> requeues = new ArrayList<>();
        RabbitInboxDeliveryHandler handler = new RabbitInboxDeliveryHandler(
                dataSource(connection(calls, true, true)),
                new JdbcInboxDeduplicator(new SnowflakeIdGenerator(7)),
                (db, envelope) -> new TenantContext(42, 420, 4200),
                (db, envelope) -> { }, "unused");

        assertFalse(handler.handleInternal(channel(calls, requeues), delivery(headers("42")),
                (db, envelope) -> context("alop-iam"),
                (db, envelope) -> calls.add("effect")));

        assertEquals(List.of(true), requeues);
        assertEquals(List.of("savepoint", "insert", "effect", "update", "release",
                "commit", "rollback", "nack"), calls);
    }

    @Test
    void ordinaryPathStillRequiresVerifiedTenantContext() throws Exception {
        List<String> calls = new ArrayList<>();
        RabbitInboxDeliveryHandler handler = new RabbitInboxDeliveryHandler(
                dataSource(connection(calls)), new JdbcInboxDeduplicator(new SnowflakeIdGenerator(5)),
                (db, envelope) -> new TenantContext(42, 420, 4200),
                (db, envelope) -> calls.add("effect"), "tenant-consumer");

        assertTrue(handler.handle(channel(calls), delivery(headers("42"))));
        assertEquals(List.of("savepoint", "insert", "effect", "update", "release", "commit", "ack"), calls);
    }

    @Test
    void ordinaryEffectFailureNacksForRetry() throws Exception {
        List<String> calls = new ArrayList<>();
        List<Boolean> requeues = new ArrayList<>();
        RabbitInboxDeliveryHandler handler = new RabbitInboxDeliveryHandler(
                dataSource(connection(calls)), new JdbcInboxDeduplicator(new SnowflakeIdGenerator(8)),
                (db, envelope) -> new TenantContext(42, 420, 4200),
                (db, envelope) -> { throw new SQLException("business failure"); }, "tenant-consumer");

        assertFalse(handler.handle(channel(calls, requeues), delivery(headers("42"))));

        assertEquals(List.of(true), requeues);
        assertEquals(List.of("savepoint", "insert", "rollback-savepoint", "rollback", "nack"), calls);
    }

    private static Delivery delivery(Map<String, Object> headers) {
        byte[] body = ("{" +
                "\"eventId\":\"" + EVENT_ID + "\",\"eventType\":\"" + EVENT_TYPE + "\"," +
                "\"eventVersion\":\"1.0\",\"tenantId\":\"42\",\"aggregateType\":\"Tenant\"," +
                "\"aggregateId\":\"42\",\"aggregateVersion\":0,\"traceId\":\"trace\"," +
                "\"occurredAt\":\"2026-10-27T00:00:00Z\",\"data\":{}}")
                .getBytes(StandardCharsets.UTF_8);
        AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                .messageId(EVENT_ID)
                .type(EVENT_TYPE)
                .headers(headers)
                .build();
        return new Delivery(new Envelope(7, false, "events", EVENT_TYPE), properties, body);
    }

    private static Map<String, Object> headers(String tenantId) {
        return Map.of("eventId", EVENT_ID, "eventType", EVENT_TYPE,
                "eventVersion", "1.0", "tenantId", tenantId);
    }

    private static DataSource dataSource(Connection connection) {
        return (DataSource) Proxy.newProxyInstance(
                RabbitInboxDeliveryHandlerTest.class.getClassLoader(), new Class<?>[]{DataSource.class},
                (proxy, method, args) -> method.getName().equals("getConnection")
                        ? connection : defaultValue(method.getReturnType()));
    }

    private static Channel channel(List<String> calls) {
        return channel(calls, new ArrayList<>());
    }

    private static Channel channel(List<String> calls, List<Boolean> requeues) {
        return (Channel) Proxy.newProxyInstance(
                RabbitInboxDeliveryHandlerTest.class.getClassLoader(), new Class<?>[]{Channel.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("basicAck")) calls.add("ack");
                    if (method.getName().equals("basicNack")) {
                        calls.add("nack");
                        requeues.add((Boolean) args[2]);
                    }
                    return defaultValue(method.getReturnType());
                });
    }

    private static Connection connection(List<String> calls) {
        return connection(calls, false, false);
    }

    private static Connection connection(List<String> calls, boolean failCommit,
            boolean failRollback) {
        Savepoint savepoint = (Savepoint) Proxy.newProxyInstance(
                RabbitInboxDeliveryHandlerTest.class.getClassLoader(), new Class<?>[]{Savepoint.class},
                (proxy, method, args) -> defaultValue(method.getReturnType()));
        PreparedStatement statement = (PreparedStatement) Proxy.newProxyInstance(
                RabbitInboxDeliveryHandlerTest.class.getClassLoader(), new Class<?>[]{PreparedStatement.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("executeUpdate")) {
                        calls.add(calls.contains("insert") ? "update" : "insert");
                        return 1;
                    }
                    return defaultValue(method.getReturnType());
                });
        return (Connection) Proxy.newProxyInstance(
                RabbitInboxDeliveryHandlerTest.class.getClassLoader(), new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getAutoCommit" -> { return false; }
                        case "setSavepoint" -> { calls.add("savepoint"); return savepoint; }
                        case "prepareStatement" -> { return statement; }
                        case "rollback" -> {
                            calls.add(args != null && args.length == 1 ? "rollback-savepoint" : "rollback");
                            if (failRollback && (args == null || args.length == 0)) {
                                throw new SQLException("rollback failed");
                            }
                            return null;
                        }
                        case "releaseSavepoint" -> { calls.add("release"); return null; }
                        case "commit" -> {
                            calls.add("commit");
                            if (failCommit) throw new SQLException("commit failed");
                            return null;
                        }
                        default -> { return defaultValue(method.getReturnType()); }
                    }
                });
    }

    private static InternalEventContext context(String serviceName) {
        try {
            var constructor = InternalEventContext.Provider.class.getDeclaredConstructor(String.class);
            constructor.setAccessible(true);
            return ((InternalEventContext.Provider) constructor.newInstance(serviceName)).context();
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("Test provider setup failed", failure);
        }
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0F;
        if (type == double.class) return 0D;
        if (type == char.class) return '\0';
        return null;
    }
}
