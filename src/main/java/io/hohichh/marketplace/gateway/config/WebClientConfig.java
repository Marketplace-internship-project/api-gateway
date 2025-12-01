package io.hohichh.marketplace.gateway.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;


@Configuration
@RequiredArgsConstructor
public class WebClientConfig {
    private final ServiceUrlsConfig urlConfig;

    @Bean
    public WebClient authServiceWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl(urlConfig.getAuth())
                .build();
    }

    @Bean
    public WebClient userServiceWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl(urlConfig.getUser())
                .build();
    }

    @Bean
    public WebClient orderServiceWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl(urlConfig.getOrder())
                .build();
    }
}
