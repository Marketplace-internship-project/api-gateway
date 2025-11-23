package io.hohichh.marketplace.gateway.integration;

import io.hohichh.marketplace.gateway.security.JwtValidator;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class GatewayRoutingTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private JwtValidator jwtValidator;

    private static MockWebServer mockBackEnd;

    @BeforeAll
    static void setUp() throws IOException {
        mockBackEnd = new MockWebServer();
        mockBackEnd.start();
    }

    @AfterAll
    static void tearDown() throws IOException {
        mockBackEnd.shutdown();
    }

     @DynamicPropertySource
    static void registerBackendProperties(DynamicPropertyRegistry registry) {
        String mockUrl = mockBackEnd.url("/").toString();

        registry.add("app.services.auth", () -> mockUrl);
        registry.add("app.services.user", () -> mockUrl);
        registry.add("app.services.order", () -> mockUrl);

        registry.add("jwt.access.secret", () -> "mySuperSecretKeyForTestingPurposesOnly12345");
    }

    @Test
    void routeToUserService_ShouldForwardRequestCorrectly() throws InterruptedException {
        when(jwtValidator.validate(anyString())).thenReturn(true);
        Claims dummyClaims = Jwts.claims()
                .subject("user-id-stub")
                .add("role", "USER")
                .build();

        when(jwtValidator.getClaims(anyString())).thenReturn(dummyClaims);

        mockBackEnd.enqueue(new MockResponse()
                .setBody("{\"id\": \"123\", \"name\": \"Ivan\"}")
                .addHeader("Content-Type", "application/json"));


        webTestClient
                .get().uri("/api/v1/users/123")
                .header("Authorization", "Bearer valid_token")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.name").isEqualTo("Ivan");


        RecordedRequest recordedRequest = mockBackEnd.takeRequest();

        assertThat(recordedRequest.getPath()).isEqualTo("/api/v1/users/123");
        assertThat(recordedRequest.getHeader("Authorization")).isEqualTo("Bearer valid_token");
    }

    @Test
    void routeToAuthLogin_ShouldBePublicAndForwarded() throws InterruptedException {
        mockBackEnd.enqueue(new MockResponse().setResponseCode(200));

        webTestClient
                .post().uri("/api/v1/auth/login")
                .exchange()
                .expectStatus().isOk();

        RecordedRequest recordedRequest = mockBackEnd.takeRequest();
        assertThat(recordedRequest.getPath()).isEqualTo("/api/v1/auth/login");
    }

    @Test
    void routeToProducts_ShouldForwardRequestToOrderService() throws InterruptedException {
        when(jwtValidator.validate(anyString())).thenReturn(true);

        Claims dummyClaims = Jwts.claims()
                .subject("user-id")
                .add("role", "USER").build();
        when(jwtValidator.getClaims(anyString())).thenReturn(dummyClaims);

        mockBackEnd.enqueue(new MockResponse()
                .setBody("[{\"id\": \"prod-1\", \"name\": \"Laptop\"}]")
                .addHeader("Content-Type", "application/json"));

        webTestClient
                .get().uri("/api/v1/products")
                .header("Authorization", "Bearer valid_token")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].name").isEqualTo("Laptop");

        RecordedRequest recordedRequest = mockBackEnd.takeRequest();

        assertThat(recordedRequest.getPath()).isEqualTo("/api/v1/products");
        assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        assertThat(recordedRequest.getHeader("Authorization")).isEqualTo("Bearer valid_token");
    }

    @Test
    void routeToOrders_ShouldForwardPostRequestWithBody() throws InterruptedException {
        when(jwtValidator.validate(anyString())).thenReturn(true);
        Claims dummyClaims = Jwts.claims().subject("user-id")
                .add("role", "USER").build();
        when(jwtValidator.getClaims(anyString())).thenReturn(dummyClaims);

        mockBackEnd.enqueue(new MockResponse()
                .setResponseCode(201)
                .setBody("{\"id\": \"order-123\", \"status\": \"CREATED\"}")
                .addHeader("Content-Type", "application/json"));

        String requestBody = "[{\"productId\": \"prod-1\", \"count\": 2}]";

        webTestClient
                .post().uri("/api/v1/orders")
                .header("Authorization", "Bearer valid_token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.status").isEqualTo("CREATED");

        RecordedRequest recordedRequest = mockBackEnd.takeRequest();

        assertThat(recordedRequest.getPath()).isEqualTo("/api/v1/orders");
        assertThat(recordedRequest.getMethod()).isEqualTo("POST");
        assertThat(recordedRequest.getBody().readUtf8()).isEqualTo(requestBody);
    }
}