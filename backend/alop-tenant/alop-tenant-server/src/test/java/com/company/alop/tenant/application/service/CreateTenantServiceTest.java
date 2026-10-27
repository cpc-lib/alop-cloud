package com.company.alop.tenant.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.company.alop.tenant.application.command.CreateTenantCommand;
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

        assertNotEquals(base.canonicalForm(), command(1002L, "Asia/Shanghai", "CNY").canonicalForm());
        assertNotEquals(base.canonicalForm(), command(1001L, "Asia/Tokyo", "CNY").canonicalForm());
        assertNotEquals(base.canonicalForm(), command(1001L, "Asia/Shanghai", "USD").canonicalForm());
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

    private static CreateTenantCommand command(long adminUserId, String timezone, String currency) {
        return new CreateTenantCommand("idem-key-1", "request-1", "tenant-1", "Tenant 1",
                "ENTERPRISE", "standard", adminUserId, timezone, currency);
    }
}
