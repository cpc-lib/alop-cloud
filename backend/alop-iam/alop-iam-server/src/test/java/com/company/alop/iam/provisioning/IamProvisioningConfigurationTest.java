package com.company.alop.iam.provisioning;

import static org.junit.jupiter.api.Assertions.*;

import com.company.alop.iam.infrastructure.config.IamProvisioningConfiguration;
import com.company.alop.iam.interfaces.mq.TenantProvisioningConsumer;
import com.company.alop.framework.common.messaging.InternalEventContext;
import com.company.alop.framework.common.messaging.InternalEventContextAutoConfiguration;
import com.rabbitmq.client.Channel;
import java.util.Arrays;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

class IamProvisioningConfigurationTest {
    @Test void topologyProvidesDurableQueueAndQuarantineWithoutCredentials() {
        Declarables topology = new IamProvisioningConfiguration().iamProvisioningTopology("alop.events", "iam.provisioning");
        var queues = topology.getDeclarablesByType(Queue.class);
        assertEquals(2, queues.size());
        Queue queue = queues.stream().filter(q -> q.getName().equals("iam.provisioning")).findFirst().orElseThrow();
        assertTrue(queue.isDurable());
        assertFalse(queue.isAutoDelete());
        assertEquals("iam.provisioning.quarantine", queue.getArguments().get("x-dead-letter-routing-key"));
        assertEquals("", queue.getArguments().get("x-dead-letter-exchange"));
    }

    @Test void springListenerUsesManualAck() throws Exception {
        var method = TenantProvisioningConsumer.class.getMethod("receive", org.springframework.amqp.core.Message.class, Channel.class);
        RabbitListener listener = method.getAnnotation(RabbitListener.class);
        assertEquals("MANUAL", listener.ackMode());
        assertTrue(Arrays.stream(listener.queues()).anyMatch(q -> q.contains("alop.iam.provisioning.queue")));
    }

    @Test void configurationInjectsStandardDataSourceAndAllowlistedProvider() throws Exception {
        try (ProvisioningFixture fixture = new ProvisioningFixture();
                AnnotationConfigApplicationContext spring = new AnnotationConfigApplicationContext()) {
            spring.getEnvironment().getPropertySources().addFirst(new MapPropertySource("iam", Map.of(
                    "alop.messaging.internal.service-name", "alop-iam", "alop.iam.worker-id", "34")));
            spring.registerBean(DataSource.class, () -> fixture.dataSource);
            spring.registerBean("rabbitListenerContainerFactory", SimpleRabbitListenerContainerFactory.class, () -> {
                SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
                factory.setConnectionFactory(new CachingConnectionFactory("127.0.0.1"));
                factory.setAutoStartup(false);
                return factory;
            });
            spring.register(IamProvisioningConfiguration.class, InternalEventContextAutoConfiguration.class);
            spring.refresh();
            assertNotNull(spring.getBean(TenantProvisioningConsumer.class));
            spring.getBean(InternalEventContext.Provider.class).context().requireService("alop-iam");
        }
    }
}
