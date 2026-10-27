package com.company.alop.framework.common.messaging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InternalEventContextAutoConfigurationTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(InternalEventContextAutoConfiguration.class));

    @ParameterizedTest
    @ValueSource(strings = {"alop-tenant", "alop-iam"})
    void createsProviderForAllowlistedService(String serviceName) {
        runner.withPropertyValues("alop.messaging.internal.service-name=" + serviceName)
                .run(context -> {
                    assertNull(context.getStartupFailure());
                    InternalEventContext.Provider provider =
                            context.getBean(InternalEventContext.Provider.class);
                    assertNotNull(provider);
                    assertEquals(serviceName, provider.context().serviceName());
                });
    }

    @ParameterizedTest
    @ValueSource(strings = {"untrusted-service", "false", "FALSE"})
    void rejectsUnknownServiceIdentityDuringStartup(String serviceName) {
        runner.withPropertyValues("alop.messaging.internal.service-name=" + serviceName)
                .run(context -> assertNotNull(context.getStartupFailure()));
    }

    @Test
    void missingServicePropertyDoesNotCreateProvider() {
        runner.run(context -> {
            assertNull(context.getStartupFailure());
            assertTrue(context.getBeansOfType(InternalEventContext.Provider.class).isEmpty());
            new com.company.alop.framework.common.tenant.TenantContext(42, 420, 4200)
                    .requireTenant(42);
        });
    }

    @Test
    void rejectsBlankServiceIdentityWhenConfigured() {
        runner.withPropertyValues("alop.messaging.internal.service-name=   ")
                .run(context -> assertNotNull(context.getStartupFailure()));
    }
}
