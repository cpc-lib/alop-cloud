package com.company.alop.framework.inbox;

import com.company.alop.framework.common.error.BusinessException;
import com.company.alop.framework.common.error.CommonErrors;
import com.company.alop.framework.common.id.SnowflakeIdGenerator;
import com.company.alop.framework.common.messaging.InternalEventContext;
import com.company.alop.framework.common.tenant.TenantContext;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcInboxDeduplicatorTest {
    private static final InternalEventContext INTERNAL = context("alop-iam");
    private final JdbcInboxDeduplicator inbox = new JdbcInboxDeduplicator(new SnowflakeIdGenerator(1));

    @Test
    void internalConsumeUsesSameConnectionAndMarksInboxAfterEffect() throws Exception {
        List<String> calls = new ArrayList<>();
        Connection connection = connection(false, calls);
        Connection[] effectConnection = new Connection[1];

        boolean applied = inbox.consume(connection, INTERNAL, 42, "tenant-provisioning",
                UUID.randomUUID().toString(), 0L, db -> {
                    calls.add("effect");
                    effectConnection[0] = db;
                });

        assertTrue(applied);
        assertSame(connection, effectConnection[0]);
        assertEquals(List.of("savepoint", "insert", "effect", "update", "release"), calls);
    }

    @Test
    void internalConsumeRequiresExplicitTransactionAndPositiveTenant() {
        assertThrows(IllegalStateException.class,
                () -> inbox.consume(connection(true, new ArrayList<>()), INTERNAL, 42,
                        "group", UUID.randomUUID().toString(), null, db -> { }));
        assertThrows(IllegalArgumentException.class,
                () -> inbox.consume(connection(false, new ArrayList<>()), INTERNAL, 0,
                        "group", UUID.randomUUID().toString(), null, db -> { }));
    }

    @Test
    void failedInternalEffectRollsBackInboxWork() {
        List<String> calls = new ArrayList<>();
        Connection connection = connection(false, calls);

        assertThrows(SQLException.class, () -> inbox.consume(connection, INTERNAL, 42, "group",
                UUID.randomUUID().toString(), null, db -> {
                    calls.add("effect");
                    throw new SQLException("business failure");
                }));

        assertEquals(List.of("savepoint", "insert", "effect", "rollback"), calls);
    }

    @Test
    void failedEffectKeepsPrimaryWhenSavepointRollbackFails() {
        List<String> calls = new ArrayList<>();
        SQLException primary = new SQLException("effect failure");
        SQLException rollbackFailure = new SQLException("rollback failure");

        SQLException failure = assertThrows(SQLException.class,
                () -> inbox.consume(connection(false, calls, rollbackFailure), INTERNAL, 42,
                        "group", UUID.randomUUID().toString(), null, db -> {
                            calls.add("effect");
                            throw primary;
                        }));

        assertSame(primary, failure);
        assertEquals(1, failure.getSuppressed().length);
        assertSame(rollbackFailure, failure.getSuppressed()[0]);
    }

    @Test
    void failedBusinessEffectKeepsPrimaryWhenRollbackThrowsRuntimeException() {
        BusinessException primary = new BusinessException(CommonErrors.TENANT_ACCESS_DENIED);
        IllegalStateException rollbackFailure = new IllegalStateException("rollback failed");

        BusinessException failure = assertThrows(BusinessException.class,
                () -> inbox.consume(connection(false, new ArrayList<>(), rollbackFailure),
                        new TenantContext(42, 420, 4200), 42, "group", "event", null,
                        db -> { throw primary; }));

        assertSame(primary, failure);
        assertEquals(List.of(rollbackFailure), List.of(failure.getSuppressed()));
    }

    @Test
    void processedDuplicateDoesNotRunInternalEffectAgain() throws Exception {
        List<String> calls = new ArrayList<>();
        boolean[] effectApplied = {false};

        boolean applied = inbox.consume(duplicateProcessedConnection(calls), INTERNAL, 42,
                "group", UUID.randomUUID().toString(), null, db -> effectApplied[0] = true);

        assertFalse(applied);
        assertFalse(effectApplied[0]);
        assertEquals(List.of("savepoint", "duplicate", "lookup", "release"), calls);
    }

    @Test
    void ordinaryConsumeStillRejectsMismatchedTenant() {
        TenantContext user = new TenantContext(7, 70, 700);

        BusinessException failure = assertThrows(BusinessException.class,
                () -> inbox.consume(connection(false, new ArrayList<>()), user, 42,
                        "group", UUID.randomUUID().toString(), null, db -> { }));

        assertEquals("TENANT_ACCESS_DENIED", failure.error().code());
        assertFalse(failure.error().retryable());
    }

    private static Connection connection(boolean autoCommit, List<String> calls) {
        return connection(autoCommit, calls, null);
    }

    private static Connection connection(boolean autoCommit, List<String> calls,
            Throwable rollbackFailure) {
        Savepoint savepoint = (Savepoint) Proxy.newProxyInstance(
                JdbcInboxDeduplicatorTest.class.getClassLoader(), new Class<?>[]{Savepoint.class},
                (proxy, method, args) -> defaultValue(method.getReturnType()));
        PreparedStatement statement = (PreparedStatement) Proxy.newProxyInstance(
                JdbcInboxDeduplicatorTest.class.getClassLoader(), new Class<?>[]{PreparedStatement.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("executeUpdate")) {
                        calls.add(calls.contains("insert") ? "update" : "insert");
                        return 1;
                    }
                    return defaultValue(method.getReturnType());
                });
        return (Connection) Proxy.newProxyInstance(
                JdbcInboxDeduplicatorTest.class.getClassLoader(), new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getAutoCommit" -> { return autoCommit; }
                        case "setSavepoint" -> { calls.add("savepoint"); return savepoint; }
                        case "prepareStatement" -> { return statement; }
                        case "rollback" -> {
                            calls.add("rollback");
                            if (rollbackFailure != null) throw rollbackFailure;
                            return null;
                        }
                        case "releaseSavepoint" -> { calls.add("release"); return null; }
                        default -> { return defaultValue(method.getReturnType()); }
                    }
                });
    }

    private static Connection duplicateProcessedConnection(List<String> calls) {
        Savepoint savepoint = (Savepoint) Proxy.newProxyInstance(
                JdbcInboxDeduplicatorTest.class.getClassLoader(), new Class<?>[]{Savepoint.class},
                (proxy, method, args) -> defaultValue(method.getReturnType()));
        ResultSet result = (ResultSet) Proxy.newProxyInstance(
                JdbcInboxDeduplicatorTest.class.getClassLoader(), new Class<?>[]{ResultSet.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("next")) return true;
                    if (method.getName().equals("getString")) return "PROCESSED";
                    return defaultValue(method.getReturnType());
                });
        PreparedStatement insert = (PreparedStatement) Proxy.newProxyInstance(
                JdbcInboxDeduplicatorTest.class.getClassLoader(), new Class<?>[]{PreparedStatement.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("executeUpdate")) {
                        calls.add("duplicate");
                        throw new SQLException("duplicate", "23000", 1062);
                    }
                    return defaultValue(method.getReturnType());
                });
        PreparedStatement lookup = (PreparedStatement) Proxy.newProxyInstance(
                JdbcInboxDeduplicatorTest.class.getClassLoader(), new Class<?>[]{PreparedStatement.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("executeQuery")) {
                        calls.add("lookup");
                        return result;
                    }
                    return defaultValue(method.getReturnType());
                });
        int[] statementNumber = {0};
        return (Connection) Proxy.newProxyInstance(
                JdbcInboxDeduplicatorTest.class.getClassLoader(), new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getAutoCommit" -> { return false; }
                        case "setSavepoint" -> { calls.add("savepoint"); return savepoint; }
                        case "prepareStatement" -> { return statementNumber[0]++ == 0 ? insert : lookup; }
                        case "releaseSavepoint" -> { calls.add("release"); return null; }
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
