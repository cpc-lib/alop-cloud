package com.company.alop.framework.outbox;

import com.company.alop.framework.common.messaging.InternalEventContext;
import com.company.alop.framework.common.tenant.TenantContext;
import com.company.alop.framework.common.error.BusinessException;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcOutboxStoreTest {
    private static final InternalEventContext INTERNAL = context("alop-tenant");
    private final JdbcOutboxStore store = new JdbcOutboxStore();

    @Test
    void internalEnqueueUsesExplicitTransactionWithoutMembershipContext() throws Exception {
        Map<Integer, Object> parameters = new HashMap<>();
        Connection connection = connection(false, parameters);
        OutboxEvent event = event(42);

        store.enqueue(connection, INTERNAL, event);

        assertEquals(event.id(), parameters.get(1));
        assertEquals(42L, parameters.get(2));
        assertEquals(event.eventId(), parameters.get(3));
    }

    @Test
    void internalEnqueueRequiresTransactionAndValidEventTenant() {
        assertThrows(IllegalStateException.class,
                () -> store.enqueue(connection(true, new HashMap<>()), INTERNAL, event(42)));
        assertThrows(IllegalArgumentException.class,
                () -> event(0));
    }

    @Test
    void internalEnqueueValidatesContextAndPropagatesSqlFailure() {
        assertThrows(NullPointerException.class,
                () -> store.enqueue(connection(false, new HashMap<>()), INTERNAL, null));
        assertThrows(NullPointerException.class,
                () -> store.enqueue(connection(false, new HashMap<>()),
                        (InternalEventContext) null, event(42)));

        Map<Integer, Object> parameters = new HashMap<>();
        assertThrows(SQLException.class,
                () -> store.enqueue(connection(false, parameters,
                        new SQLException("insert failed")), INTERNAL, event(42)));
        assertTrue(parameters.containsKey(1), "valid event reached JDBC binding before SQL failure");
    }

    @Test
    void ordinaryEnqueueStillRejectsMismatchedTenant() {
        TenantContext user = new TenantContext(7, 70, 700);

        assertThrows(BusinessException.class,
                () -> store.enqueue(connection(false, new HashMap<>()), user, event(42)));
    }

    private static OutboxEvent event(long tenantId) {
        return new OutboxEvent(1, tenantId, UUID.randomUUID().toString(),
                "Tenant", "42", 0, "tenant.provisioning.requested.v1", "1.0", "{}");
    }

    private static Connection connection(boolean autoCommit, Map<Integer, Object> parameters) {
        return connection(autoCommit, parameters, null);
    }

    private static Connection connection(boolean autoCommit, Map<Integer, Object> parameters,
            SQLException executeFailure) {
        PreparedStatement statement = (PreparedStatement) Proxy.newProxyInstance(
                JdbcOutboxStoreTest.class.getClassLoader(), new Class<?>[]{PreparedStatement.class},
                (proxy, method, args) -> {
                    if (method.getName().startsWith("set") && args != null) {
                        parameters.put((Integer) args[0], args[1]);
                        return null;
                    }
                    if (method.getName().equals("executeUpdate")) {
                        if (executeFailure != null) throw executeFailure;
                        return 1;
                    }
                    return defaultValue(method.getReturnType());
                });
        return (Connection) Proxy.newProxyInstance(
                JdbcOutboxStoreTest.class.getClassLoader(), new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getAutoCommit")) return autoCommit;
                    if (method.getName().equals("prepareStatement")) return statement;
                    return defaultValue(method.getReturnType());
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
