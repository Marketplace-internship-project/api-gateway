package io.hohichh.marketplace.gateway.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;


@Configuration
@RequiredArgsConstructor
public class GatewayRoutesConfig {
    private final ServiceUrlsConfig urlConfig;

    @Value("${marketplace.swagger-path:/api/v3/api-docs}")
    private String swaggerPath;

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {
        return builder.routes()
                //docs swagger ui
                .route("auth-docs", r -> r
                        .path("/v3/api-docs/auth")
                        .filters(f -> f.rewritePath("/v3/api-docs/auth", swaggerPath))
                        .uri(urlConfig.getAuth())
                )
                .route("user-docs", r -> r
                        .path("/v3/api-docs/user")
                        .filters(f -> f.rewritePath("/v3/api-docs/user", swaggerPath))
                        .uri(urlConfig.getUser())
                )
                .route("order-docs", r -> r
                        .path("/v3/api-docs/order")
                        .filters(f -> f.rewritePath("/v3/api-docs/order", swaggerPath))
                        .uri(urlConfig.getOrder())
                )
                .route("payment-docs", r -> r
                        .path("/v3/api-docs/payment")
                        .filters(f -> f.rewritePath("/v3/api-docs/payment", swaggerPath))
                        .uri(urlConfig.getPayment())
                )

                //authentication proxy (login, refresh token)
                .route("auth-proxy", r -> r
                        .path("/api/v1/auth/login", "/api/v1/auth/refresh")
                        .and().method(HttpMethod.POST)
                        .uri(urlConfig.getAuth())
                )

                //user-service proxy
                //for all crud operations except user creation
                .route("user-registration-handler", r -> r
                        .path("/api/v1/registration/users")
                        .and().method(HttpMethod.POST)
                        .uri(urlConfig.getUser())
                )

                .route("user-proxy", r -> r
                        .path("/api/v1/users/**",
                                "/api/v1/cards/**")
                        .uri(urlConfig.getUser())
                )

                //order-service proxy
                .route("order-proxy", r -> r
                        .path("/api/v1/products/**",
                                "/api/v1/orders/**")
                        .uri(urlConfig.getOrder())
                )

                .route("payment-proxy", r -> r
                        .path("/api/v1/payments/**")
                        .uri(urlConfig.getPayment()) 
                )

                .build();
    }
}
