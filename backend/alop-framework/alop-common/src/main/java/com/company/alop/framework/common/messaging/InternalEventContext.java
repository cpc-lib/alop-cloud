package com.company.alop.framework.common.messaging;

import java.util.Objects;

/** Trusted service identity for background event processing, not tenant membership. */
public record InternalEventContext(String serviceName) {
    public InternalEventContext {
        Objects.requireNonNull(serviceName, "serviceName");
        if (serviceName.isBlank()) {
            throw new IllegalArgumentException("Service identity must not be blank");
        }
    }
}
