package com.company.alop.tenant.domain.tenant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.alop.framework.common.error.BusinessException;
import com.company.alop.framework.common.error.CommonErrors;
import org.junit.jupiter.api.Test;

class TenantLifecycleTest {
    @Test
    void followsTheFrozenLifecycleAndIncrementsVersion() {
        Tenant tenant = Tenant.creating(1L, "request-1", "acme", "Acme", "ENTERPRISE", 10L);

        assertEquals(TenantStatus.CREATING, tenant.status());
        assertEquals(0L, tenant.version());

        tenant.provisionSucceeded();
        tenant.suspend();
        tenant.resume();
        tenant.expire();
        tenant.requestTermination();
        tenant.finalizeTermination(TenantTerminationReadiness.ready());

        assertEquals(TenantStatus.TERMINATED, tenant.status());
        assertEquals(6L, tenant.version());
    }

    @Test
    void rejectsInvalidTransitionsAndCannotReopenTerminalTenant() {
        Tenant tenant = Tenant.creating(1L, "request-1", "acme", "Acme", "ENTERPRISE", 10L);

        assertError(CommonErrors.TENANT_INVALID_STATE, tenant::resume);
        tenant.provisionSucceeded();
        assertError(CommonErrors.TENANT_INVALID_STATE, tenant::provisionSucceeded);
        tenant.requestTermination();
        tenant.finalizeTermination(TenantTerminationReadiness.ready());
        assertError(CommonErrors.TENANT_INVALID_STATE, tenant::resume);
    }

    @Test
    void blocksTerminationUntilAllClosureChecksPass() {
        Tenant tenant = Tenant.creating(1L, "request-1", "acme", "Acme", "ENTERPRISE", 10L);
        tenant.provisionSucceeded();
        tenant.requestTermination();

        assertError(CommonErrors.TENANT_TERMINATION_BLOCKED,
                () -> tenant.finalizeTermination(TenantTerminationReadiness.blocked()));
        assertEquals(TenantStatus.TERMINATING, tenant.status());
    }

    @Test
    void onlyActiveTenantAcceptsBusinessTraffic() {
        Tenant tenant = Tenant.creating(1L, "request-1", "acme", "Acme", "ENTERPRISE", 10L);

        assertFalse(tenant.acceptsBusinessTraffic());
        tenant.provisionSucceeded();
        assertTrue(tenant.acceptsBusinessTraffic());
        tenant.suspend();
        assertFalse(tenant.acceptsBusinessTraffic());
    }

    @Test
    void creatingAndRehydratingPreserveTimezoneAndCurrencyWithoutChangingVersioning() {
        Tenant creating = Tenant.creating(1L, "request-1", "acme", "Acme", "ENTERPRISE", 10L,
                "Asia/Tokyo", "USD");
        Tenant rehydrated = Tenant.rehydrate(1L, "request-1", "acme", "Acme", "ENTERPRISE", 10L,
                "Asia/Tokyo", "USD", TenantStatus.CREATING, 0L);

        assertEquals("Asia/Tokyo", creating.timezone());
        assertEquals("USD", creating.currency());
        assertEquals("Asia/Tokyo", rehydrated.timezone());
        assertEquals("USD", rehydrated.currency());
        assertEquals(10L, rehydrated.packageId());
        assertEquals(TenantStatus.CREATING, rehydrated.status());
        assertEquals(0L, rehydrated.version());
    }

    @Test
    void directCreatingFactoryUsesDefaultsAndCannotBypassTenantValidation() {
        Tenant defaults = Tenant.creating(1L, "request-1", "acme", "Acme", "ENTERPRISE", 10L,
                null, null);

        assertEquals("Asia/Shanghai", defaults.timezone());
        assertEquals("CNY", defaults.currency());
        assertThrows(IllegalArgumentException.class, () -> Tenant.creating(1L, "request-1", "acme",
                "Acme", "ENTERPRISE", 10L, " ", "CNY"));
        assertThrows(IllegalArgumentException.class, () -> Tenant.creating(1L, "request-1", "acme",
                "Acme", "ENTERPRISE", 10L, "Asia/Shanghai", "US"));
        assertThrows(IllegalArgumentException.class, () -> Tenant.creating(1L, "request-1",
                "C".repeat(65), "Acme", "ENTERPRISE", 10L));
        assertThrows(IllegalArgumentException.class, () -> Tenant.creating(1L, "request-1", "acme",
                "N".repeat(129), "ENTERPRISE", 10L));
        assertThrows(IllegalArgumentException.class, () -> Tenant.creating(1L, "request-1", "acme",
                "Acme", "T".repeat(33), 10L));
    }

    private static void assertError(com.company.alop.framework.common.error.ErrorDefinition expected,
            org.junit.jupiter.api.function.Executable operation) {
        BusinessException exception = assertThrows(BusinessException.class, operation);
        assertEquals(expected, exception.error());
    }
}
