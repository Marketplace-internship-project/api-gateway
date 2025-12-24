package io.hohichh.marketplace.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.services")
@Data
public class ServiceUrlsConfig {
    private String auth;
    private String user;
    private String order;
    private String payment;
}
