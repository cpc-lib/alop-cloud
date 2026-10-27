package com.company.alop.framework.inbox;

import com.company.alop.framework.common.error.BusinessException;
import com.company.alop.framework.common.id.SnowflakeIdGenerator;
import com.company.alop.framework.common.messaging.InternalEventContext;
import com.company.alop.framework.common.tenant.TenantContext;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
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
    private static final InternalEventContext INTERNAL = new InternalEventContext("alop-iam");
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
    void ordinaryConsumeStillRejectsMismatchedTenant() {
        TenantContext user = new TenantContext(7, 70, 700);

        BusinessException failure = assertThrows(BusinessException.class,
                () -> inbox.consume(connection(false, new ArrayList<>()), user, 42,
                        "group", UUID.randomUUID().toString(), null, db -> { }));

        assertEquals("TENANT_ACCESS_DENIED", failure.error().code());
        assertFalse(failure.error().retryable());
    }

    private static Connection connection(boolean autoCommit, List<String> calls) {
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
                        case "rollback" -> { calls.add("rollback"); return null; }
                        case "releaseSavepoint" -> { calls.add("release"); return null; }
                        default -> { return defaultValue(method.getReturnType()); }
                    }
                });
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
