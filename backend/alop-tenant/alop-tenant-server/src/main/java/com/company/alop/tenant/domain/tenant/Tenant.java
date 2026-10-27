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
        if (id <= 0 || !validRequestId(tenantCreateRequestId) || tenantCode == null || tenantCode.isBlank()
                || tenantName == null || tenantName.isBlank()
                || tenantType == null || tenantType.isBlank()
                || timezone == null || timezone.isBlank() || currency == null
                || !currency.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException("Tenant identity and create request fields are required");
        }
        this.id = id;
        this.tenantCreateRequestId = tenantCreateRequestId;
        this.tenantCode = tenantCode;
        this.tenantName = tenantName;
        this.tenantType = tenantType;
        this.packageId = packageId;
        this.timezone = timezone;
        this.currency = currency;
        this.status = TenantStatus.CREATING;
    }

    public static Tenant creating(long id, String tenantCreateRequestId, String tenantCode,
            String tenantName, String tenantType, Long packageId) {
        return creating(id, tenantCreateRequestId, tenantCode, tenantName, tenantType, packageId,
                "Asia/Shanghai", "CNY");
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
                "Asia/Shanghai", "CNY", status, version);
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

    private static boolean validRequestId(String requestId) {
        return requestId != null && requestId.length() >= 8 && requestId.length() <= 128
                && !requestId.isBlank();
    }
}
