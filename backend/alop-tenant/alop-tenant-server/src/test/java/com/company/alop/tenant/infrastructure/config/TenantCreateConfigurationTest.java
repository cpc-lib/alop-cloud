package com.company.alop.tenant.infrastructure.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.alop.framework.common.messaging.InternalEventContextAutoConfiguration;
import com.company.alop.tenant.application.service.CreateTenantService;
import com.company.alop.tenant.infrastructure.persistence.repository.JdbcTenantCreateIdempotencyRepository;
import com.company.alop.tenant.infrastructure.persistence.repository.JdbcTenantPackageRepository;
import com.company.alop.tenant.infrastructure.persistence.repository.JdbcTenantRepository;
import com.company.alop.framework.common.id.SnowflakeIdGenerator;
import java.util.Map;
import java.util.function.LongSupplier;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class TenantCreateConfigurationTest {
    @Test
    void configuredWorkerIdWiresCreateDependencies() {
        try (AnnotationConfigApplicationContext context = contextWithWorkerId("7")) {
            assertTrue(context.getBean(SnowflakeIdGenerator.class).nextId() > 0);
            assertEquals(3, context.getBeansOfType(LongSupplier.class).size());
            assertTrue(context.getBean(JdbcTenantRepository.class) != null);
            assertTrue(context.getBean(JdbcTenantPackageRepository.class) != null);
            assertTrue(context.getBean(JdbcTenantCreateIdempotencyRepository.class) != null);
            assertTrue(context.getBean(CreateTenantService.class) != null);
        }
    }

    @Test
    void missingWorkerIdFailsStartup() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(DataSource.class,
                    (java.util.function.Supplier<DataSource>) DriverManagerDataSource::new);
            context.register(TenantCreateConfiguration.class);

            assertThrows(UnsatisfiedDependencyException.class, context::refresh);
        }
    }

    private static AnnotationConfigApplicationContext contextWithWorkerId(String workerId) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource(
                "test", Map.of("alop.tenant.worker-id", workerId,
                        "alop.messaging.internal.service-name", "alop-tenant")));
        context.registerBean(DataSource.class,
                (java.util.function.Supplier<DataSource>) DriverManagerDataSource::new);
        context.register(InternalEventContextAutoConfiguration.class);
        context.register(TenantCreateConfiguration.class);
        context.refresh();
        return context;
    }
}
