package io.hohichh.marketplace.gateway.config;

import io.hohichh.marketplace.gateway.handler.UserDeletionHandler;
import io.hohichh.marketplace.gateway.handler.UserRegistrationHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.userdetails.UserDetailsService;


@Configuration
@RequiredArgsConstructor
public class GatewayRoutesConfig {
    private final ServiceUrlsConfig urlConfig;
    private final UserRegistrationHandler register;
    private final UserDeletionHandler deletion;

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {
        return builder.routes()
                //docs swagger ui
                .route("auth-docs", r -> r
                        .path("/v3/api-docs/auth")
                        .filters(f -> f.rewritePath("/v3/api-docs/auth", "/api/v3/api-docs"))
                        .uri(urlConfig.getAuth())
                )
                .route("user-docs", r -> r
                        .path("/v3/api-docs/user")
                        .filters(f -> f.rewritePath("/v3/api-docs/user", "/api/v3/api-docs"))
                        .uri(urlConfig.getUser())
                )
                .route("order-docs", r -> r
                        .path("/v3/api-docs/order")
                        .filters(f -> f.rewritePath("/v3/api-docs/order", "/api/v3/api-docs"))
                        .uri(urlConfig.getOrder())
                )

                //registration required saving of user data and receiving user_id from user-service
                //and saving user_id and credentials in auth-service
                .route("user-registration-handler", r -> r
                        .path("/api/v1/auth/credentials")
                        .and().method(HttpMethod.POST)
                        .filters(f -> f.filter((exchange, chain) -> register.handle(exchange)))
                        .uri("no://op")
                )

                .route("user-delete-handler", r -> r
                        .path("/api/v1/users/{id}")
                        .and().method(HttpMethod.DELETE)
                        .filters(f -> f.filter((exchange, chain) -> deletion.handle(exchange)))
                        .uri("no://op")
                )

                //authentication proxy (login, refresh token)
                .route("auth-proxy", r -> r
                        .path("/api/v1/auth/login", "/api/v1/auth/refresh")
                        .and().method(HttpMethod.POST)
                        .uri(urlConfig.getAuth())
                )

                //user-service proxy
                //for all crud operations except user creation
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
