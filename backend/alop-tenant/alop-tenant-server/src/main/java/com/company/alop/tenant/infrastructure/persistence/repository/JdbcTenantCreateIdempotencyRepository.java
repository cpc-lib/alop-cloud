package com.company.alop.tenant.infrastructure.persistence.repository;

import com.company.alop.tenant.domain.repository.TenantCreateIdempotencyRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;

/** JDBC idempotency adapter for the platform-scoped CreateTenant V2 command. */
public final class JdbcTenantCreateIdempotencyRepository
        implements TenantCreateIdempotencyRepository {
    private static final long PLATFORM_TENANT_ID = 0L;
    private static final long PLATFORM_USER_ID = 0L;
    private static final String API_CODE = "TENANT_CREATE_V2";

    @Override
    public boolean insertPending(Connection connection, long id, String idempotencyKey,
            String requestHash) throws SQLException {
        requireTransaction(connection);
        requireText(idempotencyKey, "Idempotency key");
        requireText(requestHash, "Request hash");
        String sql = """
                INSERT INTO idempotency_record
                  (id, tenant_id, user_id, api_code, idempotency_key, request_hash,
                   status, expire_at, created_at)
                VALUES (?, 0, 0, 'TENANT_CREATE_V2', ?, ?, 'PENDING',
                        DATE_ADD(UTC_TIMESTAMP(3), INTERVAL 3650 DAY), UTC_TIMESTAMP(3))
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            statement.setString(2, idempotencyKey);
            statement.setString(3, requestHash);
            statement.executeUpdate();
            return true;
        } catch (SQLException failure) {
            if (isDuplicateKey(failure)) {
                return false;
            }
            throw failure;
        }
    }

    @Override
    public Optional<Entry> findByKeyForUpdate(Connection connection, String idempotencyKey)
            throws SQLException {
        requireTransaction(connection);
        requireText(idempotencyKey, "Idempotency key");
        String sql = """
                SELECT request_hash, status, response_code, response_body
                  FROM idempotency_record
                 WHERE tenant_id=? AND user_id=? AND api_code=? AND idempotency_key=?
                 FOR UPDATE
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, PLATFORM_TENANT_ID);
            statement.setLong(2, PLATFORM_USER_ID);
            statement.setString(3, API_CODE);
            statement.setString(4, idempotencyKey);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return Optional.empty();
                }
                return Optional.of(new Entry(result.getString("request_hash"),
                        result.getString("status"), result.getString("response_code"),
                        result.getString("response_body")));
            }
        }
    }

    @Override
    public void complete(Connection connection, String idempotencyKey, String requestHash,
            String responseCode, String responseBody) throws SQLException {
        requireTransaction(connection);
        requireText(idempotencyKey, "Idempotency key");
        requireText(requestHash, "Request hash");
        requireText(responseCode, "Response code");
        requireText(responseBody, "Response body");
        String sql = """
                UPDATE idempotency_record
                   SET request_hash=?, response_code=?, response_body=?, status='COMPLETED'
                 WHERE tenant_id=? AND user_id=? AND api_code=? AND idempotency_key=? AND status='PENDING'
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, requestHash);
            statement.setString(2, responseCode);
            statement.setString(3, responseBody);
            statement.setLong(4, PLATFORM_TENANT_ID);
            statement.setLong(5, PLATFORM_USER_ID);
            statement.setString(6, API_CODE);
            statement.setString(7, idempotencyKey);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("CreateTenant idempotency record is not pending");
            }
        }
    }

    private static void requireTransaction(Connection connection) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        if (connection.getAutoCommit()) {
            throw new IllegalStateException("CreateTenant idempotency requires an explicit transaction");
        }
    }

    static boolean isDuplicateKey(SQLException failure) {
        for (SQLException current = failure; current != null; current = current.getNextException()) {
            if (current.getErrorCode() == 1062) {
                return true;
            }
        }
        return false;
    }

    private static void requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is required");
        }
    }
}
