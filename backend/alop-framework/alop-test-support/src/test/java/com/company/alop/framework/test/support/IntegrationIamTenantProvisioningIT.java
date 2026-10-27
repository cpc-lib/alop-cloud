package com.company.alop.framework.test.support;

import static org.junit.jupiter.api.Assertions.*;

import com.company.alop.framework.common.id.SnowflakeIdGenerator;
import com.company.alop.framework.common.messaging.InternalEventContext;
import com.company.alop.framework.common.messaging.InternalEventContextAutoConfiguration;
import com.company.alop.framework.inbox.JdbcInboxDeduplicator;
import com.company.alop.framework.outbox.JdbcOutboxStore;
import com.company.alop.iam.application.service.TenantProvisioningService;
import com.company.alop.iam.infrastructure.persistence.repository.JdbcTenantProvisioningRepository;
import com.company.alop.iam.interfaces.mq.TenantProvisioningConsumer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mysql.cj.jdbc.MysqlDataSource;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Delivery;
import com.rabbitmq.client.Envelope;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

/** Uses the existing disposable MySQL harness fixture: frozen IAM + integration V1 tables. */
class IntegrationIamTenantProvisioningIT {
    private static MysqlDataSource source;
    private static AnnotationConfigApplicationContext context;
    private static TenantProvisioningConsumer consumer;
    private static final ObjectMapper JSON = new ObjectMapper();
    private final List<String> acks = new ArrayList<>();

    @BeforeAll static void init() {
        String url = System.getProperty("alop.mysql.jdbcUrl");
        String password = System.getProperty("alop.mysql.password");
        if (url == null || !url.matches("jdbc:mysql://127\\.0\\.0\\.1:[0-9]+/alop_schema_it(?:\\?.*)?") || password == null) {
            throw new IllegalStateException("Requires disposable loopback alop_schema_it MySQL with frozen IAM/integration migrations");
        }
        source = new MysqlDataSource();
        source.setURL(url); source.setUser("root"); source.setPassword(password);
        context = new AnnotationConfigApplicationContext();
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("iam",
                Map.of("alop.messaging.internal.service-name", "alop-iam")));
        context.register(InternalEventContextAutoConfiguration.class);
        context.refresh();
        var provider = context.getBean(InternalEventContext.Provider.class);
        AtomicLong ids = new AtomicLong(10000);
        var service = new TenantProvisioningService(new JdbcTenantProvisioningRepository(ids::incrementAndGet),
                new JdbcOutboxStore(), ids::incrementAndGet, provider);
        consumer = new TenantProvisioningConsumer(source, new JdbcInboxDeduplicator(new SnowflakeIdGenerator(30)),
                provider, service, "alop-iam.tenant-provisioning");
    }

    @AfterAll static void close() { if (context != null) context.close(); }

    @BeforeEach void seed() throws Exception {
        for (String table : List.of("mq_inbox", "mq_outbox", "integration_task", "user_role", "role_permission", "role", "tenant_user_membership", "permission", "platform_user")) sql("DELETE FROM " + table);
        sql("INSERT INTO platform_user(id,username,password_hash,status) VALUES(7,'admin','unused','ACTIVE')");
        sql("INSERT INTO permission VALUES(1,'tenant:user:manage','Users','tenant'),"
                + "(2,'tenant:role:manage','Roles','tenant'),(3,'organization:team:manage','Teams','organization')");
    }

    @Test void duplicateRequestedEventCommitsExactlyOneMembershipRoleOutboxAndProcessedInbox() throws Exception {
        Delivery delivery = delivery(event());
        assertTrue(consumer.handle(channel(), delivery));
        assertTrue(consumer.handle(channel(), delivery));
        assertEquals(List.of("ack", "ack"), acks);
        for (String table : List.of("tenant_user_membership", "role", "user_role", "mq_outbox", "mq_inbox")) assertEquals("1", value("SELECT COUNT(*) FROM " + table), table);
        assertEquals("3", value("SELECT COUNT(*) FROM role_permission"));
        assertEquals("PROCESSED", value("SELECT status FROM mq_inbox"));
        assertEquals("0", value("SELECT COUNT(*) FROM integration_task"));
        ObjectNode completion = (ObjectNode) JSON.readTree(value("SELECT payload FROM mq_outbox"));
        assertEquals("iam.tenant.provisioning.completed.v1", completion.path("eventType").asText());
        assertEquals("request-42", completion.path("data").path("tenantCreateRequestId").asText());
        assertEquals("trace-42", completion.path("traceId").asText());
        assertEquals("correlation-42", completion.path("correlationId").asText());
        assertEquals(value("SELECT id FROM tenant_user_membership"), completion.path("data").path("membershipId").asText());
        assertEquals(value("SELECT id FROM role"), completion.path("data").path("adminRoleId").asText());
    }

    @Test void failedUserIsProcessedOnceAndCreatesOneManualTaskAndSafeFailedEvent() throws Exception {
        sql("UPDATE platform_user SET status='DISABLED'");
        assertTrue(consumer.handle(channel(), delivery(event())));
        assertTrue(consumer.handle(channel(), delivery(event())));
        assertEquals("1", value("SELECT COUNT(*) FROM integration_task WHERE task_type='TENANT_PROVISIONING' "
                + "AND business_type='TENANT' AND business_id=42 AND event_id='event-42' AND manual_required=1"));
        assertEquals("1", value("SELECT COUNT(*) FROM mq_outbox"));
        assertEquals("PROCESSED", value("SELECT status FROM mq_inbox"));
        assertEquals("0", value("SELECT COUNT(*) FROM tenant_user_membership"));
        assertEquals("iam.tenant.provisioning.failed.v1", value("SELECT event_type FROM mq_outbox"));
        assertEquals("TENANT_ADMIN_USER_UNAVAILABLE", JSON.readTree(value("SELECT payload FROM mq_outbox")).path("data").path("errorCode").asText());
    }

    @Test void roleConflictRollsBackNewMembershipAndLeavesPermissionsUntouched() throws Exception {
        sql("INSERT INTO role VALUES(80,42,'TENANT_ADMIN','Tenant Admin','ACTIVE',1)");
        sql("INSERT INTO role_permission VALUES(80,1),(80,2)");
        assertTrue(consumer.handle(channel(), delivery(event())));
        assertEquals("TENANT_ADMIN_ROLE_CONFLICT", value("SELECT last_error_code FROM integration_task"));
        assertEquals("2", value("SELECT COUNT(*) FROM role_permission"));
        assertEquals("0", value("SELECT COUNT(*) FROM tenant_user_membership"));
        assertEquals("0", value("SELECT COUNT(*) FROM user_role"));
    }

    @Test void outboxFailureRollsBackInboxAndAllEffectsThenRedeliverySucceeds() throws Exception {
        sql("CREATE TRIGGER fail_iam_outbox BEFORE INSERT ON mq_outbox FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='injected outbox failure'");
        try {
            assertFalse(consumer.handle(channel(), delivery(event())));
            assertEquals(List.of("nack:true"), acks);
            for (String table : List.of("tenant_user_membership", "role", "role_permission", "user_role", "mq_inbox", "mq_outbox", "integration_task")) assertEquals("0", value("SELECT COUNT(*) FROM " + table), table);
        } finally { sql("DROP TRIGGER fail_iam_outbox"); }
        assertTrue(consumer.handle(channel(), delivery(event())));
        assertEquals("1", value("SELECT COUNT(*) FROM mq_outbox"));
    }

    @Test void platformUserStatusIsCheckedLiveForEachNewEvent() throws Exception {
        assertTrue(consumer.handle(channel(), delivery(event())));
        sql("UPDATE platform_user SET status='DISABLED'");
        assertTrue(consumer.handle(channel(), delivery(event().put("eventId", "event-43"))));
        assertEquals("1", value("SELECT COUNT(*) FROM mq_outbox WHERE event_type='iam.tenant.provisioning.failed.v1'"));
        assertEquals("1", value("SELECT COUNT(*) FROM user_role"));
    }

    private ObjectNode event() {
        ObjectNode event = JSON.createObjectNode().put("eventId", "event-42").put("eventType", "tenant.provisioning.requested.v1")
                .put("eventVersion", "1.0").put("tenantId", "42").put("aggregateType", "Tenant").put("aggregateId", "42")
                .put("aggregateVersion", 0).put("traceId", "trace-42").put("correlationId", "correlation-42").put("occurredAt", "2026-10-27T00:00:00Z");
        event.putObject("data").put("tenantId", "42").put("adminUserId", 7).put("tenantCreateRequestId", "request-42")
                .put("tenantPackageId", 1).put("timezone", "Asia/Shanghai").put("currency", "CNY");
        return event;
    }

    private Delivery delivery(ObjectNode event) throws Exception {
        return new Delivery(new Envelope(17, false, "alop.events", "tenant.provisioning.requested.v1"),
                new AMQP.BasicProperties.Builder().messageId(event.path("eventId").asText()).type("tenant.provisioning.requested.v1")
                        .headers(Map.of("eventId", event.path("eventId").asText(), "eventType", "tenant.provisioning.requested.v1", "eventVersion", "1.0", "tenantId", "42")).build(), JSON.writeValueAsBytes(event));
    }

    private Channel channel() {
        return (Channel) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {Channel.class}, (proxy, method, args) -> {
            if (method.getName().equals("basicAck")) {
                // A new connection can only see these rows after the handler committed.
                assertEquals("PROCESSED", value("SELECT status FROM mq_inbox WHERE event_id='event-42'"));
                assertNotEquals("0", value("SELECT COUNT(*) FROM mq_outbox"));
                acks.add("ack");
            }
            if (method.getName().equals("basicNack")) acks.add("nack:" + args[2]);
            return null;
        });
    }

    private static void sql(String sql) throws Exception {
        try (Connection db = source.getConnection(); var statement = db.createStatement()) { statement.execute(sql); }
    }
    private static String value(String sql) throws Exception {
        try (Connection db = source.getConnection(); var statement = db.createStatement(); var rows = statement.executeQuery(sql)) {
            assertTrue(rows.next()); return rows.getString(1);
        }
    }
}
