package com.company.alop.iam.api.event;

/** Data contract for iam.tenant.provisioning.failed.v1. */
public record IamTenantProvisioningFailed(String tenantId, long adminUserId,
        String tenantCreateRequestId, String errorCode, boolean retryable) {
    public IamTenantProvisioningFailed {
        if (tenantId == null || !tenantId.matches("[1-9][0-9]*") || adminUserId <= 0
                || tenantCreateRequestId == null || tenantCreateRequestId.isBlank()
                || errorCode == null || errorCode.isBlank()) {
            throw new IllegalArgumentException("Invalid tenant provisioning failure data");
        }
    }
}
