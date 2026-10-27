package com.company.alop.tenant.application.port;

import com.company.alop.tenant.application.command.CreateTenantCommand;
import com.company.alop.tenant.domain.tenant.Tenant;
import java.sql.Connection;
import java.sql.SQLException;

/** Publishes the Tenant provisioning request into the caller-owned local transaction. */
@FunctionalInterface
public interface TenantProvisioningRequestedPublisher {
    void publish(Connection connection, Tenant tenant, CreateTenantCommand command) throws SQLException;
}
