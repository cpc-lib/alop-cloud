package com.company.alop.framework.common.messaging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InternalEventContextTest {
    @Test
    void controlledProviderRequiresNonBlankServiceIdentity() {
        assertThrows(NullPointerException.class, () -> InternalEventContext.provider(null));
        assertThrows(IllegalArgumentException.class, () -> InternalEventContext.provider(""));
        assertThrows(IllegalArgumentException.class, () -> InternalEventContext.provider("  "));
        assertThrows(IllegalArgumentException.class,
                () -> InternalEventContext.provider("untrusted-service"));
    }

    @Test
    void onlyControlledProviderIssuesTheTrustedContext() {
        InternalEventContext.Provider provider = InternalEventContext.provider("alop-tenant");

        assertEquals("alop-tenant", provider.context().serviceName());
        assertEquals(0, InternalEventContext.class.getConstructors().length);
        assertEquals(0, InternalEventContext.Provider.class.getConstructors().length);
    }

    @ParameterizedTest
    @ValueSource(strings = {"alop-tenant", "alop-iam"})
    void requireServiceChecksTheCapabilityIdentity(String serviceName) {
        InternalEventContext context = InternalEventContext.provider(serviceName).context();

        assertDoesNotThrow(() -> context.requireService(serviceName));
        assertThrows(IllegalArgumentException.class, () -> context.requireService(
                serviceName.equals("alop-tenant") ? "alop-iam" : "alop-tenant"));
        assertThrows(IllegalArgumentException.class, () -> context.requireService("untrusted-service"));
        assertThrows(IllegalArgumentException.class, () -> context.requireService(" "));
        assertThrows(NullPointerException.class, () -> context.requireService(null));
    }
}
