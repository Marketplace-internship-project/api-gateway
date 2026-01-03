package io.hohichh.marketplace.gateway.handler;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Map;

@Component
@Slf4j
public class UserDeletionHandler extends UserBaseHandler {

    public UserDeletionHandler(WebClient authServiceWebClient,
                               WebClient userServiceWebClient,
                               ObjectMapper objectMapper) {
        super(authServiceWebClient, userServiceWebClient, objectMapper);
    }

    public Mono<Void> handle(ServerWebExchange exchange) {
        Map<String, String> pathVariables = exchange.getAttribute(ServerWebExchangeUtils.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        String userId = pathVariables != null ? pathVariables.get("id") : null;

        if (userId == null) {
            return Mono.error(new IllegalArgumentException("User ID is missing in request path"));
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        log.info("Starting deletion process for user: {}", userId);

        return authServiceWebClient.delete()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/auth/credentials")
                        .queryParam("user-id", userId)
                        .build())
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .retrieve()
                .toBodilessEntity()

                //if auth-service is unavailable
                .retryWhen(Retry.backoff(3, Duration.ofMillis(500))
                        .filter(this::isServerError))
                .onErrorMap(e -> {
                    log.error("Failed to delete credentials for user {}. Aborting.", userId, e);
                    return new RuntimeException("Auth service unavailable or access denied. Deletion aborted.");
                })

                .flatMap(authResponse -> {
                    log.info("Credentials deleted for user {}. Proceeding to delete profile.", userId);

                    return userServiceWebClient.delete()
                            .uri("/api/v1/users/" + userId)
                            .header(HttpHeaders.AUTHORIZATION, authHeader)
                            .retrieve()
                            .toBodilessEntity()
                            //if user-service unavailable
                            .retryWhen(Retry.backoff(3, Duration.ofMillis(500))
                                    .filter(this::isServerError));
                })

                .then(Mono.defer(() -> {
                    exchange.getResponse().setStatusCode(HttpStatus.NO_CONTENT);
                    return exchange.getResponse().setComplete();
                }))


                .onErrorResume(e -> {
                    if (e.getMessage().contains("Auth service unavailable")) {
                        return handleErrors(exchange, e);
                    }
                    log.error("CRITICAL: User credentials deleted, but profile deletion failed for user {}. Manual cleanup required!", userId);
                    exchange.getResponse().setStatusCode(HttpStatus.ACCEPTED);
                    return exchange.getResponse().setComplete();
                });
    }

    private boolean isServerError(Throwable t) {
        if (t instanceof WebClientResponseException ex) {
            return ex.getStatusCode().is5xxServerError();
        }
        return true;
    }

}
