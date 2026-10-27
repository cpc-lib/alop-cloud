package com.company.alop.iam.application.service;

/** A deterministic provisioning rejection safe to persist and publish. */
public final class TenantProvisioningRejected extends RuntimeException {
    public TenantProvisioningRejected(String errorCode) { super(errorCode); }
    public String errorCode() { return getMessage(); }
}
