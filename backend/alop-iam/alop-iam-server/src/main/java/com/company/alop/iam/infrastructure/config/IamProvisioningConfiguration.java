package com.company.alop.iam.infrastructure.config;

import com.company.alop.framework.common.id.SnowflakeIdGenerator;
import com.company.alop.framework.common.messaging.InternalEventContext;
import com.company.alop.framework.inbox.JdbcInboxDeduplicator;
import com.company.alop.framework.outbox.JdbcOutboxStore;
import com.company.alop.iam.application.service.TenantProvisioningEventParser;
import com.company.alop.iam.application.service.TenantProvisioningService;
import com.company.alop.iam.infrastructure.persistence.repository.JdbcTenantProvisioningRepository;
import com.company.alop.iam.interfaces.mq.TenantProvisioningConsumer;
import javax.sql.DataSource;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableRabbit
public class IamProvisioningConfiguration {
    @Bean
    TenantProvisioningConsumer tenantProvisioningConsumer(DataSource dataSource,
            InternalEventContext.Provider provider,
            @Value("${alop.iam.worker-id}") long workerId,
            @Value("${alop.iam.provisioning.group:alop-iam.tenant-provisioning}") String group) {
        SnowflakeIdGenerator ids = new SnowflakeIdGenerator(workerId);
        TenantProvisioningService service = new TenantProvisioningService(
                new JdbcTenantProvisioningRepository(ids::nextId), new JdbcOutboxStore(), ids::nextId, provider);
        return new TenantProvisioningConsumer(dataSource, new JdbcInboxDeduplicator(ids), provider, service, group);
    }

    @Bean
    public Declarables iamProvisioningTopology(
            @Value("${alop.iam.provisioning.exchange:alop.events}") String exchange,
            @Value("${alop.iam.provisioning.queue:alop-iam.tenant-provisioning}") String queueName) {
        TopicExchange events = new TopicExchange(exchange, true, false);
        Queue quarantine = QueueBuilder.durable(queueName + ".quarantine").build();
        Queue requests = QueueBuilder.durable(queueName).deadLetterExchange("")
                .deadLetterRoutingKey(quarantine.getName()).build();
        Binding binding = new Binding(queueName, Binding.DestinationType.QUEUE, exchange,
                TenantProvisioningEventParser.REQUESTED, null);
        return new Declarables(events, requests, quarantine, binding);
    }
}
