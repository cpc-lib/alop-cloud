package com.company.alop.tenant.application.service;

import com.company.alop.framework.common.error.BusinessException;
import com.company.alop.framework.common.error.CommonErrors;
import com.company.alop.tenant.application.command.CreateTenantCommand;
import com.company.alop.tenant.application.port.TenantProvisioningRequestedPublisher;
import com.company.alop.tenant.domain.repository.TenantCreateIdempotencyRepository;
import com.company.alop.tenant.domain.repository.TenantPackageRepository;
import com.company.alop.tenant.domain.repository.TenantRepository;
import com.company.alop.tenant.domain.tenant.Tenant;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import java.util.function.LongSupplier;
import javax.sql.DataSource;

public final class CreateTenantService {
    private static final String RESPONSE_CODE = "TENANT_CREATE_ACCEPTED";
    private final DataSource dataSource;
    private final TenantRepository tenantRepository;
    private final TenantPackageRepository packageRepository;
    private final TenantCreateIdempotencyRepository idempotencyRepository;
    private final LongSupplier tenantIdGenerator;
    private final LongSupplier idempotencyIdGenerator;
    private final TenantProvisioningRequestedPublisher provisioningPublisher;

    public CreateTenantService(DataSource dataSource, TenantRepository tenantRepository,
            TenantPackageRepository packageRepository, TenantCreateIdempotencyRepository idempotencyRepository,
            LongSupplier tenantIdGenerator, LongSupplier idempotencyIdGenerator,
            TenantProvisioningRequestedPublisher provisioningPublisher) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
        this.tenantRepository = Objects.requireNonNull(tenantRepository, "tenantRepository");
        this.packageRepository = Objects.requireNonNull(packageRepository, "packageRepository");
        this.idempotencyRepository = Objects.requireNonNull(idempotencyRepository, "idempotencyRepository");
        this.tenantIdGenerator = Objects.requireNonNull(tenantIdGenerator, "tenantIdGenerator");
        this.idempotencyIdGenerator = Objects.requireNonNull(idempotencyIdGenerator, "idempotencyIdGenerator");
        this.provisioningPublisher = Objects.requireNonNull(provisioningPublisher, "provisioningPublisher");
    }

    public CreateTenantResult create(CreateTenantCommand command) throws SQLException {
        Objects.requireNonNull(command, "command");
        String requestHash = requestHash(command);
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                if (!idempotencyRepository.insertPending(connection, idempotencyIdGenerator.getAsLong(),
                        command.idempotencyKey(), requestHash)) {
                    TenantCreateIdempotencyRepository.Entry existing = idempotencyRepository
                            .findByKeyForUpdate(connection, command.idempotencyKey())
                            .orElseThrow(() -> new SQLException("CreateTenant idempotency record disappeared"));
                    if (!requestHash.equals(existing.requestHash())) {
                        throw new BusinessException(CommonErrors.IDEMPOTENCY_KEY_CONFLICT);
                    }
                    if (!"COMPLETED".equals(existing.status())) {
                        throw new SQLException("CreateTenant idempotency record is not completed");
                    }
                    if (!RESPONSE_CODE.equals(existing.responseCode())) {
                        throw new SQLException("CreateTenant idempotency response code is invalid");
                    }
                    CreateTenantResult replay = CreateTenantResult.fromResponseBody(existing.responseBody());
                    if (!command.tenantCreateRequestId().equals(replay.tenantCreateRequestId())) {
                        throw new SQLException("CreateTenant replay request ID does not match command");
                    }
                    Tenant replayTenant = tenantRepository.findByIdForUpdate(connection, replay.tenantId())
                            .orElseThrow(() -> new SQLException("CreateTenant replay Tenant does not exist"));
                    if (!command.tenantCreateRequestId().equals(replayTenant.tenantCreateRequestId())) {
                        throw new SQLException("CreateTenant replay Tenant request ID does not match command");
                    }
                    connection.commit();
                    return replay;
                }

                long packageId = packageRepository.findActiveIdByCode(connection, command.packageCode())
                        .orElseThrow(() -> new BusinessException(CommonErrors.TENANT_PACKAGE_NOT_FOUND));
                Tenant tenant = Tenant.creating(tenantIdGenerator.getAsLong(),
                        command.tenantCreateRequestId(), command.tenantCode(), command.tenantName(),
                        command.tenantType(), packageId, command.timezone(), command.currency());
                try {
                    tenantRepository.insert(connection, tenant);
                } catch (SQLException failure) {
                    if (isTenantIdentityDuplicateKey(failure)) {
                        throw new BusinessException(CommonErrors.TENANT_CREATE_REQUEST_CONFLICT);
                    }
                    throw failure;
                }
                provisioningPublisher.publish(connection, tenant, command);
                CreateTenantResult result = new CreateTenantResult(tenant.id(),
                        tenant.tenantCreateRequestId(), tenant.status(), tenant.version());
                idempotencyRepository.complete(connection, command.idempotencyKey(), requestHash,
                        RESPONSE_CODE, result.responseBody());
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException failure) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackFailure) {
                    failure.addSuppressed(rollbackFailure);
                }
                throw failure;
            }
        }
    }

    static String requestHash(CreateTenantCommand command) {
        Objects.requireNonNull(command, "command");
        return sha256(command.canonicalForm());
    }

    static boolean isTenantIdentityDuplicateKey(SQLException failure) {
        for (SQLException current = failure; current != null; current = current.getNextException()) {
            if (current.getErrorCode() != 1062 || current.getMessage() == null) {
                continue;
            }
            String message = current.getMessage();
            if (message.contains("uk_tenant_code")
                    || message.contains("uk_tenant_create_request_id")) {
                return true;
            }
        }
        return false;
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte item : digest) {
                hex.append(String.format("%02x", item));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is required", impossible);
        }
    }
}
