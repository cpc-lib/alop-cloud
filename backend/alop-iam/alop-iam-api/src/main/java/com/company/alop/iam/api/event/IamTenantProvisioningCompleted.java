package com.company.alop.iam.api.event;

/** Data contract for iam.tenant.provisioning.completed.v1. */
public record IamTenantProvisioningCompleted(String tenantId, long adminUserId, long membershipId,
        long adminRoleId, String tenantCreateRequestId) {
    public IamTenantProvisioningCompleted {
        if (tenantId == null || !tenantId.matches("[1-9][0-9]*")
                || adminUserId <= 0 || membershipId <= 0 || adminRoleId <= 0
                || tenantCreateRequestId == null || tenantCreateRequestId.isBlank()) {
            throw new IllegalArgumentException("Invalid tenant provisioning completion data");
        }
    }
}
