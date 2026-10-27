package com.company.alop.framework.common.messaging;

import java.util.Objects;

/**
 * Trusted service identity for background event processing, not tenant membership.
 * A provider is created only by trusted framework configuration and then injected
 * into the internal consumer path; HTTP headers and JWT claims are not sources.
 */
public final class InternalEventContext {
    private final String serviceName;

    private InternalEventContext(Provider provider) {
        this.serviceName = provider.serviceName;
    }

    public String serviceName() {
        return serviceName;
    }

    /** Package-private factory reserved for trusted framework configuration. */
    static Provider provider(String serviceName) {
        return new Provider(serviceName);
    }

    public static final class Provider {
        private final String serviceName;

        Provider(String serviceName) {
            Objects.requireNonNull(serviceName, "serviceName");
            if (serviceName.isBlank()) {
                throw new IllegalArgumentException("Service identity must not be blank");
            }
            this.serviceName = serviceName;
        }

        public InternalEventContext context() {
            return new InternalEventContext(this);
        }
    }
}
