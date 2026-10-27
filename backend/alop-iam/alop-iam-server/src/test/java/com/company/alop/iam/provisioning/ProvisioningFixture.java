package com.company.alop.iam.provisioning;

import com.company.alop.framework.common.id.SnowflakeIdGenerator;
import com.company.alop.framework.common.messaging.InternalEventContext;
import com.company.alop.framework.common.messaging.InternalEventContextAutoConfiguration;
import com.company.alop.framework.inbox.JdbcInboxDeduplicator;
import com.company.alop.framework.mq.RabbitEventEnvelope;
import com.company.alop.framework.outbox.JdbcOutboxStore;
import com.company.alop.iam.application.service.TenantProvisioningService;
import com.company.alop.iam.infrastructure.persistence.repository.JdbcTenantProvisioningRepository;
import com.company.alop.iam.interfaces.mq.TenantProvisioningConsumer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Delivery;
import com.rabbitmq.client.Envelope;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.h2.jdbcx.JdbcDataSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

public class ProvisioningFixture implements AutoCloseable {
    public final JdbcDataSource dataSource = new JdbcDataSource();
    public final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    public final InternalEventContext.Provider provider;
    public final TenantProvisioningService service;
    public final TenantProvisioningConsumer consumer;
    public final AtomicLong ids = new AtomicLong(1000);
    public final ObjectMapper json = new ObjectMapper();

    public ProvisioningFixture() throws Exception {
        dataSource.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=ROLE");
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("iam",
                Map.of("alop.messaging.internal.service-name", "alop-iam")));
        context.register(InternalEventContextAutoConfiguration.class);
        context.refresh();
        provider = context.getBean(InternalEventContext.Provider.class);
        Path root = Path.of("").toAbsolutePath();
        while (!Files.isDirectory(root.resolve("spec/alop-v7.0"))) root = root.getParent();
        try (Connection db = dataSource.getConnection(); var statement = db.createStatement()) {
            statement.execute("CREATE ALIAS UTC_TIMESTAMP FOR 'com.company.alop.iam.provisioning.ProvisioningFixture.utcTimestamp'");
            for (String module : new String[] {"iam", "integration"}) {
                String ddl = Files.readString(root.resolve("spec/alop-v7.0/03-database/flyway/" + module + "/V1__init.sql"));
                for (String sql : ddl.split(";")) if (!sql.isBlank()) statement.execute(sql);
            }
        }
        sql("INSERT INTO platform_user(id,username,password_hash,status) VALUES(7,'admin','unused','ACTIVE')");
        sql("INSERT INTO permission VALUES(1,'tenant:user:manage','Users','tenant'),"
                + "(2,'tenant:role:manage','Roles','tenant'),(3,'organization:team:manage','Teams','organization')");
        service = new TenantProvisioningService(new JdbcTenantProvisioningRepository(ids::incrementAndGet),
                new JdbcOutboxStore(), ids::incrementAndGet, provider);
        consumer = new TenantProvisioningConsumer(dataSource,
                new JdbcInboxDeduplicator(new SnowflakeIdGenerator(31)), provider, service, "iam-provisioning");
    }

    public static Timestamp utcTimestamp(int precision) { return Timestamp.from(Instant.now()); }

    public ObjectNode event() {
        ObjectNode root = json.createObjectNode();
        root.put("eventId", "request-event-1").put("eventType", "tenant.provisioning.requested.v1")
                .put("eventVersion", "1.0").put("tenantId", "42").put("aggregateType", "Tenant")
                .put("aggregateId", "42").put("aggregateVersion", 0).put("traceId", "trace-42")
                .put("correlationId", "correlation-42").put("occurredAt", "2026-10-27T00:00:00Z");
        root.putObject("data").put("tenantId", "42").put("adminUserId", 7)
                .put("tenantCreateRequestId", "create-42").put("tenantPackageId", 1)
                .put("timezone", "Asia/Shanghai").put("currency", "CNY");
        return root;
    }

    public void process(ObjectNode event) throws Exception {
        try (Connection db = dataSource.getConnection()) {
            db.setAutoCommit(false);
            try {
                service.provision(db, RabbitEventEnvelope.parse(json.writeValueAsBytes(event)));
                db.commit();
            } catch (SQLException | RuntimeException failure) {
                db.rollback();
                throw failure;
            }
        }
    }

    public Delivery delivery(ObjectNode event) throws Exception {
        return new Delivery(new Envelope(17, false, "alop.events", "tenant.provisioning.requested.v1"),
                new AMQP.BasicProperties.Builder().messageId(event.path("eventId").asText())
                        .type(event.path("eventType").asText()).headers(Map.of(
                                "eventId", event.path("eventId").asText(), "eventType", event.path("eventType").asText(),
                                "eventVersion", event.path("eventVersion").asText(), "tenantId", event.path("tenantId").asText())).build(),
                json.writeValueAsBytes(event));
    }

    public void sql(String sql) throws SQLException {
        try (Connection db = dataSource.getConnection(); var statement = db.createStatement()) { statement.execute(sql); }
    }

    public String value(String sql) throws SQLException {
        try (Connection db = dataSource.getConnection(); var statement = db.createStatement(); var result = statement.executeQuery(sql)) {
            if (!result.next()) throw new AssertionError("No result: " + sql);
            return result.getString(1);
        }
    }

    public int count(String table) throws SQLException { return Integer.parseInt(value("SELECT COUNT(*) FROM " + table)); }
    public ObjectNode payload() throws Exception {
        var node = json.readTree(value("SELECT payload FROM mq_outbox"));
        return (ObjectNode) (node.isTextual() ? json.readTree(node.textValue()) : node);
    }
    @Override public void close() throws Exception {
        context.close();
        try (Connection db = dataSource.getConnection(); var statement = db.createStatement()) { statement.execute("DROP ALL OBJECTS"); }
    }
}
