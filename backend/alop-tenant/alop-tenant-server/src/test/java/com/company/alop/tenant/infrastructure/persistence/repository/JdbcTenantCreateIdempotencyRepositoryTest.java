package com.company.alop.tenant.infrastructure.persistence.repository;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;
import org.junit.jupiter.api.Test;

class JdbcTenantCreateIdempotencyRepositoryTest {
    @Test
    void onlyMysqlDuplicateKeyErrorCodeIsRetryable() {
        assertTrue(JdbcTenantCreateIdempotencyRepository.isDuplicateKey(
                new SQLException("Duplicate entry", "23000", 1062)));
        assertFalse(JdbcTenantCreateIdempotencyRepository.isDuplicateKey(
                new SQLException("Integrity error", "23000", 1063)));
        assertFalse(JdbcTenantCreateIdempotencyRepository.isDuplicateKey(
                new SQLException("Integrity error", "23000", 0)));
    }
}
