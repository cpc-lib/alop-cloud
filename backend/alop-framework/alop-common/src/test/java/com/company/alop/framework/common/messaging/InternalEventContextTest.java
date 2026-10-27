package com.company.alop.framework.common.messaging;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InternalEventContextTest {
    @Test
    void controlledProviderRequiresNonBlankServiceIdentity() {
        assertThrows(NullPointerException.class, () -> InternalEventContext.provider(null));
        assertThrows(IllegalArgumentException.class, () -> InternalEventContext.provider(""));
        assertThrows(IllegalArgumentException.class, () -> InternalEventContext.provider("  "));
    }

    @Test
    void onlyControlledProviderIssuesTheTrustedContext() {
        InternalEventContext.Provider provider = InternalEventContext.provider("alop-tenant");

        assertEquals("alop-tenant", provider.context().serviceName());
        assertEquals(0, InternalEventContext.class.getConstructors().length);
        assertEquals(0, InternalEventContext.Provider.class.getConstructors().length);
    }
}
