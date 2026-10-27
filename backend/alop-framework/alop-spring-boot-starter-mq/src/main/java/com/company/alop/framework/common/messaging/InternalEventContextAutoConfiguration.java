package com.company.alop.framework.common.messaging;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(InternalEventContextProperties.class)
public class InternalEventContextAutoConfiguration {
    @Bean
    public InternalEventContext.Provider internalEventContextProvider(
            InternalEventContextProperties properties) {
        return InternalEventContext.provider(properties.getServiceName());
    }
}
