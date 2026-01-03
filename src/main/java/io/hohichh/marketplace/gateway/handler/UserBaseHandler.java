package io.hohichh.marketplace.gateway.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.hohichh.marketplace.gateway.exception.ActionNotPermittedException;
import io.hohichh.marketplace.gateway.exception.GlobalExceptionHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;


@RequiredArgsConstructor
@Slf4j
public abstract class UserBaseHandler {
    protected final WebClient authServiceWebClient;
    protected final WebClient userServiceWebClient;
    protected final ObjectMapper objectMapper;

    protected Mono<Void> handleErrors(ServerWebExchange exchange, Throwable e){
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
                } catch (Exception _) {
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
