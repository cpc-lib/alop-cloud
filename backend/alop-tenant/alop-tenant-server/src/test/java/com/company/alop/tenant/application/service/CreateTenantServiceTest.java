package com.company.alop.tenant.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.alop.tenant.application.command.CreateTenantCommand;
import com.company.alop.tenant.domain.tenant.TenantStatus;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;

class CreateTenantServiceTest {
    @Test
    void rejectsARequestIdentityShorterThanEightCharacters() {
        assertThrows(IllegalArgumentException.class, () -> new CreateTenantCommand(
                "idem-key-1", "short", "tenant-1", "Tenant 1", "ENTERPRISE", "standard",
                1001L, "Asia/Shanghai", "CNY"));
    }

    @Test
    void packageCodeParticipatesInTheCanonicalRequestHash() {
        CreateTenantCommand standard = new CreateTenantCommand(
                "idem-key-1", "request-1", "tenant-1", "Tenant 1", "ENTERPRISE", "standard",
                1001L, "Asia/Shanghai", "CNY");
        CreateTenantCommand premium = new CreateTenantCommand(
                "idem-key-1", "request-1", "tenant-1", "Tenant 1", "ENTERPRISE", "premium",
                1001L, "Asia/Shanghai", "CNY");

        assertNotEquals(standard.canonicalForm(), premium.canonicalForm());
    }

    @Test
    void adminUserIdTimezoneAndCurrencyParticipateInTheCanonicalRequestHash() {
        CreateTenantCommand base = command(1001L, "Asia/Shanghai", "CNY");

        assertNotEquals(CreateTenantService.requestHash(base),
                CreateTenantService.requestHash(command(1002L, "Asia/Shanghai", "CNY")));
        assertNotEquals(CreateTenantService.requestHash(base),
                CreateTenantService.requestHash(command(1001L, "Asia/Tokyo", "CNY")));
        assertNotEquals(CreateTenantService.requestHash(base),
                CreateTenantService.requestHash(command(1001L, "Asia/Shanghai", "USD")));
    }

    @Test
    void canonicalEncodingDoesNotCollideAtFieldBoundaries() {
        CreateTenantCommand first = new CreateTenantCommand(
                "idem-key-1", "request-1", "a\u001fb", "c", "ENTERPRISE", "standard",
                1001L, "Asia/Shanghai", "CNY");
        CreateTenantCommand second = new CreateTenantCommand(
                "idem-key-1", "request-1", "a", "b\u001fc", "ENTERPRISE", "standard",
                1001L, "Asia/Shanghai", "CNY");

        assertNotEquals(CreateTenantService.requestHash(first), CreateTenantService.requestHash(second));
    }

    @Test
    void omittedTimezoneAndCurrencyUseTheV7DefaultsInTheCommand() {
        CreateTenantCommand command = new CreateTenantCommand(
                "idem-key-1", "request-1", "tenant-1", "Tenant 1", "ENTERPRISE", "standard",
                1001L, null, null);

        assertEquals("Asia/Shanghai", command.timezone());
        assertEquals("CNY", command.currency());

        CreateTenantCommand normalized = command(1001L, " Asia/Shanghai ", "usd");
        assertEquals("Asia/Shanghai", normalized.timezone());
        assertEquals("USD", normalized.currency());
    }

    @Test
    void rejectsInvalidAdminUserIdAndExplicitBlankLocaleValues() {
        assertThrows(IllegalArgumentException.class,
                () -> command(0L, "Asia/Shanghai", "CNY"));
        assertThrows(IllegalArgumentException.class,
                () -> command(1001L, " ", "CNY"));
        assertThrows(IllegalArgumentException.class,
                () -> command(1001L, "Asia/Shanghai", " "));
    }

    @Test
    void alignsTenantFieldsWithTheFrozenDatabaseLengths() {
        assertThrows(IllegalArgumentException.class, () -> command(
                "C".repeat(65), "Tenant", "ENTERPRISE"));
        assertThrows(IllegalArgumentException.class, () -> command(
                "tenant", "N".repeat(129), "ENTERPRISE"));
        assertThrows(IllegalArgumentException.class, () -> command(
                "tenant", "Tenant", "T".repeat(33)));
        assertDoesNotThrow(() -> command("C".repeat(64), "N".repeat(128), "T".repeat(32)));
    }

    @Test
    void validatesStoredCreateTenantResultsBeforeReplay() {
        assertThrows(IllegalArgumentException.class,
                () -> new CreateTenantResult(0L, "request-1", TenantStatus.CREATING, 0L));
        assertThrows(IllegalArgumentException.class,
                () -> new CreateTenantResult(1L, "short", TenantStatus.CREATING, 0L));
        assertThrows(IllegalArgumentException.class,
                () -> new CreateTenantResult(1L, "request-1", TenantStatus.ACTIVE, 0L));
        assertThrows(IllegalArgumentException.class,
                () -> new CreateTenantResult(1L, "request-1", TenantStatus.CREATING, -1L));
        assertThrows(IllegalArgumentException.class,
                () -> new CreateTenantResult(1L, "request-1", TenantStatus.CREATING, 1L));
        assertThrows(IllegalArgumentException.class, () -> CreateTenantResult.fromResponseBody(
                "{\"tenantId\":0,\"tenantCreateRequestId\":\"request-1\","
                        + "\"status\":\"CREATING\",\"version\":0}"));
        assertThrows(IllegalArgumentException.class, () -> CreateTenantResult.fromResponseBody(
                "{\"tenantCreateRequestId\":\"request-1\",\"status\":\"CREATING\",\"version\":0}"));
        assertThrows(IllegalArgumentException.class, () -> CreateTenantResult.fromResponseBody(
                "{\"tenantId\":1,\"status\":\"CREATING\",\"version\":0}"));
        assertThrows(IllegalArgumentException.class, () -> CreateTenantResult.fromResponseBody(
                "{\"tenantId\":1,\"tenantCreateRequestId\":\"request-1\",\"version\":0}"));
        assertThrows(IllegalArgumentException.class, () -> CreateTenantResult.fromResponseBody(
                "{\"tenantId\":1,\"tenantCreateRequestId\":\"request-1\","
                        + "\"status\":\"CREATING\"}"));
        assertThrows(IllegalArgumentException.class, () -> CreateTenantResult.fromResponseBody(
                "{\"tenantId\":\"1\",\"tenantCreateRequestId\":\"request-1\","
                        + "\"status\":\"CREATING\",\"version\":0}"));
        assertThrows(IllegalArgumentException.class, () -> CreateTenantResult.fromResponseBody(
                "{\"tenantId\":1,\"tenantCreateRequestId\":1,"
                        + "\"status\":\"CREATING\",\"version\":0}"));
        assertThrows(IllegalArgumentException.class, () -> CreateTenantResult.fromResponseBody(
                "{\"tenantId\":1,\"tenantCreateRequestId\":\"request-1\","
                        + "\"status\":1,\"version\":0}"));
        assertThrows(IllegalArgumentException.class, () -> CreateTenantResult.fromResponseBody(
                "{\"tenantId\":1,\"tenantCreateRequestId\":\"request-1\","
                        + "\"status\":\"CREATING\",\"version\":\"0\"}"));
    }

    @Test
    void recognizesOnlyNamedTenantDuplicateKeysWithMysqlDuplicateErrorCode() {
        assertTrue(CreateTenantService.isTenantIdentityDuplicateKey(
                new SQLException("Duplicate entry for key 'uk_tenant_code'", "23000", 1062)));
        assertTrue(CreateTenantService.isTenantIdentityDuplicateKey(
                new SQLException("Duplicate entry for key 'uk_tenant_create_request_id'", "23000", 1062)));
        assertFalse(CreateTenantService.isTenantIdentityDuplicateKey(
                new SQLException("Duplicate entry for key 'uk_other'", "23000", 1062)));
        assertFalse(CreateTenantService.isTenantIdentityDuplicateKey(
                new SQLException("Integrity error for key 'uk_tenant_code'", "23000", 1063)));
    }

    private static CreateTenantCommand command(long adminUserId, String timezone, String currency) {
        return new CreateTenantCommand("idem-key-1", "request-1", "tenant-1", "Tenant 1",
                "ENTERPRISE", "standard", adminUserId, timezone, currency);
    }

    private static CreateTenantCommand command(String tenantCode, String tenantName, String tenantType) {
        return new CreateTenantCommand("idem-key-1", "request-1", tenantCode, tenantName,
                tenantType, "standard", 1001L, "Asia/Shanghai", "CNY");
    }
}
