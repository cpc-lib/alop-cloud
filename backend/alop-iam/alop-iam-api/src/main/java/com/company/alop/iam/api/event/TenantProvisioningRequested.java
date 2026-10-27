package com.company.alop.iam.api.event;

/** Data contract for tenant.provisioning.requested.v1. */
public record TenantProvisioningRequested(String tenantId, String tenantCreateRequestId,
        long adminUserId, long tenantPackageId, String timezone, String currency) {
    public TenantProvisioningRequested {
        if (tenantId == null || !tenantId.matches("[1-9][0-9]*")
                || tenantCreateRequestId == null || tenantCreateRequestId.isBlank()
                || adminUserId <= 0 || tenantPackageId <= 0 || timezone == null || timezone.isBlank()
                || currency == null || !currency.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException("Invalid tenant provisioning request data");
        }
    }
}
