package io.hohichh.marketplace.gateway.handler;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.hohichh.marketplace.gateway.dto.in.UserDataWithCredentialsDto;
import io.hohichh.marketplace.gateway.dto.out.UserDto;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserRegistrationHandlerTest {

    private MockWebServer mockWebServer;
    private UserRegistrationHandler handler;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        String baseUrl = mockWebServer.url("/").toString();
        WebClient testClient = WebClient.builder().baseUrl(baseUrl).build();

        handler = new UserRegistrationHandler(testClient, testClient, objectMapper);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void handle_Success_ShouldReturn201AndCreatedUser() throws Exception {
        UUID newUserId = UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11");

        UserDataWithCredentialsDto requestDto = new UserDataWithCredentialsDto(
                "Ivan", "Ivanov", LocalDate.of(1990, 1, 1), "ivan@test.com",
                 "ivan_login", "password123"
        );
        String requestJson = objectMapper.writeValueAsString(requestDto);

        UserDto userServiceResponse = new UserDto(
                newUserId, "Ivan", "Ivanov", LocalDate.of(1990, 1, 1), "ivan@test.com"
        );

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(objectMapper.writeValueAsString(userServiceResponse)));

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(201));

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/v1/auth/credentials")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(requestJson)
        );

        StepVerifier.create(handler.handle(exchange))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.CREATED);

        RecordedRequest requestToUser = mockWebServer.takeRequest();
        assertThat(requestToUser.getPath()).isEqualTo("/api/v1/users");
        assertThat(requestToUser.getMethod()).isEqualTo("POST");
        assertThat(requestToUser.getBody().readUtf8()).contains("Ivan");

        RecordedRequest requestToAuth = mockWebServer.takeRequest();
        assertThat(requestToAuth.getPath()).isEqualTo("/api/v1/auth/credentials");
        assertThat(requestToAuth.getMethod()).isEqualTo("POST");
        assertThat(requestToAuth.getBody().readUtf8()).contains(newUserId.toString());
    }

    @Test
    void handle_AuthServiceFails_ShouldRollbackUser() throws Exception {

        UUID userId = UUID.randomUUID();
        UserDto userDto = new UserDto(userId, "Test", "Test", LocalDate.now(), "test@test.com");
        UserDataWithCredentialsDto input = new UserDataWithCredentialsDto(
                "Test", "Test", LocalDate.now(),
                "test@test.com", "login", "pass"
        );

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(objectMapper.writeValueAsString(userDto)));

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(409)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"message\": \"Login busy\"}"));

        mockWebServer.enqueue(new MockResponse().setResponseCode(204));

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(objectMapper.writeValueAsString(input))
        );

        StepVerifier.create(handler.handle(exchange))
                .verifyComplete();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.CONFLICT);


        mockWebServer.takeRequest();
        mockWebServer.takeRequest();

        RecordedRequest rollbackRequest = mockWebServer.takeRequest();
        assertThat(rollbackRequest).isNotNull();
        assertThat(rollbackRequest.getMethod()).isEqualTo("DELETE");
        assertThat(rollbackRequest.getPath()).isEqualTo("/api/v1/users/" + userId);
    }
}