package com.company.alop.framework.test.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.company.alop.framework.common.messaging.InternalEventContext;
import com.company.alop.framework.common.messaging.InternalEventContextAutoConfiguration;
import com.company.alop.framework.common.error.BusinessException;
import com.company.alop.framework.common.error.CommonErrors;
import com.company.alop.framework.outbox.JdbcOutboxStore;
import com.company.alop.tenant.application.command.CreateTenantCommand;
import com.company.alop.tenant.application.port.TenantProvisioningRequestedPublisher;
import com.company.alop.tenant.application.service.CreateTenantResult;
import com.company.alop.tenant.application.service.CreateTenantService;
import com.company.alop.tenant.domain.repository.TenantPackageRepository;
import com.company.alop.tenant.domain.repository.TenantRepository;
import com.company.alop.tenant.infrastructure.messaging.JdbcTenantProvisioningRequestedPublisher;
import com.company.alop.tenant.infrastructure.persistence.repository.JdbcTenantCreateIdempotencyRepository;
import com.company.alop.tenant.infrastructure.persistence.repository.JdbcTenantPackageRepository;
import com.company.alop.tenant.infrastructure.persistence.repository.JdbcTenantRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysql.cj.jdbc.MysqlDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

class IntegrationTenantCreateIT {
    private static DataSource dataSource;
    private static TenantPackageRepository packageRepository;
    private static TenantRepository tenantRepository;
    private static CreateTenantService service;
    private static TenantProvisioningRequestedPublisher publisher;
    private static AnnotationConfigApplicationContext messagingContext;
    private static final ObjectMapper JSON = new ObjectMapper();

    @BeforeAll
    static void initialize() throws Exception {
        String url = System.getProperty("alop.mysql.jdbcUrl");
        String password = System.getProperty("alop.mysql.password");
        if (url == null || !url.contains("/alop_schema_it") || password == null) {
            throw new IllegalStateException("Tenant create test requires disposable alop_schema_it MySQL");
        }
        MysqlDataSource mysql = new MysqlDataSource();
        mysql.setUrl(url);
        mysql.setUser("root");
        mysql.setPassword(password);
        dataSource = mysql;
        packageRepository = new JdbcTenantPackageRepository();
        tenantRepository = new JdbcTenantRepository();
        AtomicLong tenantIds = new AtomicLong(400L);
        AtomicLong idempotencyIds = new AtomicLong(800L);
        AtomicLong eventIds = new AtomicLong(1200L);
        messagingContext = new AnnotationConfigApplicationContext();
        messagingContext.getEnvironment().getPropertySources().addFirst(new MapPropertySource(
                "test", Map.of("alop.messaging.internal.service-name", "alop-tenant")));
        messagingContext.register(InternalEventContextAutoConfiguration.class);
        messagingContext.refresh();
        publisher = new JdbcTenantProvisioningRequestedPublisher(new JdbcOutboxStore(),
                eventIds::incrementAndGet,
                messagingContext.getBean(InternalEventContext.Provider.class));
        service = createService(publisher, tenantIds, idempotencyIds);
    }

    @AfterAll
    static void closeMessagingContext() {
        if (messagingContext != null) {
            messagingContext.close();
        }
    }

    @BeforeEach
    void clean() throws SQLException {
        try (Connection connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM mq_outbox");
            statement.executeUpdate("DELETE FROM idempotency_record WHERE api_code='TENANT_CREATE_V2'");
            statement.executeUpdate("DELETE FROM tenant");
            statement.executeUpdate("DELETE FROM tenant_package");
            statement.executeUpdate("INSERT INTO tenant_package "
                    + "(id, package_code, package_name, status, capability_json) "
                    + "VALUES (1, 'standard', 'Standard', 'ACTIVE', '{}')");
        }
    }

    @Test
    void sameKeyAndSameRequestReplaysTheOriginalTenant() throws Exception {
        CreateTenantResult first = service.create(command("idem-300", "request-300", "tenant-300"));
        CreateTenantResult replay = service.create(command("idem-300", "request-300", "tenant-300"));

        assertEquals(first, replay);
        assertEquals(1, count("tenant", "tenant_code='tenant-300'"));
        assertEquals(1, count("tenant", "tenant_code='tenant-300' AND status='CREATING'"));
        assertEquals(1, count("idempotency_record",
                "api_code='TENANT_CREATE_V2' AND response_code='TENANT_CREATE_ACCEPTED'"));
        assertEquals(1, count("mq_outbox", "event_type='tenant.provisioning.requested.v1'"));
    }

    @Test
    void sameKeyWithDifferentPayloadIsRejected() throws Exception {
        service.create(command("idem-301", "request-301", "tenant-301"));

        BusinessException failure = assertThrows(BusinessException.class,
                () -> service.create(command("idem-301", "request-301", "tenant-301-changed")));

        assertEquals(CommonErrors.IDEMPOTENCY_KEY_CONFLICT, failure.error());
    }

    @Test
    void sameRequestIdentityWithDifferentKeyIsRejected() throws Exception {
        service.create(command("idem-302-a", "request-302", "tenant-302"));

        BusinessException failure = assertThrows(BusinessException.class,
                () -> service.create(command("idem-302-b", "request-302", "tenant-302-other")));

        assertEquals(CommonErrors.TENANT_CREATE_REQUEST_CONFLICT, failure.error());
    }

    @Test
    void concurrentIdenticalRequestsCreateOneTenantAndOneReplayRecord() throws Exception {
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Callable<CreateTenantResult> create = () -> service.create(
                    command("idem-303", "request-303", "tenant-303"));
            List<Future<CreateTenantResult>> futures = executor.invokeAll(List.of(create, create));
            List<CreateTenantResult> results = new ArrayList<>();
            for (Future<CreateTenantResult> future : futures) {
                results.add(future.get());
            }
            assertEquals(results.get(0), results.get(1));
        }
        assertEquals(1, count("tenant", "tenant_code='tenant-303'"));
        assertEquals(1, count("idempotency_record", "api_code='TENANT_CREATE_V2'"));
        assertEquals(1, count("mq_outbox", "event_type='tenant.provisioning.requested.v1'"));
    }

    @Test
    void failedTenantInsertRollsBackTheNewIdempotencyRecord() throws Exception {
        service.create(command("idem-304-a", "request-304-a", "tenant-304"));

        assertThrows(BusinessException.class,
                () -> service.create(command("idem-304-b", "request-304-b", "tenant-304")));

        CreateTenantResult retry = service.create(command("idem-304-b", "request-304-b", "tenant-304-retry"));
        assertEquals("request-304-b", retry.tenantCreateRequestId());
        assertEquals(2, count("idempotency_record", "api_code='TENANT_CREATE_V2'"));
    }

    @Test
    void inactivePackageIsRejectedBeforeTenantInsert() throws Exception {
        try (Connection connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO tenant_package "
                    + "(id, package_code, package_name, status, capability_json) "
                    + "VALUES (2, 'inactive', 'Inactive', 'INACTIVE', '{}')");
        }

        BusinessException failure = assertThrows(BusinessException.class,
                () -> service.create(command("idem-305", "request-305", "tenant-305", "inactive")));

        assertEquals(CommonErrors.TENANT_PACKAGE_NOT_FOUND, failure.error());
        assertEquals(0, count("tenant", "tenant_code='tenant-305'"));
        assertEquals(0, count("idempotency_record", "api_code='TENANT_CREATE_V2'"));
    }

    @Test
    void requestedOutboxContainsTheVersionedSchemaFieldsWithoutAuthorizationToken() throws Exception {
        service.create(command("idem-306", "request-306", "tenant-306"));

        String payload = payload("tenant-306");
        JsonNode event = JSON.readTree(payload);
        assertEquals("tenant.provisioning.requested.v1", event.path("eventType").asText());
        assertEquals("1.0", event.path("eventVersion").asText());
        assertEquals("Tenant", event.path("aggregateType").asText());
        assertEquals(0, event.path("aggregateVersion").asInt());
        assertEquals("1001", event.path("data").path("adminUserId").asText());
        assertEquals("request-306", event.path("data").path("tenantCreateRequestId").asText());
        assertEquals("1", event.path("data").path("tenantPackageId").asText());
        assertEquals("Asia/Shanghai", event.path("data").path("timezone").asText());
        assertEquals("CNY", event.path("data").path("currency").asText());
        assertFalse(payload.contains("Authorization"));
        assertFalse(payload.contains("Bearer "));
    }

    @Test
    void publisherFailureRollsBackTenantIdempotencyAndOutboxTogether() throws Exception {
        AtomicLong tenantIds = new AtomicLong(600L);
        AtomicLong idempotencyIds = new AtomicLong(900L);
        TenantProvisioningRequestedPublisher failingPublisher = (connection, tenant, command) -> {
            publisher.publish(connection, tenant, command);
            throw new SQLException("injected publisher failure");
        };
        CreateTenantService failingService = createService(failingPublisher, tenantIds, idempotencyIds);

        assertThrows(SQLException.class,
                () -> failingService.create(command("idem-307", "request-307", "tenant-307")));

        assertEquals(0, count("tenant", "tenant_code='tenant-307'"));
        assertEquals(0, count("idempotency_record", "idempotency_key='idem-307'"));
        assertEquals(0, count("mq_outbox", "event_type='tenant.provisioning.requested.v1'"));
    }

    private static CreateTenantCommand command(String key, String requestId, String code) {
        return command(key, requestId, code, "standard");
    }

    private static CreateTenantCommand command(String key, String requestId, String code,
            String packageCode) {
        return new CreateTenantCommand(key, requestId, code, "Tenant " + code,
                "ENTERPRISE", packageCode, 1001L, "Asia/Shanghai", "CNY");
    }

    private static CreateTenantService createService(TenantProvisioningRequestedPublisher requestedPublisher,
            AtomicLong tenantIds, AtomicLong idempotencyIds) {
        return new CreateTenantService(dataSource, tenantRepository, packageRepository,
                new JdbcTenantCreateIdempotencyRepository(), tenantIds::incrementAndGet,
                idempotencyIds::incrementAndGet, requestedPublisher);
    }

    private static String payload(String tenantCode) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             var statement = connection.prepareStatement(
                     "SELECT payload FROM mq_outbox WHERE event_type=? AND aggregate_id="
                             + "(SELECT CAST(id AS CHAR) FROM tenant WHERE tenant_code=?)")) {
            statement.setString(1, "tenant.provisioning.requested.v1");
            statement.setString(2, tenantCode);
            try (var result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new SQLException("Requested Tenant event was not persisted");
                }
                return result.getString(1);
            }
        }
    }

    private static int count(String table, String predicate) throws SQLException {
        try (Connection connection = dataSource.getConnection(); var statement = connection.createStatement();
             var result = statement.executeQuery("SELECT COUNT(*) FROM " + table + " WHERE " + predicate)) {
            result.next();
            return result.getInt(1);
        }
    }
}
