package com.company.alop.iam.provisioning;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rabbitmq.client.Channel;
import com.company.alop.framework.common.id.SnowflakeIdGenerator;
import com.company.alop.framework.inbox.JdbcInboxDeduplicator;
import com.company.alop.iam.interfaces.mq.TenantProvisioningConsumer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TenantProvisioningConsumerTest {
    private ProvisioningFixture f;
    private final List<String> acks = new ArrayList<>();
    @BeforeEach void setup() throws Exception { f = new ProvisioningFixture(); }
    @AfterEach void close() throws Exception { f.close(); }

    @Test void ackObservesCommittedIamInboxAndOutbox() throws Exception {
        assertTrue(f.consumer.handle(channel(), f.delivery(f.event())));
        assertEquals(List.of("ack"), acks);
        assertEquals("PROCESSED", f.value("SELECT status FROM mq_inbox"));
    }

    @Test void deterministicFailureCommitsTaskAndFailedOutboxBeforeAck() throws Exception {
        f.sql("UPDATE platform_user SET status='DISABLED'");
        assertTrue(f.consumer.handle(channel(), f.delivery(f.event())));
        assertEquals(List.of("ack"), acks);
        assertEquals("PROCESSED", f.value("SELECT status FROM mq_inbox"));
        assertEquals(1, f.count("integration_task"));
        assertEquals("iam.tenant.provisioning.failed.v1", f.value("SELECT event_type FROM mq_outbox"));
    }

    @Test void outboxFailureRollsBackInboxAndIamAndRequeues() throws Exception {
        f.sql("ALTER TABLE mq_outbox ADD CONSTRAINT reject_outbox CHECK (id < 0)");
        assertFalse(f.consumer.handle(channel(), f.delivery(f.event())));
        assertEquals(List.of("nack:true"), acks);
        for (String table : new String[] {"mq_inbox", "mq_outbox", "tenant_user_membership", "role", "user_role", "integration_task"}) assertEquals(0, f.count(table), table);
    }

    @Test void failureTaskWriteErrorRollsBackInboxAndRequeues() throws Exception {
        f.sql("DELETE FROM platform_user");
        f.sql("ALTER TABLE integration_task ADD CONSTRAINT reject_task CHECK (id < 0)");
        assertFalse(f.consumer.handle(channel(), f.delivery(f.event())));
        assertEquals(List.of("nack:true"), acks);
        assertEquals(0, f.count("mq_inbox"));
        assertEquals(0, f.count("mq_outbox"));
    }

    @Test void sqlReadFailureDoesNotBecomeADeterministicUserFailure() throws Exception {
        f.sql("DROP TABLE platform_user");
        assertFalse(f.consumer.handle(channel(), f.delivery(f.event())));
        assertEquals(List.of("nack:true"), acks);
        assertEquals(0, f.count("mq_inbox"));
        assertEquals(0, f.count("mq_outbox"));
        assertEquals(0, f.count("integration_task"));
    }

    @Test void connectionFailureRequeues() throws Exception {
        DataSource source = (DataSource) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {DataSource.class},
                (proxy, method, args) -> { throw new SQLException("connection unavailable"); });
        var consumer = new TenantProvisioningConsumer(source, new JdbcInboxDeduplicator(new SnowflakeIdGenerator(32)),
                f.provider, f.service, "iam-provisioning");
        assertFalse(consumer.handle(channel(), f.delivery(f.event())));
        assertEquals(List.of("nack:true"), acks);
    }

    @Test void commitFailureRollsBackAndRequeuesWithoutAck() throws Exception {
        DataSource source = (DataSource) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {DataSource.class},
                (proxy, method, args) -> {
                    Connection db = f.dataSource.getConnection();
                    return Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {Connection.class},
                            (connection, operation, parameters) -> {
                                if (operation.getName().equals("commit")) throw new SQLException("commit unavailable");
                                try { return operation.invoke(db, parameters); }
                                catch (InvocationTargetException failure) { throw failure.getCause(); }
                            });
                });
        var consumer = new TenantProvisioningConsumer(source, new JdbcInboxDeduplicator(new SnowflakeIdGenerator(33)),
                f.provider, f.service, "iam-provisioning");
        assertFalse(consumer.handle(channel(), f.delivery(f.event())));
        assertEquals(List.of("nack:true"), acks);
        for (String table : new String[] {"mq_inbox", "mq_outbox", "tenant_user_membership", "role", "role_permission", "user_role"}) assertEquals(0, f.count(table), table);
    }

    @Test void springAdapterPreservesBrokerIdentityAndUsesTheTransactionalHandler() throws Exception {
        var delivery = f.delivery(f.event());
        MessageProperties properties = new MessageProperties();
        properties.setDeliveryTag(17);
        properties.setRedelivered(false);
        properties.setMessageId(delivery.getProperties().getMessageId());
        properties.setType(delivery.getProperties().getType());
        properties.getHeaders().putAll(delivery.getProperties().getHeaders());
        f.consumer.receive(new Message(delivery.getBody(), properties), channel());
        assertEquals(List.of("ack"), acks);
    }

    @Test void headerMismatchIsQuarantinedBeforeInbox() throws Exception {
        var delivery = f.delivery(f.event());
        var headers = new java.util.HashMap<>(delivery.getProperties().getHeaders());
        headers.put("tenantId", "99");
        var malformed = new com.rabbitmq.client.Delivery(delivery.getEnvelope(),
                delivery.getProperties().builder().headers(headers).build(), delivery.getBody());
        assertFalse(f.consumer.handle(channel(), malformed));
        assertEquals(List.of("nack:false"), acks);
        assertEquals(0, f.count("mq_inbox"));
    }

    @ParameterizedTest @ValueSource(strings = {"extraEnvelope", "extraData", "currency", "timezone", "package", "version", "aggregate", "occurredAt", "tenantMismatch"})
    void addressableInvalidEventProducesDeterministicFailure(String invalid) throws Exception {
        ObjectNode event = f.event();
        ObjectNode data = (ObjectNode) event.path("data");
        switch (invalid) {
            case "extraEnvelope" -> event.put("Authorization", "must-never-be-copied");
            case "extraData" -> data.put("permissions", "platform:admin");
            case "currency" -> data.put("currency", "usd");
            case "timezone" -> data.remove("timezone");
            case "package" -> data.put("tenantPackageId", 0);
            case "version" -> event.put("eventVersion", "2.0");
            case "aggregate" -> event.put("aggregateId", "43");
            case "occurredAt" -> event.put("occurredAt", "yesterday");
            case "tenantMismatch" -> data.put("tenantId", "43");
        }
        assertTrue(f.consumer.handle(channel(), f.delivery(event)));
        assertEquals("TENANT_PROVISIONING_INVALID_EVENT", f.payload().path("data").path("errorCode").asText());
        assertFalse(f.payload().toString().contains("Authorization"));
        assertEquals("PROCESSED", f.value("SELECT status FROM mq_inbox"));
        assertEquals(0, f.count("tenant_user_membership"));
    }

    @ParameterizedTest @ValueSource(strings = {"tenantId", "adminUserId", "tenantCreateRequestId"})
    void unaddressableMalformedIdentityIsQuarantinedWithoutInventingFailureIdentity(String field) throws Exception {
        ObjectNode event = f.event();
        if (field.equals("tenantId")) event.put("tenantId", "0");
        else ((ObjectNode) event.path("data")).remove(field);
        assertFalse(f.consumer.handle(channel(), f.delivery(event)));
        assertEquals(List.of("nack:false"), acks);
        assertEquals(0, f.count("mq_inbox"));
        assertEquals(0, f.count("mq_outbox"));
    }

    @Test void subscriptionUsesManualAckAndInternalHandler() throws Exception {
        List<String> calls = new ArrayList<>();
        Channel channel = (Channel) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {Channel.class},
                (proxy, method, args) -> {
                    calls.add(method.getName());
                    if (method.getName().equals("basicConsume")) { assertEquals(false, args[1]); return "consumer"; }
                    return null;
                });
        assertEquals("consumer", f.consumer.subscribe(channel, "iam-provisioning"));
        assertEquals(List.of("basicQos", "basicConsume"), calls);
    }

    private Channel channel() {
        return (Channel) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {Channel.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("basicAck")) {
                        assertEquals(1, f.count("mq_outbox"));
                        assertEquals("PROCESSED", f.value("SELECT status FROM mq_inbox"));
                        acks.add("ack");
                    }
                    if (method.getName().equals("basicNack")) acks.add("nack:" + args[2]);
                    return null;
                });
    }
}
