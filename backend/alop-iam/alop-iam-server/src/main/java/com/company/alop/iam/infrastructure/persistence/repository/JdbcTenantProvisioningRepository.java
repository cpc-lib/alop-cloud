package com.company.alop.iam.infrastructure.persistence.repository;

import com.company.alop.iam.application.service.TenantProvisioningEventParser.Request;
import com.company.alop.iam.application.service.TenantProvisioningRejected;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.LongSupplier;

/** IAM-only writes on the caller's Inbox transaction. No platform authorization grants. */
public final class JdbcTenantProvisioningRepository {
    private static final Set<String> ADMIN_PERMISSIONS = Set.of(
            "tenant:user:manage", "tenant:role:manage", "organization:team:manage");
    private final LongSupplier ids;

    public JdbcTenantProvisioningRepository(LongSupplier ids) { this.ids = ids; }
    public record Provisioned(long membershipId, long roleId) {}
    private record Row(long id, String status) {}

    public Provisioned provision(Connection db, Request request) throws SQLException {
        transaction(db);
        try (PreparedStatement statement = db.prepareStatement("SELECT status FROM platform_user WHERE id=? FOR UPDATE")) {
            statement.setLong(1, request.adminUserId());
            try (var rows = statement.executeQuery()) {
                if (!rows.next() || !"ACTIVE".equals(rows.getString(1))) reject("TENANT_ADMIN_USER_UNAVAILABLE");
            }
        }
        Row membership = membership(db, request);
        if (membership == null) {
            try (PreparedStatement statement = db.prepareStatement("""
                    INSERT INTO tenant_user_membership
                    (id,tenant_id,user_id,employee_no,display_name,status,version)
                    VALUES (?,?,?,NULL,'Tenant Administrator','ACTIVE',0)
                    """)) {
                statement.setLong(1, ids.getAsLong());
                statement.setLong(2, request.tenantId());
                statement.setLong(3, request.adminUserId());
                statement.executeUpdate();
            } catch (SQLException collision) {
                if (collision.getErrorCode() != 1062 || collision.getSuppressed().length > 0
                        || membership(db, request) == null) throw collision;
            }
            membership = membership(db, request);
        }
        if (membership == null) throw new SQLException("Membership insert was lost");
        if (!"ACTIVE".equals(membership.status())) reject("TENANT_ADMIN_MEMBERSHIP_CONFLICT");

        Row role = role(db, request.tenantId());
        boolean created = false;
        if (role == null) {
            try (PreparedStatement statement = db.prepareStatement("""
                    INSERT INTO role(id,tenant_id,role_code,role_name,status,risk_level)
                    VALUES (?,?,'TENANT_ADMIN','Tenant Administrator','ACTIVE',1)
                    """)) {
                statement.setLong(1, ids.getAsLong());
                statement.setLong(2, request.tenantId());
                statement.executeUpdate();
                created = true;
            } catch (SQLException collision) {
                if (collision.getErrorCode() != 1062 || collision.getSuppressed().length > 0
                        || role(db, request.tenantId()) == null) throw collision;
            }
            role = role(db, request.tenantId());
        }
        if (role == null) throw new SQLException("Role insert was lost");
        if (!"ACTIVE".equals(role.status())) reject("TENANT_ADMIN_ROLE_CONFLICT");
        if (created) {
            Map<String, Long> permissions = new HashMap<>();
            try (PreparedStatement statement = db.prepareStatement("""
                    SELECT permission_code,id FROM permission
                    WHERE permission_code IN (?,?,?) FOR UPDATE
                    """)) {
                int index = 1;
                for (String code : ADMIN_PERMISSIONS) statement.setString(index++, code);
                try (var rows = statement.executeQuery()) {
                    while (rows.next()) permissions.put(rows.getString(1), rows.getLong(2));
                }
            }
            if (!permissions.keySet().equals(ADMIN_PERMISSIONS)) reject("TENANT_ADMIN_PERMISSION_UNAVAILABLE");
            try (PreparedStatement statement = db.prepareStatement("INSERT INTO role_permission(role_id,permission_id) VALUES (?,?)")) {
                for (long permissionId : permissions.values()) {
                    statement.setLong(1, role.id());
                    statement.setLong(2, permissionId);
                    statement.executeUpdate();
                }
            }
        }
        Set<String> granted = new HashSet<>();
        try (PreparedStatement statement = db.prepareStatement("""
                SELECT p.permission_code FROM role_permission rp
                LEFT JOIN permission p ON p.id=rp.permission_id WHERE rp.role_id=? FOR UPDATE
                """)) {
            statement.setLong(1, role.id());
            try (var rows = statement.executeQuery()) { while (rows.next()) granted.add(rows.getString(1)); }
        }
        if (!granted.equals(ADMIN_PERMISSIONS)) reject("TENANT_ADMIN_ROLE_CONFLICT");
        try (PreparedStatement statement = db.prepareStatement("""
                INSERT INTO user_role(tenant_id,membership_id,role_id) VALUES (?,?,?)
                ON DUPLICATE KEY UPDATE membership_id=membership_id
                """)) {
            statement.setLong(1, request.tenantId());
            statement.setLong(2, membership.id());
            statement.setLong(3, role.id());
            statement.executeUpdate();
        }
        return new Provisioned(membership.id(), role.id());
    }

    public void recordFailure(Connection db, Request request, String eventId, String code) throws SQLException {
        transaction(db);
        try (PreparedStatement statement = db.prepareStatement("""
                INSERT INTO integration_task
                (id,tenant_id,task_type,business_type,business_id,event_id,trace_id,status,retry_count,
                 last_error_code,last_error_message,manual_required,created_at,updated_at)
                VALUES (?,?,'TENANT_PROVISIONING','TENANT',?,?,?,'OPEN',0,?,
                        'Tenant administrator provisioning requires manual review',1,UTC_TIMESTAMP(3),UTC_TIMESTAMP(3))
                """)) {
            statement.setLong(1, ids.getAsLong());
            statement.setLong(2, request.tenantId());
            statement.setLong(3, request.tenantId());
            statement.setString(4, eventId);
            statement.setString(5, request.traceId().length() <= 128 ? request.traceId() : null);
            statement.setString(6, code);
            statement.executeUpdate();
        }
    }

    private static Row membership(Connection db, Request request) throws SQLException {
        try (PreparedStatement statement = db.prepareStatement("SELECT id,status FROM tenant_user_membership WHERE tenant_id=? AND user_id=? FOR UPDATE")) {
            statement.setLong(1, request.tenantId());
            statement.setLong(2, request.adminUserId());
            try (var rows = statement.executeQuery()) { return rows.next() ? new Row(rows.getLong(1), rows.getString(2)) : null; }
        }
    }

    private static Row role(Connection db, long tenantId) throws SQLException {
        try (PreparedStatement statement = db.prepareStatement("SELECT id,status FROM role WHERE tenant_id=? AND role_code='TENANT_ADMIN' FOR UPDATE")) {
            statement.setLong(1, tenantId);
            try (var rows = statement.executeQuery()) { return rows.next() ? new Row(rows.getLong(1), rows.getString(2)) : null; }
        }
    }

    private static void transaction(Connection db) throws SQLException {
        if (db.getAutoCommit()) throw new IllegalStateException("Provisioning requires an explicit transaction");
    }
    private static void reject(String code) { throw new TenantProvisioningRejected(code); }
}
