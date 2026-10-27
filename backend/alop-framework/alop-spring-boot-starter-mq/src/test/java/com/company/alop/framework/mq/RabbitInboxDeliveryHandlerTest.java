package com.company.alop.framework.mq;

import com.company.alop.framework.common.error.BusinessException;
import com.company.alop.framework.common.error.CommonErrors;
import com.company.alop.framework.common.id.SnowflakeIdGenerator;
import com.company.alop.framework.common.messaging.InternalEventContext;
import com.company.alop.framework.common.tenant.TenantContext;
import com.company.alop.framework.inbox.JdbcInboxDeduplicator;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Delivery;
import com.rabbitmq.client.Envelope;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.sql.DataSource;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

        boolean handled = handler.handleInternal(channel, delivery(headers("42")), "alop-tenant",
                (db, envelope) -> context("alop-tenant"),
                (db, envelope) -> calls.add("effect"));

        assertTrue(handled);
        assertTrue(requeues.isEmpty());
        assertEquals(List.of("savepoint", "insert", "effect", "update", "release", "commit", "close", "ack"), calls);
    }

    @Test
    void internalPathRollsBackAndNacksWhenEffectFails() throws Exception {
        List<String> calls = new ArrayList<>();
        RabbitInboxDeliveryHandler handler = new RabbitInboxDeliveryHandler(
                dataSource(connection(calls)), new JdbcInboxDeduplicator(new SnowflakeIdGenerator(3)),
                (db, envelope) -> new TenantContext(42, 420, 4200),
                (db, envelope) -> { }, "unused");

        List<Boolean> requeues = new ArrayList<>();
        boolean handled = handler.handleInternal(channel(calls, requeues), delivery(headers("42")), "alop-iam",
                (db, envelope) -> context("alop-iam"),
                (db, envelope) -> {
                    calls.add("effect");
                    throw new SQLException("business failure");
                });

        assertFalse(handled);
        assertEquals(List.of(true), requeues);
        assertEquals(List.of("savepoint", "insert", "effect", "rollback-savepoint", "rollback", "close", "nack"), calls);
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

        assertFalse(handler.handleInternal(channel, delivery(headers("99")), "alop-iam",
                (db, envelope) -> context("alop-iam"),
                (db, envelope) -> calls.add("effect")));
        assertEquals(List.of(false), requeues);
        assertEquals(List.of("nack"), calls);
    }

    @ParameterizedTest
    @ValueSource(strings = {"alop-tenant", "alop-iam"})
    void internalPathRejectsWrongAllowlistedServiceBeforeInboxWork(String expectedService)
            throws Exception {
        List<String> calls = new ArrayList<>();
        List<Boolean> requeues = new ArrayList<>();
        RabbitInboxDeliveryHandler handler = new RabbitInboxDeliveryHandler(
                dataSource(connection(calls)), new JdbcInboxDeduplicator(new SnowflakeIdGenerator(4)),
                (db, envelope) -> new TenantContext(42, 420, 4200),
                (db, envelope) -> { }, "unused");

        assertFalse(handler.handleInternal(channel(calls, requeues), delivery(headers("42")),
                expectedService, (db, envelope) -> context(expectedService.equals("alop-tenant")
                        ? "alop-iam" : "alop-tenant"),
                (db, envelope) -> calls.add("effect")));

        assertEquals(List.of(false), requeues);
        assertEquals(List.of("rollback", "close", "nack"), calls);
        assertThrows(NoSuchMethodException.class, () -> RabbitInboxDeliveryHandler.class.getMethod(
                "handleInternal", Channel.class, Delivery.class,
                RabbitInboxDeliveryHandler.InternalEventResolver.class,
                RabbitInboxDeliveryHandler.BusinessEffect.class));
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

        assertFalse(handler.handleInternal(channel(calls, requeues), delivery(headers("42")), "alop-iam",
                (db, envelope) -> context("alop-iam"),
                (db, envelope) -> calls.add("effect")));

        assertEquals(List.of(true), requeues);
        assertEquals(List.of("savepoint", "insert", "effect", "update", "release",
                "commit", "rollback", "close", "nack"), calls);
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

        assertFalse(handler.handleInternal(channel(calls, requeues), delivery(headers("42")), "alop-iam",
                (db, envelope) -> context("alop-iam"),
                (db, envelope) -> calls.add("effect")));

        assertEquals(List.of(true), requeues);
        assertEquals(List.of("savepoint", "insert", "effect", "update", "release",
                "commit", "rollback", "close", "nack"), calls);
    }

    @Test
    void ordinaryPathStillRequiresVerifiedTenantContext() throws Exception {
        List<String> calls = new ArrayList<>();
        RabbitInboxDeliveryHandler handler = new RabbitInboxDeliveryHandler(
                dataSource(connection(calls)), new JdbcInboxDeduplicator(new SnowflakeIdGenerator(5)),
                (db, envelope) -> new TenantContext(42, 420, 4200),
                (db, envelope) -> calls.add("effect"), "tenant-consumer");

        assertTrue(handler.handle(channel(calls), delivery(headers("42"))));
        assertEquals(List.of("savepoint", "insert", "effect", "update", "release", "commit", "close", "ack"), calls);
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
        assertEquals(List.of("savepoint", "insert", "rollback-savepoint", "rollback", "close", "nack"), calls);
    }

    @Test
    void ordinaryNonRetryableBusinessFailureNacksWithoutRetry() throws Exception {
        List<String> calls = new ArrayList<>();
        List<Boolean> requeues = new ArrayList<>();
        RabbitInboxDeliveryHandler handler = new RabbitInboxDeliveryHandler(
                dataSource(connection(calls)), new JdbcInboxDeduplicator(new SnowflakeIdGenerator(14)),
                (db, envelope) -> new TenantContext(42, 420, 4200),
                (db, envelope) -> {
                    throw new BusinessException(CommonErrors.TENANT_ACCESS_DENIED);
                }, "tenant-consumer");

        assertFalse(handler.handle(channel(calls, requeues), delivery(headers("42"))));

        assertEquals(List.of(false), requeues);
        assertEquals(List.of("savepoint", "insert", "rollback-savepoint", "rollback", "close", "nack"), calls);
        assertFalse(calls.contains("ack"));
    }

    @Test
    void internalNonRetryableBusinessFailureNacksWithoutRetry() throws Exception {
        List<String> calls = new ArrayList<>();
        List<Boolean> requeues = new ArrayList<>();
        RabbitInboxDeliveryHandler handler = new RabbitInboxDeliveryHandler(
                dataSource(connection(calls)), new JdbcInboxDeduplicator(new SnowflakeIdGenerator(15)),
                (db, envelope) -> new TenantContext(42, 420, 4200),
                (db, envelope) -> { }, "unused");

        assertFalse(handler.handleInternal(channel(calls, requeues), delivery(headers("42")), "alop-iam",
                (db, envelope) -> context("alop-iam"),
                (db, envelope) -> {
                    throw new BusinessException(CommonErrors.TENANT_ACCESS_DENIED);
                }));

        assertEquals(List.of(false), requeues);
        assertEquals(List.of("savepoint", "insert", "rollback-savepoint", "rollback", "close", "nack"), calls);
        assertFalse(calls.contains("ack"));
    }

    @Test
    void ordinaryCloseFailureNacksOnceForRetry() throws Exception {
        List<String> calls = new ArrayList<>();
        List<Boolean> requeues = new ArrayList<>();
        RabbitInboxDeliveryHandler handler = new RabbitInboxDeliveryHandler(
                dataSource(connection(calls, false, false, true)),
                new JdbcInboxDeduplicator(new SnowflakeIdGenerator(16)),
                (db, envelope) -> new TenantContext(42, 420, 4200),
                (db, envelope) -> {
                    calls.add("effect");
                    throw new SQLException("business failure");
                }, "tenant-consumer");

        assertFalse(handler.handle(channel(calls, requeues), delivery(headers("42"))));

        assertEquals(List.of(true), requeues);
        assertEquals(List.of("savepoint", "insert", "effect", "rollback-savepoint",
                "rollback", "close", "nack"), calls);
        assertFalse(calls.contains("ack"));
        assertEquals(1, calls.stream().filter("nack"::equals).count());
    }

    @Test
    void internalCloseFailureNacksOnceForRetry() throws Exception {
        List<String> calls = new ArrayList<>();
        List<Boolean> requeues = new ArrayList<>();
        RabbitInboxDeliveryHandler handler = new RabbitInboxDeliveryHandler(
                dataSource(connection(calls, false, false, true)),
                new JdbcInboxDeduplicator(new SnowflakeIdGenerator(17)),
                (db, envelope) -> new TenantContext(42, 420, 4200),
                (db, envelope) -> { }, "unused");

        assertFalse(handler.handleInternal(channel(calls, requeues), delivery(headers("42")), "alop-tenant",
                (db, envelope) -> context("alop-tenant"),
                (db, envelope) -> {
                    calls.add("effect");
                    throw new SQLException("business failure");
                }));

        assertEquals(List.of(true), requeues);
        assertEquals(List.of("savepoint", "insert", "effect", "rollback-savepoint",
                "rollback", "close", "nack"), calls);
        assertFalse(calls.contains("ack"));
        assertEquals(1, calls.stream().filter("nack"::equals).count());
    }

    @Test
    void ordinaryCommitFailureNacksForRetryWithoutAck() throws Exception {
        List<String> calls = new ArrayList<>();
        List<Boolean> requeues = new ArrayList<>();
        RabbitInboxDeliveryHandler handler = new RabbitInboxDeliveryHandler(
                dataSource(connection(calls, true, false)), new JdbcInboxDeduplicator(new SnowflakeIdGenerator(9)),
                (db, envelope) -> new TenantContext(42, 420, 4200),
                (db, envelope) -> calls.add("effect"), "tenant-consumer");

        assertFalse(handler.handle(channel(calls, requeues), delivery(headers("42"))));

        assertEquals(List.of(true), requeues);
        assertEquals(List.of("savepoint", "insert", "effect", "update", "release",
                "commit", "rollback", "close", "nack"), calls);
        assertFalse(calls.contains("ack"));
    }

    @Test
    void ordinaryRollbackFailureNacksForRetryWithoutAck() throws Exception {
        List<String> calls = new ArrayList<>();
        List<Boolean> requeues = new ArrayList<>();
        RabbitInboxDeliveryHandler handler = new RabbitInboxDeliveryHandler(
                dataSource(connection(calls, true, true)), new JdbcInboxDeduplicator(new SnowflakeIdGenerator(10)),
                (db, envelope) -> new TenantContext(42, 420, 4200),
                (db, envelope) -> calls.add("effect"), "tenant-consumer");

        assertFalse(handler.handle(channel(calls, requeues), delivery(headers("42"))));

        assertEquals(List.of(true), requeues);
        assertEquals(List.of("savepoint", "insert", "effect", "update", "release",
                "commit", "rollback", "close", "nack"), calls);
        assertFalse(calls.contains("ack"));
    }

    @Test
    void invalidEnvelopeNacksWithoutRetry() throws Exception {
        List<String> calls = new ArrayList<>();
        List<Boolean> requeues = new ArrayList<>();
        RabbitInboxDeliveryHandler handler = new RabbitInboxDeliveryHandler(
                dataSource(connection(calls)), new JdbcInboxDeduplicator(new SnowflakeIdGenerator(11)),
                (db, envelope) -> new TenantContext(42, 420, 4200),
                (db, envelope) -> calls.add("effect"), "tenant-consumer");
        Delivery invalid = delivery("{}".getBytes(StandardCharsets.UTF_8), EVENT_ID, EVENT_TYPE,
                headers("42"));

        assertFalse(handler.handle(channel(calls, requeues), invalid));

        assertEquals(List.of(false), requeues);
        assertEquals(List.of("nack"), calls);
        assertFalse(calls.contains("ack"));
    }

    @Test
    void messageIdMismatchNacksWithoutRetry() throws Exception {
        List<String> calls = new ArrayList<>();
        List<Boolean> requeues = new ArrayList<>();
        RabbitInboxDeliveryHandler handler = new RabbitInboxDeliveryHandler(
                dataSource(connection(calls)), new JdbcInboxDeduplicator(new SnowflakeIdGenerator(12)),
                (db, envelope) -> new TenantContext(42, 420, 4200),
                (db, envelope) -> calls.add("effect"), "tenant-consumer");

        assertFalse(handler.handle(channel(calls, requeues),
                delivery(validBody(), "different-event", EVENT_TYPE, headers("42"))));

        assertEquals(List.of(false), requeues);
        assertEquals(List.of("nack"), calls);
        assertFalse(calls.contains("ack"));
    }

    @Test
    void amqpTypeMismatchNacksWithoutRetry() throws Exception {
        List<String> calls = new ArrayList<>();
        List<Boolean> requeues = new ArrayList<>();
        RabbitInboxDeliveryHandler handler = new RabbitInboxDeliveryHandler(
                dataSource(connection(calls)), new JdbcInboxDeduplicator(new SnowflakeIdGenerator(13)),
                (db, envelope) -> new TenantContext(42, 420, 4200),
                (db, envelope) -> calls.add("effect"), "tenant-consumer");

        assertFalse(handler.handle(channel(calls, requeues),
                delivery(validBody(), EVENT_ID, "different.event.type", headers("42"))));

        assertEquals(List.of(false), requeues);
        assertEquals(List.of("nack"), calls);
        assertFalse(calls.contains("ack"));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void classifiesRuntimeFailuresWithoutUnboundedRetry(boolean internal) throws Exception {
        for (RuntimeException primary : List.of(new IllegalStateException("invalid state"),
                new BusinessException(CommonErrors.TENANT_ACCESS_DENIED),
                new BusinessException(CommonErrors.OPTIMISTIC_LOCK_CONFLICT))) {
            List<String> calls = new ArrayList<>();
            List<Boolean> requeues = new ArrayList<>();

            assertFalse(handle(internal, dataSource(connection(calls)), channel(calls, requeues),
                    (db, envelope) -> { throw primary; }));

            assertEquals(List.of(primary instanceof BusinessException business
                    && business.error().retryable()), requeues);
            assertEquals(List.of("savepoint", "insert", "rollback-savepoint", "rollback",
                    "close", "nack"), calls);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void cleanupFailureOverridesNonRetryableBusinessFailure(boolean internal) throws Exception {
        for (String operation : List.of("rollback-savepoint", "rollback", "close")) {
            for (Throwable cleanup : List.of(new SQLException("cleanup failed"),
                    new IllegalStateException("cleanup failed"))) {
                List<String> calls = new ArrayList<>();
                List<Boolean> requeues = new ArrayList<>();
                BusinessException primary = new BusinessException(CommonErrors.TENANT_ACCESS_DENIED);

                assertFalse(handle(internal, dataSource(connection(calls, Map.of(operation, cleanup))),
                        channel(calls, requeues), (db, envelope) -> { throw primary; }));

                assertEquals(List.of(true), requeues, operation);
                assertEquals(1, primary.getSuppressed().length);
                assertSame(cleanup, primary.getSuppressed()[0]);
                assertEquals(List.of("savepoint", "insert", "rollback-savepoint", "rollback",
                        "close", "nack"), calls);
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void preservesAllCleanupFailuresOnPrimary(boolean internal) throws Exception {
        List<String> calls = new ArrayList<>();
        List<Boolean> requeues = new ArrayList<>();
        BusinessException primary = new BusinessException(CommonErrors.TENANT_ACCESS_DENIED);
        SQLException savepoint = new SQLException("savepoint rollback failed");
        SQLException rollback = new SQLException("transaction rollback failed");
        SQLException close = new SQLException("close failed");

        assertFalse(handle(internal, dataSource(connection(calls, Map.of(
                "rollback-savepoint", savepoint, "rollback", rollback, "close", close))),
                channel(calls, requeues), (db, envelope) -> { throw primary; }));

        assertEquals(List.of(true), requeues);
        assertEquals(List.of(savepoint, rollback, close), List.of(primary.getSuppressed()));
        assertEquals(List.of("savepoint", "insert", "rollback-savepoint", "rollback",
                "close", "nack"), calls);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void commitRuntimeFailureNacksOnceForRetry(boolean internal) throws Exception {
        List<String> calls = new ArrayList<>();
        List<Boolean> requeues = new ArrayList<>();

        assertFalse(handle(internal, dataSource(connection(calls,
                Map.of("commit", new IllegalStateException("commit failed")))),
                channel(calls, requeues), (db, envelope) -> calls.add("effect")));

        assertEquals(List.of(true), requeues);
        assertEquals(List.of("savepoint", "insert", "effect", "update", "release", "commit",
                "rollback", "close", "nack"), calls);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void closeFailureAfterCommitNacksOnceForRetry(boolean internal) throws Exception {
        List<String> calls = new ArrayList<>();
        List<Boolean> requeues = new ArrayList<>();

        assertFalse(handle(internal, dataSource(connection(calls, false, false, true)),
                channel(calls, requeues), (db, envelope) -> calls.add("effect")));

        assertEquals(List.of(true), requeues);
        assertEquals(List.of("savepoint", "insert", "effect", "update", "release", "commit",
                "close", "nack"), calls);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void connectionFailureNacksOnceForRetry(boolean internal) throws Exception {
        for (Throwable primary : List.of(new SQLException("connection failed"),
                new IllegalStateException("connection failed"))) {
            List<String> calls = new ArrayList<>();
            List<Boolean> requeues = new ArrayList<>();
            DataSource unavailable = (DataSource) Proxy.newProxyInstance(
                    getClass().getClassLoader(), new Class<?>[]{DataSource.class},
                    (proxy, method, args) -> { throw primary; });

            assertFalse(handle(internal, unavailable, channel(calls, requeues),
                    (db, envelope) -> calls.add("effect")));

            assertEquals(List.of(true), requeues);
            assertEquals(List.of("nack"), calls);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void transactionSetupFailureNacksOnceForRetry(boolean internal) throws Exception {
        List<String> calls = new ArrayList<>();
        List<Boolean> requeues = new ArrayList<>();

        assertFalse(handle(internal, dataSource(connection(calls,
                Map.of("setAutoCommit", new IllegalStateException("transaction setup failed")))),
                channel(calls, requeues), (db, envelope) -> calls.add("effect")));

        assertEquals(List.of(true), requeues);
        assertEquals(List.of("rollback", "close", "nack"), calls);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void brokerIoFailureDoesNotCauseSecondDisposition(boolean internal) {
        for (boolean success : List.of(false, true)) {
            List<String> calls = new ArrayList<>();
            IOException primary = new IOException("broker IO failure");
            Channel channel = (Channel) Proxy.newProxyInstance(
                    getClass().getClassLoader(), new Class<?>[]{Channel.class},
                    (proxy, method, args) -> {
                        calls.add(method.getName());
                        throw primary;
                    });

            assertSame(primary, assertThrows(IOException.class,
                    () -> handle(internal, dataSource(connection(new ArrayList<>())), channel,
                            (db, envelope) -> {
                                if (!success) throw new SQLException("effect failed");
                            })));
            assertEquals(List.of(success ? "basicAck" : "basicNack"), calls);
        }
    }

    private static boolean handle(boolean internal, DataSource dataSource, Channel channel,
            RabbitInboxDeliveryHandler.BusinessEffect effect) throws IOException {
        RabbitInboxDeliveryHandler handler = new RabbitInboxDeliveryHandler(dataSource,
                new JdbcInboxDeduplicator(new SnowflakeIdGenerator(18)),
                (db, envelope) -> new TenantContext(42, 420, 4200), effect, "tenant-consumer");
        return internal ? handler.handleInternal(channel, delivery(headers("42")), "alop-iam",
                (db, envelope) -> context("alop-iam"), effect)
                : handler.handle(channel, delivery(headers("42")));
    }

    private static Connection connection(List<String> calls, Map<String, Throwable> failures) {
        Connection delegate = connection(calls);
        return (Connection) Proxy.newProxyInstance(
                RabbitInboxDeliveryHandlerTest.class.getClassLoader(), new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    Object result;
                    try {
                        result = method.invoke(delegate, args);
                    } catch (InvocationTargetException failure) {
                        throw failure.getCause();
                    }
                    String operation = method.getName().equals("rollback")
                            && args != null && args.length == 1 ? "rollback-savepoint" : method.getName();
                    if (failures.containsKey(operation)) throw failures.get(operation);
                    return result;
                });
    }

    private static Delivery delivery(Map<String, Object> headers) {
        return delivery(validBody(), EVENT_ID, EVENT_TYPE, headers);
    }

    private static Delivery delivery(byte[] body, String messageId, String type,
            Map<String, Object> headers) {
        AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                .messageId(messageId)
                .type(type)
                .headers(headers)
                .build();
        return new Delivery(new Envelope(7, false, "events", EVENT_TYPE), properties, body);
    }

    private static byte[] validBody() {
        return ("{" +
                "\"eventId\":\"" + EVENT_ID + "\",\"eventType\":\"" + EVENT_TYPE + "\"," +
                "\"eventVersion\":\"1.0\",\"tenantId\":\"42\",\"aggregateType\":\"Tenant\"," +
                "\"aggregateId\":\"42\",\"aggregateVersion\":0,\"traceId\":\"trace\"," +
                "\"occurredAt\":\"2026-10-27T00:00:00Z\",\"data\":{}}")
                .getBytes(StandardCharsets.UTF_8);
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
        return connection(calls, false, false, false);
    }

    private static Connection connection(List<String> calls, boolean failCommit,
            boolean failRollback) {
        return connection(calls, failCommit, failRollback, false);
    }

    private static Connection connection(List<String> calls, boolean failCommit,
            boolean failRollback, boolean failClose) {
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
                        case "close" -> {
                            calls.add("close");
                            if (failClose) {
                                throw new SQLException("close failed");
                            }
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
