package io.hohichh.marketplace.gateway.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class UserDeletionHandlerTest {

    private MockWebServer mockWebServer;
    private UserDeletionHandler handler;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        String baseUrl = mockWebServer.url("/").toString();
        WebClient testClient = WebClient.builder().baseUrl(baseUrl).build();

        objectMapper = new ObjectMapper();
        handler = new UserDeletionHandler(testClient, testClient, objectMapper);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void handle_Success_ShouldReturn204() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse().setResponseCode(204));
        mockWebServer.enqueue(new MockResponse().setResponseCode(204));

        String userId = "123e4567-e89b-12d3-a456-426614174000";
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.delete("/api/v1/users/" + userId)
                        .header("Authorization", "Bearer test-token")
        );

        exchange.getAttributes().put(
                org.springframework.cloud.gateway.support.ServerWebExchangeUtils.URI_TEMPLATE_VARIABLES_ATTRIBUTE,
                java.util.Map.of("id", userId)
        );

        StepVerifier.create(handler.handle(exchange))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);


        RecordedRequest authRequest = mockWebServer.takeRequest();
        assertThat(authRequest.getPath()).contains("/api/v1/auth/credentials");
        assertThat(authRequest.getPath()).contains("user-id=" + userId);


        RecordedRequest userRequest = mockWebServer.takeRequest();
        assertThat(userRequest.getPath()).isEqualTo("/api/v1/users/" + userId);
        assertThat(userRequest.getHeader("Authorization")).isEqualTo("Bearer test-token");
    }

    @Test
    void handle_UserServiceFlaky_ShouldRetryAndSucceed() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse().setResponseCode(204));

        mockWebServer.enqueue(new MockResponse().setResponseCode(500));
        mockWebServer.enqueue(new MockResponse().setResponseCode(204));


        String userId = "user-retry-id";
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.delete("/" + userId).header("Authorization", "token")
        );
        exchange.getAttributes().put(
                org.springframework.cloud.gateway.support.ServerWebExchangeUtils.URI_TEMPLATE_VARIABLES_ATTRIBUTE,
                java.util.Map.of("id", userId)
        );

        StepVerifier.create(handler.handle(exchange))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(mockWebServer.getRequestCount()).isEqualTo(3);
    }

    @Test
    void handle_AuthServiceUnavailable_ShouldReturn500_AndNotCallUserService() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(503));
        mockWebServer.enqueue(new MockResponse().setResponseCode(503));
        mockWebServer.enqueue(new MockResponse().setResponseCode(503));
        mockWebServer.enqueue(new MockResponse().setResponseCode(503));

        MockServerWebExchange exchange = createExchange("user-fail");

        StepVerifier.create(handler.handle(exchange))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void handle_UserServiceDead_ShouldReturn202_ZombieScenario() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(204));

        for (int i = 0; i < 4; i++) {
            mockWebServer.enqueue(new MockResponse().setResponseCode(500));
        }

        MockServerWebExchange exchange = createExchange("zombie-user");

        StepVerifier.create(handler.handle(exchange))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    }

    private MockServerWebExchange createExchange(String userId) {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.delete("/" + userId).header("Authorization", "token")
        );
        exchange.getAttributes().put(
                org.springframework.cloud.gateway.support.ServerWebExchangeUtils.URI_TEMPLATE_VARIABLES_ATTRIBUTE,
                java.util.Map.of("id", userId)
        );
        return exchange;
    }
}