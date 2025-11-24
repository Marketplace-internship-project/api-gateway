package io.hohichh.marketplace.gateway.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.hohichh.marketplace.gateway.exception.ActionNotPermittedException;
import io.hohichh.marketplace.gateway.exception.GlobalExceptionHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserDeletionHandler {

    private final WebClient authServiceWebClient;
    private final WebClient userServiceWebClient;
    private final ObjectMapper objectMapper;

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

    private Mono<Void> handleErrors(ServerWebExchange exchange, Throwable e) {
        log.error("Registration error: {}", e.getMessage());

        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String mes = e.getMessage();

        if(e instanceof WebClientResponseException wcre){
            status = (HttpStatus) wcre.getStatusCode();
            String responseBody = wcre.getResponseBodyAsString();

            if (responseBody != null && !responseBody.isBlank()) {
                try {
                    GlobalExceptionHandler.ErrorResponse serviceError =
                            objectMapper.readValue(responseBody, GlobalExceptionHandler.ErrorResponse.class);
                    mes = serviceError.message();
                } catch (Exception parseEx) {
                    mes = wcre.getStatusText();
                }
            }
        } else if(e instanceof ActionNotPermittedException){
            status = HttpStatus.BAD_REQUEST;
        }

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        GlobalExceptionHandler.ErrorResponse errorResponse =
                new GlobalExceptionHandler.ErrorResponse(mes);
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(errorResponse);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (JsonProcessingException jsonEx) {
            log.error("Error writing error response", jsonEx);
            return exchange.getResponse().setComplete();
        }
    }
}
