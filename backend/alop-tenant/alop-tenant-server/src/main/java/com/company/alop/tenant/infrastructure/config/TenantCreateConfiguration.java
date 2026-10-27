package com.company.alop.tenant.infrastructure.config;

import com.company.alop.framework.common.id.SnowflakeIdGenerator;
import com.company.alop.framework.common.messaging.InternalEventContext;
import com.company.alop.framework.outbox.JdbcOutboxStore;
import com.company.alop.tenant.application.port.TenantProvisioningRequestedPublisher;
import com.company.alop.tenant.application.service.CreateTenantService;
import com.company.alop.tenant.domain.repository.TenantCreateIdempotencyRepository;
import com.company.alop.tenant.domain.repository.TenantPackageRepository;
import com.company.alop.tenant.domain.repository.TenantRepository;
import com.company.alop.tenant.infrastructure.persistence.repository.JdbcTenantCreateIdempotencyRepository;
import com.company.alop.tenant.infrastructure.persistence.repository.JdbcTenantPackageRepository;
import com.company.alop.tenant.infrastructure.persistence.repository.JdbcTenantRepository;
import com.company.alop.tenant.infrastructure.messaging.JdbcTenantProvisioningRequestedPublisher;
import java.util.function.LongSupplier;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class TenantCreateConfiguration {
    private final SnowflakeIdGenerator idGenerator;

    public TenantCreateConfiguration(
            @Value("${alop.tenant.worker-id}") long workerId) {
        this.idGenerator = new SnowflakeIdGenerator(workerId);
    }

    @Bean
    SnowflakeIdGenerator snowflakeIdGenerator() {
        return idGenerator;
    }

    @Bean
    LongSupplier tenantIdGenerator(SnowflakeIdGenerator generator) {
        return generator::nextId;
    }

    @Bean
    LongSupplier idempotencyIdGenerator(SnowflakeIdGenerator generator) {
        return generator::nextId;
    }

    @Bean
    LongSupplier tenantEventIdGenerator(SnowflakeIdGenerator generator) {
        return generator::nextId;
    }

    @Bean
    JdbcOutboxStore outboxStore() {
        return new JdbcOutboxStore();
    }

    @Bean
    JdbcTenantProvisioningRequestedPublisher tenantProvisioningRequestedPublisher(
            JdbcOutboxStore outboxStore,
            @Qualifier("tenantEventIdGenerator") LongSupplier eventIdGenerator,
            InternalEventContext.Provider contextProvider) {
        return new JdbcTenantProvisioningRequestedPublisher(outboxStore, eventIdGenerator,
                contextProvider);
    }

    @Bean
    JdbcTenantRepository tenantRepository() {
        return new JdbcTenantRepository();
    }

    @Bean
    JdbcTenantPackageRepository tenantPackageRepository() {
        return new JdbcTenantPackageRepository();
    }

    @Bean
    JdbcTenantCreateIdempotencyRepository tenantCreateIdempotencyRepository() {
        return new JdbcTenantCreateIdempotencyRepository();
    }

    @Bean
    CreateTenantService createTenantService(DataSource dataSource,
            TenantRepository tenantRepository, TenantPackageRepository tenantPackageRepository,
            TenantCreateIdempotencyRepository idempotencyRepository,
            @Qualifier("tenantIdGenerator") LongSupplier tenantIdGenerator,
            @Qualifier("idempotencyIdGenerator") LongSupplier idempotencyIdGenerator,
            TenantProvisioningRequestedPublisher provisioningPublisher) {
        return new CreateTenantService(dataSource, tenantRepository, tenantPackageRepository,
                idempotencyRepository, tenantIdGenerator, idempotencyIdGenerator, provisioningPublisher);
    }

}
