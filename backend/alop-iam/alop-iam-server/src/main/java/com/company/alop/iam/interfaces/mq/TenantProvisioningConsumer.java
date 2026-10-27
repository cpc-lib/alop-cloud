package com.company.alop.iam.interfaces.mq;

import com.company.alop.framework.common.messaging.InternalEventContext;
import com.company.alop.framework.inbox.JdbcInboxDeduplicator;
import com.company.alop.framework.mq.RabbitInboxDeliveryHandler;
import com.company.alop.iam.application.service.TenantProvisioningService;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Delivery;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Envelope;
import java.io.IOException;
import javax.sql.DataSource;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

public final class TenantProvisioningConsumer {
    private final RabbitInboxDeliveryHandler handler;
    private final InternalEventContext.Provider provider;
    private final TenantProvisioningService service;

    public TenantProvisioningConsumer(DataSource dataSource, JdbcInboxDeduplicator inbox,
            InternalEventContext.Provider provider, TenantProvisioningService service, String group) {
        this.provider = provider;
        this.service = service;
        handler = new RabbitInboxDeliveryHandler(dataSource, inbox,
                (db, event) -> { throw new IllegalStateException("Internal provisioning only"); },
                (db, event) -> { throw new IllegalStateException("Internal provisioning only"); }, group);
    }

    public boolean handle(Channel channel, Delivery delivery) throws IOException {
        return handler.handleInternal(channel, delivery, "alop-iam",
                (db, event) -> provider.context(), service::provision);
    }

    @RabbitListener(queues = "${alop.iam.provisioning.queue:alop-iam.tenant-provisioning}", ackMode = "MANUAL")
    public void receive(Message message, Channel channel) throws IOException {
        var p = message.getMessageProperties();
        AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                .messageId(p.getMessageId()).type(p.getType()).headers(p.getHeaders()).build();
        handle(channel, new Delivery(new Envelope(p.getDeliveryTag(), p.isRedelivered(),
                p.getReceivedExchange(), p.getReceivedRoutingKey()), properties, message.getBody()));
    }

    public String subscribe(Channel channel, String queue) throws IOException {
        if (queue == null || queue.isBlank()) throw new IllegalArgumentException("Queue required");
        channel.basicQos(10);
        return channel.basicConsume(queue, false, (tag, delivery) -> handle(channel, delivery), tag -> { });
    }
}
