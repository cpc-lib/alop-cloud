package com.company.alop.tenant.domain.tenant;

import com.company.alop.framework.common.error.BusinessException;
import com.company.alop.framework.common.error.CommonErrors;
/** Pure Tenant aggregate lifecycle; provisioning remains outside this slice. */
public final class Tenant {
    private final long id;
    private final String tenantCreateRequestId;
    private final String tenantCode;
    private final String tenantName;
    private final String tenantType;
    private final Long packageId;
    private final String timezone;
    private final String currency;
    private TenantStatus status;
    private long version;

    private Tenant(long id, String tenantCreateRequestId, String tenantCode, String tenantName,
            String tenantType, Long packageId, String timezone, String currency) {
        if (id <= 0) {
            throw new IllegalArgumentException("Tenant identity and create request fields are required");
        }
        this.id = id;
        this.tenantCreateRequestId = TenantDefaults.requireRequestId(tenantCreateRequestId);
        this.tenantCode = TenantDefaults.requireTenantCode(tenantCode);
        this.tenantName = TenantDefaults.requireTenantName(tenantName);
        this.tenantType = TenantDefaults.requireTenantType(tenantType);
        this.packageId = packageId;
        this.timezone = TenantDefaults.normalizeTimezone(timezone);
        this.currency = TenantDefaults.normalizeCurrency(currency);
        this.status = TenantStatus.CREATING;
    }

    public static Tenant creating(long id, String tenantCreateRequestId, String tenantCode,
            String tenantName, String tenantType, Long packageId) {
        return creating(id, tenantCreateRequestId, tenantCode, tenantName, tenantType, packageId,
                null, null);
    }

    public static Tenant creating(long id, String tenantCreateRequestId, String tenantCode,
            String tenantName, String tenantType, Long packageId, String timezone, String currency) {
        return new Tenant(id, tenantCreateRequestId, tenantCode, tenantName, tenantType, packageId,
                timezone, currency);
    }

    public static Tenant rehydrate(long id, String tenantCreateRequestId, String tenantCode,
            String tenantName, String tenantType, Long packageId, TenantStatus status,
            long version) {
        return rehydrate(id, tenantCreateRequestId, tenantCode, tenantName, tenantType, packageId,
                null, null, status, version);
    }

    public static Tenant rehydrate(long id, String tenantCreateRequestId, String tenantCode,
            String tenantName, String tenantType, Long packageId, String timezone, String currency,
            TenantStatus status, long version) {
        if (status == null || version < 0) {
            throw new IllegalArgumentException("Tenant status and version are required");
        }
        Tenant tenant = new Tenant(id, tenantCreateRequestId, tenantCode, tenantName, tenantType, packageId,
                timezone, currency);
        tenant.status = status;
        tenant.version = version;
        return tenant;
    }

    public void provisionSucceeded() {
        moveFrom(TenantStatus.CREATING, TenantStatus.ACTIVE);
    }

    public void suspend() {
        moveFrom(TenantStatus.ACTIVE, TenantStatus.SUSPENDED);
    }

    public void resume() {
        moveFrom(TenantStatus.SUSPENDED, TenantStatus.ACTIVE);
    }

    public void expire() {
        moveFrom(TenantStatus.ACTIVE, TenantStatus.EXPIRED);
    }

    public void requestTermination() {
        if (status != TenantStatus.ACTIVE && status != TenantStatus.SUSPENDED
                && status != TenantStatus.EXPIRED) {
            throw invalidState();
        }
        status = TenantStatus.TERMINATING;
        version++;
    }

    public void finalizeTermination(TenantTerminationReadiness readiness) {
        require(TenantStatus.TERMINATING);
        if (readiness == null || !readiness.isReady()) {
            throw new BusinessException(CommonErrors.TENANT_TERMINATION_BLOCKED);
        }
        status = TenantStatus.TERMINATED;
        version++;
    }

    public boolean acceptsBusinessTraffic() {
        return status == TenantStatus.ACTIVE;
    }

    public long id() { return id; }

    public String tenantCreateRequestId() { return tenantCreateRequestId; }

    public String tenantCode() { return tenantCode; }

    public String tenantName() { return tenantName; }

    public String tenantType() { return tenantType; }

    public Long packageId() { return packageId; }

    public String timezone() { return timezone; }

    public String currency() { return currency; }

    public TenantStatus status() { return status; }

    public long version() { return version; }

    private void moveFrom(TenantStatus expected, TenantStatus target) {
        require(expected);
        status = target;
        version++;
    }

    private void require(TenantStatus expected) {
        if (status != expected) {
            throw invalidState();
        }
    }

    private static BusinessException invalidState() {
        return new BusinessException(CommonErrors.TENANT_INVALID_STATE);
    }

}
