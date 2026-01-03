package io.hohichh.marketplace.gateway.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.hohichh.marketplace.gateway.dto.in.NewUserDto;
import io.hohichh.marketplace.gateway.dto.in.UserCredentialsCreateDto;
import io.hohichh.marketplace.gateway.dto.in.UserDataWithCredentialsDto;
import io.hohichh.marketplace.gateway.dto.out.UserDto;
import io.hohichh.marketplace.gateway.exception.ActionNotPermittedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
@Slf4j
public class UserRegistrationHandler extends UserBaseHandler{

    public UserRegistrationHandler(WebClient authServiceWebClient,
                                   WebClient userServiceWebClient,
                                   ObjectMapper objectMapper) {
        super(authServiceWebClient, userServiceWebClient, objectMapper);
    }

    public Mono<Void> handle(ServerWebExchange exchange){
        return DataBufferUtils.join(exchange.getRequest().getBody())
                .flatMap(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);

                    try{
                        String bodyStr = new String(bytes, StandardCharsets.UTF_8);
                        UserDataWithCredentialsDto inDto = objectMapper.readValue(
                                bodyStr, UserDataWithCredentialsDto.class);
                        return processRegistration(inDto, exchange);
                    } catch (JsonProcessingException e) {
                        return Mono.error(new ActionNotPermittedException("Invalid JSON format: " + e.getMessage()));
                    }
                }).onErrorResume(e -> handleErrors(exchange, e));
    }

    private Mono<Void> processRegistration(UserDataWithCredentialsDto fullDto,
                                           ServerWebExchange exchange){
        NewUserDto newUserDto = new NewUserDto(
                fullDto.name(),
                fullDto.surname(),
                fullDto.birthDate(),
                fullDto.email()
        );

        return userServiceWebClient.post()
                .uri("/api/v1/users")
                .bodyValue(newUserDto)
                .retrieve()
                .bodyToMono(UserDto.class)
                .flatMap(createdUser -> {
                    log.info("User created in User-Service with ID: {}", createdUser.id());

                    UserCredentialsCreateDto credentials = new UserCredentialsCreateDto(
                            createdUser.id(),
                            fullDto.login(),
                            fullDto.password()
                    );

                    return authServiceWebClient.post()
                            .uri("/api/v1/auth/credentials")
                            .bodyValue(credentials)
                            .retrieve()
                            .toBodilessEntity()
                            .then(writeResponse(exchange, createdUser))
                            .onErrorResume(e -> {
                                log.error("Failed to create credentials in Auth-Service. Rolling back user creation for ID: {}", createdUser.id());
                                return rollBackUserCreation(createdUser.id())
                                        .then(Mono.error(e));
                            });
                });
    }

    private Mono<Void> rollBackUserCreation(UUID userId){
        return userServiceWebClient.delete()
                .uri("/api/v1/users/" + userId)
                .retrieve()
                .toBodilessEntity()
                .doOnSuccess(v -> log.info("Rollback successful: User {} deleted", userId))
                .doOnError(e -> log.error("CRITICAL: Rollback failed for user {}." +
                        " Manual intervention required!", userId))
                .then();
    }

    private Mono<Void> writeResponse(ServerWebExchange exchange, UserDto userDto){
        exchange.getResponse().setStatusCode(HttpStatus.CREATED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(userDto);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (JsonProcessingException _) {
            return Mono.error(new RuntimeException("Error writing response"));
        }
    }
}
