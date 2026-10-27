package com.company.alop.iam.provisioning;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.lang.reflect.Proxy;
import java.lang.reflect.InvocationTargetException;
import com.company.alop.framework.mq.RabbitEventEnvelope;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TenantProvisioningServiceTest {
    private ProvisioningFixture f;
    @BeforeEach void setup() throws Exception { f = new ProvisioningFixture(); }
    @AfterEach void close() throws Exception { f.close(); }

    @Test void activeUserGetsExactlyThreePermissionsAndCompletionWithPreservedIdentity() throws Exception {
        f.process(f.event());
        assertEquals(1, f.count("tenant_user_membership"));
        assertEquals("ACTIVE", f.value("SELECT status FROM tenant_user_membership"));
        assertEquals(1, f.count("role"));
        assertEquals(3, f.count("role_permission"));
        assertEquals(1, f.count("user_role"));
        assertEquals(0, f.count("integration_task"));
        ObjectNode event = f.payload();
        assertEquals("iam.tenant.provisioning.completed.v1", event.path("eventType").asText());
        assertEquals("1.0", event.path("eventVersion").asText());
        assertEquals("Tenant", event.path("aggregateType").asText());
        assertEquals("42", event.path("aggregateId").asText());
        assertEquals(0, event.path("aggregateVersion").asLong());
        assertEquals("trace-42", event.path("traceId").asText());
        assertEquals("correlation-42", event.path("correlationId").asText());
        assertEquals("create-42", event.path("data").path("tenantCreateRequestId").asText());
        assertEquals(Set.of("tenantId", "adminUserId", "membershipId", "adminRoleId", "tenantCreateRequestId"),
                new java.util.HashSet<>(java.util.stream.StreamSupport.stream(
                        java.util.Spliterators.spliteratorUnknownSize(event.path("data").fieldNames(), 0), false).toList()));
    }

    @ParameterizedTest @ValueSource(strings = {"missing", "DISABLED", "LOCKED"})
    void unavailablePlatformUserFailsClosedWithoutDisclosingExistence(String state) throws Exception {
        f.sql(state.equals("missing") ? "DELETE FROM platform_user" : "UPDATE platform_user SET status='" + state + "'");
        f.process(f.event());
        assertFailure("TENANT_ADMIN_USER_UNAVAILABLE");
        assertEquals(0, f.count("tenant_user_membership"));
        assertEquals(0, f.count("role"));
    }

    @Test void reusedMembershipAndExactRoleKeepStableFieldsAndUserRoleIsIdempotent() throws Exception {
        f.sql("INSERT INTO tenant_user_membership(id,tenant_id,user_id,employee_no,display_name,status,version) "
                + "VALUES(90,42,7,'E7','Existing Name','ACTIVE',4)");
        seedRole();
        f.process(f.event());
        f.process(f.event().put("eventId", "another-event"));
        assertEquals(1, f.count("tenant_user_membership"));
        assertEquals("Existing Name", f.value("SELECT display_name FROM tenant_user_membership"));
        assertEquals("E7", f.value("SELECT employee_no FROM tenant_user_membership"));
        assertEquals("4", f.value("SELECT version FROM tenant_user_membership"));
        assertEquals(1, f.count("role"));
        assertEquals(3, f.count("role_permission"));
        assertEquals(1, f.count("user_role"));
        assertEquals(90, f.payload().path("data").path("membershipId").asLong());
    }

    @ParameterizedTest @ValueSource(strings = {"extra", "missing", "inactive", "orphan"})
    void conflictingRoleIsNeverRepairedOrGranted(String conflict) throws Exception {
        seedRole();
        switch (conflict) {
            case "extra" -> { f.sql("INSERT INTO permission VALUES(4,'payment:execute','Payments','payment')"); f.sql("INSERT INTO role_permission VALUES(80,4)"); }
            case "missing" -> f.sql("DELETE FROM role_permission WHERE permission_id=3");
            case "inactive" -> f.sql("UPDATE role SET status='DISABLED'");
            case "orphan" -> f.sql("INSERT INTO role_permission VALUES(80,999)");
        }
        int before = f.count("role_permission");
        f.process(f.event());
        assertFailure("TENANT_ADMIN_ROLE_CONFLICT");
        assertEquals(before, f.count("role_permission"));
        assertEquals(0, f.count("user_role"));
        assertEquals(0, f.count("tenant_user_membership"));
    }

    @Test void missingPermissionCatalogFailsWithoutCreatingPartialIamState() throws Exception {
        f.sql("DELETE FROM permission WHERE id=3");
        f.process(f.event());
        assertFailure("TENANT_ADMIN_PERMISSION_UNAVAILABLE");
        assertEquals(0, f.count("role"));
        assertEquals(0, f.count("tenant_user_membership"));
    }

    @Test void existingRoleWithDeletedCatalogPermissionIsAStableRoleConflict() throws Exception {
        seedRole();
        f.sql("DELETE FROM permission WHERE id=3");
        f.process(f.event());
        assertFailure("TENANT_ADMIN_ROLE_CONFLICT");
        assertEquals(3, f.count("role_permission"));
        assertEquals(0, f.count("tenant_user_membership"));
    }

    @Test void disabledMembershipIsNotSilentlyReactivated() throws Exception {
        f.sql("INSERT INTO tenant_user_membership(id,tenant_id,user_id,status) VALUES(90,42,7,'DISABLED')");
        f.process(f.event());
        assertFailure("TENANT_ADMIN_MEMBERSHIP_CONFLICT");
        assertEquals("DISABLED", f.value("SELECT status FROM tenant_user_membership"));
        assertEquals(0, f.count("role"));
    }

    @Test void malformedRecoverableDataCreatesFailureTask() throws Exception {
        ObjectNode event = f.event();
        ((ObjectNode) event.path("data")).put("currency", "invalid");
        f.process(event);
        assertFailure("TENANT_PROVISIONING_INVALID_EVENT");
        assertEquals(0, f.count("tenant_user_membership"));
    }

    @Test void outboxSqlFailureRollsBackAllEffectsAndDoesNotPublishBusinessFailure() throws Exception {
        f.sql("ALTER TABLE mq_outbox ADD CONSTRAINT reject_outbox CHECK (id < 0)");
        assertThrows(SQLException.class, () -> f.process(f.event()));
        for (String table : new String[] {"tenant_user_membership", "role", "role_permission", "user_role", "mq_outbox", "integration_task"}) assertEquals(0, f.count(table), table);
    }

    @Test void sqlCleanupFailureDuringUserRejectionRemainsRetryable() throws Exception {
        f.sql("UPDATE platform_user SET status='DISABLED'");
        try (Connection db = f.dataSource.getConnection()) {
            db.setAutoCommit(false);
            Connection wrapped = (Connection) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {Connection.class},
                    (proxy, method, args) -> {
                        try {
                            Object result = method.invoke(db, args);
                            if (method.getName().equals("prepareStatement") && ((String) args[0]).contains("FROM platform_user")) {
                                return Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {PreparedStatement.class},
                                        (statement, operation, parameters) -> {
                                            if (operation.getName().equals("close")) {
                                                ((PreparedStatement) result).close();
                                                throw new SQLException("resource cleanup failed");
                                            }
                                            try { return operation.invoke(result, parameters); }
                                            catch (InvocationTargetException failure) { throw failure.getCause(); }
                                        });
                            }
                            return result;
                        } catch (InvocationTargetException failure) { throw failure.getCause(); }
                    });
            assertThrows(SQLException.class, () -> f.service.provision(wrapped,
                    RabbitEventEnvelope.parse(f.json.writeValueAsBytes(f.event()))));
            db.rollback();
        }
        assertEquals(0, f.count("mq_outbox"));
        assertEquals(0, f.count("integration_task"));
    }

    private void seedRole() throws Exception {
        f.sql("INSERT INTO role VALUES(80,42,'TENANT_ADMIN','Tenant Administrator','ACTIVE',1)");
        f.sql("INSERT INTO role_permission VALUES(80,1),(80,2),(80,3)");
    }

    private void assertFailure(String code) throws Exception {
        assertEquals(1, f.count("mq_outbox"));
        ObjectNode event = f.payload();
        assertEquals("iam.tenant.provisioning.failed.v1", event.path("eventType").asText());
        assertEquals(code, event.path("data").path("errorCode").asText());
        assertFalse(event.path("data").path("retryable").asBoolean());
        assertEquals(1, f.count("integration_task"));
        assertEquals("TENANT_PROVISIONING", f.value("SELECT task_type FROM integration_task"));
        assertEquals("TENANT", f.value("SELECT business_type FROM integration_task"));
        assertEquals("42", f.value("SELECT business_id FROM integration_task"));
        assertEquals("request-event-1", f.value("SELECT event_id FROM integration_task"));
        assertEquals("1", f.value("SELECT manual_required FROM integration_task"));
        assertFalse(event.toString().contains("password"));
        assertFalse(event.toString().contains("SQL"));
    }
}
