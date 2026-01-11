package io.hohichh.marketplace.gateway.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.hohichh.marketplace.gateway.exception.ActionNotPermittedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Instant;

@RequiredArgsConstructor
@Slf4j
public abstract class UserBaseHandler {
    protected final WebClient authServiceWebClient;
    protected final WebClient userServiceWebClient;
    protected final ObjectMapper objectMapper;

    protected Mono<Void> handleErrors(ServerWebExchange exchange, Throwable e) {
        log.error("Handler error occurred: {}", e.getMessage());

        ProblemDetail problemDetail;

        if (e instanceof WebClientResponseException wcre) {
            problemDetail = extractProblemDetailFromDownstream(wcre);
        } else if (e instanceof ActionNotPermittedException) {
            problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, e.getMessage());
            problemDetail.setTitle("Action Not Permitted");
        } else {
            problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Gateway Error");
        }

        problemDetail.setInstance(URI.create(exchange.getRequest().getPath().value()));
        problemDetail.setProperty("timestamp", Instant.now());

        return writeResponse(exchange, problemDetail);
    }


    private ProblemDetail extractProblemDetailFromDownstream(WebClientResponseException wcre) {
        String responseBody = wcre.getResponseBodyAsString();
        if (responseBody != null && !responseBody.isBlank()) {
            try {
                return objectMapper.readValue(responseBody, ProblemDetail.class);
            } catch (Exception ex) {
                log.warn("Failed to parse downstream ProblemDetail: {}", ex.getMessage());
            }
        }
        return ProblemDetail.forStatusAndDetail(wcre.getStatusCode(), wcre.getStatusText());
    }


    private Mono<Void> writeResponse(ServerWebExchange exchange, ProblemDetail problemDetail) {
        ServerHttpResponse response = exchange.getResponse();

        response.setStatusCode(HttpStatus.valueOf(problemDetail.getStatus()));
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(problemDetail);

            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException jsonEx) {
            log.error("Error writing JSON response", jsonEx);
            return response.setComplete();
        }
    }
}