package com.cms.client.integration;

import com.cms.client.config.IntegrationTestSecurityConfig;
import com.cms.client.dto.request.CreateClientRequest;
import com.cms.client.dto.response.ClientResponse;
import com.cms.client.domain.enums.ClientTier;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Testcontainers integration test for the full create-client flow.
 *
 * <p>Uses real MySQL and Kafka containers. Account Service HTTP call is mocked
 * via WireMock (no second Spring context needed).
 *
 * <p>Exit criteria verified:
 * <ul>
 *   <li>POST /clients â†’ 201 with populated client_id and account_id</li>
 *   <li>Row exists in MySQL {@code clients} table with account_id set</li>
 *   <li>ClientOnboardedEvent appears on Kafka {@code client-onboarded} topic</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ActiveProfiles("integration-test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Import(IntegrationTestSecurityConfig.class)
class CreateClientIT {

    @Container
    static final MySQLContainer<?> mysql =
            new MySQLContainer<>("mysql:8.0")
                    .withDatabaseName("cms_client")
                    .withUsername("cms_user")
                    .withPassword("cms_pass");

    @Container
    static final KafkaContainer kafka =
            new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    static WireMockServer wireMock;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",      mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.flyway.enabled",           () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto",   () -> "validate");
        registry.add("spring.cache.type",               () -> "simple");
        // Point AccountServiceClient at WireMock
        registry.add("account-service.url", () -> "http://localhost:18099");
        // Skip JWT validation â€” no Keycloak in integration tests
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
                () -> "http://localhost:18099/jwks");
    }

    @BeforeAll
    static void startWireMock() {
        wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig().port(18099));
        wireMock.start();
        // Stub Account Service link endpoint â€” matches any clientId in the path
        wireMock.stubFor(post(urlPathMatching("/api/v1/accounts/link/.*"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"accountId\": 999, \"accountName\": \"Jane Smith\", \"email\": \"jane.smith@example.com\", \"status\": \"ACTIVE\"}")));
        // Stub JWKS endpoint (Spring Security calls this to validate JWT signature)
        wireMock.stubFor(get(urlEqualTo("/jwks"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"keys\":[]}")));
    }

    @AfterAll
    static void stopWireMock() {
        wireMock.stop();
    }

    @Autowired TestRestTemplate restTemplate;
    @Autowired JdbcTemplate     jdbcTemplate;

    @Test
    @Order(1)
    @DisplayName("POST /clients â†’ 201, account_id populated, DB row exists, Kafka event published")
    void createClient_fullFlow() throws Exception {
        // â”€â”€ Arrange â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        CreateClientRequest req = CreateClientRequest.builder()
                .firstName("Jane")
                .lastName("Smith")
                .email("jane.smith@example.com")
                .tier(ClientTier.PREMIUM)
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // Provide a minimal JWT that passes resource server filter
        // In integration-test profile, security is relaxed (see application-integration-test.yml)
        HttpEntity<CreateClientRequest> entity = new HttpEntity<>(req, headers);

        // â”€â”€ Act â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        ResponseEntity<ClientResponse> response = restTemplate.postForEntity(
                "/api/v1/clients", entity, ClientResponse.class);

        // â”€â”€ Assert: HTTP 201 â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getClientId()).isPositive();
        assertThat(response.getBody().getAccountId()).isEqualTo(999L);
        assertThat(response.getBody().getEmail()).isEqualTo("jane.smith@example.com");

        Long clientId = response.getBody().getClientId();

        // â”€â”€ Assert: DB row â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM clients WHERE client_id = ? AND account_id = 999",
                Integer.class, clientId);
        assertThat(count).isEqualTo(1);

        // â”€â”€ Assert: Kafka event â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        boolean eventReceived = pollKafkaForEvent("client-onboarded", clientId, kafka.getBootstrapServers());
        assertThat(eventReceived).as("ClientOnboardedEvent should appear on Kafka topic").isTrue();

        // â”€â”€ Assert: WireMock received the link call â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        wireMock.verify(1, postRequestedFor(urlPathMatching("/api/v1/accounts/link/.*")));
    }

    @Test
    @Order(2)
    @DisplayName("POST /clients with duplicate email â†’ 409 Conflict")
    void createClient_duplicateEmail_returns409() {
        CreateClientRequest req = CreateClientRequest.builder()
                .firstName("Jane")
                .lastName("Smith")
                .email("jane.smith@example.com") // same email as Order(1)
                .tier(ClientTier.STANDARD)
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/v1/clients", new HttpEntity<>(req, headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).containsKey("errorCode");
        assertThat(response.getBody().get("errorCode")).isEqualTo("DUPLICATE_EMAIL");
    }

    // â”€â”€ Helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private boolean pollKafkaForEvent(String topic, Long clientId, String bootstrapServers)
            throws InterruptedException {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "it-consumer-" + System.currentTimeMillis());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(List.of(topic));
            long deadline = System.currentTimeMillis() + 10_000; // 10 s timeout
            while (System.currentTimeMillis() < deadline) {
                for (ConsumerRecord<String, String> record : consumer.poll(Duration.ofSeconds(1))) {
                    if (record.value().contains("\"clientId\":" + clientId)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
