package com.company.alop.framework.common.messaging;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "alop.messaging.internal")
public final class InternalEventContextProperties {
    private String serviceName;

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        // Validate every configured value, including "false" which disables the bean condition.
        InternalEventContext.provider(serviceName);
        this.serviceName = serviceName;
    }
}
