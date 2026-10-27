package com.company.alop.tenant.infrastructure.persistence.repository;

import com.company.alop.framework.common.error.BusinessException;
import com.company.alop.framework.common.error.CommonErrors;
import com.company.alop.tenant.domain.repository.TenantRepository;
import com.company.alop.tenant.domain.tenant.Tenant;
import com.company.alop.tenant.domain.tenant.TenantStatus;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Objects;
import java.util.Optional;

/** JDBC adapter for the Tenant schema; callers own the transaction. */
public final class JdbcTenantRepository implements TenantRepository {
    private static final String SELECT = """
            SELECT id, tenant_create_request_id, tenant_code, tenant_name, tenant_type, status,
                   package_id, timezone, currency, version
              FROM tenant WHERE id=?
            """;
    private static final String INSERT = """
            INSERT INTO tenant
              (id, tenant_create_request_id, tenant_code, tenant_name, tenant_type, status, package_id, version,
               timezone, currency, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, 0, ?, ?, UTC_TIMESTAMP(3), UTC_TIMESTAMP(3))
            """;
    private static final String UPDATE_LIFECYCLE = """
            UPDATE tenant
               SET status=?, version=version+1, updated_at=UTC_TIMESTAMP(3)
             WHERE id=? AND version=?
            """;

    @Override
    public Optional<Tenant> findById(Connection connection, long tenantId) throws SQLException {
        requireConnection(connection);
        requireId(tenantId);
        try (PreparedStatement statement = connection.prepareStatement(SELECT)) {
            statement.setLong(1, tenantId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(read(result)) : Optional.empty();
            }
        }
    }

    @Override
    public Optional<Tenant> findByIdForUpdate(Connection connection, long tenantId)
            throws SQLException {
        requireTransaction(connection);
        requireId(tenantId);
        try (PreparedStatement statement = connection.prepareStatement(SELECT + " FOR UPDATE")) {
            statement.setLong(1, tenantId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(read(result)) : Optional.empty();
            }
        }
    }

    @Override
    public void insert(Connection connection, Tenant tenant) throws SQLException {
        requireTransaction(connection);
        Objects.requireNonNull(tenant, "tenant");
        if (tenant.version() != 0) {
            throw new IllegalArgumentException("Only a new Tenant can be inserted");
        }
        try (PreparedStatement statement = connection.prepareStatement(INSERT)) {
            statement.setLong(1, tenant.id());
            statement.setString(2, tenant.tenantCreateRequestId());
            statement.setString(3, tenant.tenantCode());
            statement.setString(4, tenant.tenantName());
            statement.setString(5, tenant.tenantType());
            statement.setString(6, tenant.status().name());
            if (tenant.packageId() == null) {
                statement.setNull(7, Types.BIGINT);
            } else {
                statement.setLong(7, tenant.packageId());
            }
            statement.setString(8, tenant.timezone());
            statement.setString(9, tenant.currency());
            statement.executeUpdate();
        }
    }

    @Override
    public void updateLifecycle(Connection connection, Tenant tenant, long expectedVersion)
            throws SQLException {
        requireTransaction(connection);
        Objects.requireNonNull(tenant, "tenant");
        if (expectedVersion < 0 || tenant.version() != expectedVersion + 1) {
            throw new IllegalArgumentException("Tenant version does not match lifecycle transition");
        }
        try (PreparedStatement statement = connection.prepareStatement(UPDATE_LIFECYCLE)) {
            statement.setString(1, tenant.status().name());
            statement.setLong(2, tenant.id());
            statement.setLong(3, expectedVersion);
            if (statement.executeUpdate() != 1) {
                throw new BusinessException(CommonErrors.OPTIMISTIC_LOCK_CONFLICT);
            }
        }
    }

    private static Tenant read(ResultSet result) throws SQLException {
        Long packageId = result.getObject("package_id", Long.class);
        TenantStatus status;
        try {
            status = TenantStatus.valueOf(result.getString("status"));
        } catch (IllegalArgumentException ex) {
            throw new SQLException("Unknown Tenant status", ex);
        }
        return Tenant.rehydrate(result.getLong("id"), result.getString("tenant_create_request_id"),
                result.getString("tenant_code"), result.getString("tenant_name"),
                result.getString("tenant_type"), packageId, result.getString("timezone"),
                result.getString("currency"), status, result.getLong("version"));
    }

    private static void requireConnection(Connection connection) {
        Objects.requireNonNull(connection, "connection");
    }

    private static void requireTransaction(Connection connection) throws SQLException {
        requireConnection(connection);
        if (connection.getAutoCommit()) {
            throw new IllegalStateException("Tenant lifecycle persistence requires an explicit transaction");
        }
    }

    private static void requireId(long tenantId) {
        if (tenantId <= 0) {
            throw new IllegalArgumentException("Tenant ID must be positive");
        }
    }
}
