package com.company.alop.framework.common.messaging;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InternalEventContextTest {
    @Test
    void requiresNonBlankServiceIdentity() {
        assertThrows(NullPointerException.class, () -> new InternalEventContext(null));
        assertThrows(IllegalArgumentException.class, () -> new InternalEventContext(""));
        assertThrows(IllegalArgumentException.class, () -> new InternalEventContext("  "));
    }

    @Test
    void exposesTheTrustedServiceIdentity() {
        assertEquals("alop-tenant", new InternalEventContext("alop-tenant").serviceName());
    }
}
