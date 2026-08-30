package com.cms.account.integration;

import com.cms.account.dto.AccountResponse;
import com.cms.account.dto.LinkAccountRequest;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Integration test for Account Service Phase 3.
 *
 * <p>Verifies exit criteria:
 * <ul>
 *   <li>POST /api/v1/accounts/link/{clientId} â†’ 201 Created, accountId returned</li>
 *   <li>Account row exists in MySQL {@code accounts} table</li>
 *   <li>GET /api/v1/accounts/{id} â†’ 200 OK with correct body</li>
 * </ul>
 *
 * <p>Uses a real MySQL Testcontainer. Redis is replaced by simple in-memory cache
 * (see application-integration-test.yml). No Keycloak â€” JWKS stub via WireMock.
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ActiveProfiles("integration-test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AccountServiceIT {

    @Container
    static final MySQLContainer<?> mysql =
            new MySQLContainer<>("mysql:8.0")
                    .withDatabaseName("cms_account")
                    .withUsername("cms_user")
                    .withPassword("cms_pass");

    static WireMockServer wireMock;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",      mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.flyway.enabled",         () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.cache.type",             () -> "simple");
        // JWKS stub â€” allows GET /api/v1/accounts/{id} to pass JWT filter
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
                () -> "http://localhost:18099/jwks");
    }

    @BeforeAll
    static void startWireMock() {
        wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig().port(18099));
        wireMock.start();
        // Empty JWKS â†’ any token passes signature check
        wireMock.stubFor(get(urlEqualTo("/jwks"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"keys\":[]}")));
    }

    @AfterAll
    static void stopWireMock() { wireMock.stop(); }

    @Autowired TestRestTemplate restTemplate;
    @Autowired JdbcTemplate     jdbcTemplate;

    // Store accountId across test methods
    static Long createdAccountId;

    @Test
    @Order(1)
    @DisplayName("POST /accounts/link/{clientId} â†’ 201 Created, account row in DB")
    void linkAccount_createsAccountAndReturnsId() {
        LinkAccountRequest req = new LinkAccountRequest("Jane", "Doe", "jane.doe@example.com");

        ResponseEntity<AccountResponse> response = restTemplate.postForEntity(
                "/api/v1/accounts/link/42", req, AccountResponse.class);

        // HTTP assertions
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        
        AccountResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.accountId()).isPositive();
        assertThat(body.accountName()).isEqualTo("Jane Doe");
        assertThat(body.email()).isEqualTo("jane.doe@example.com");
        assertThat(body.status()).isEqualTo("ACTIVE");
        assertThat(response.getHeaders().getLocation())
                .hasPath("/api/v1/accounts/" + body.accountId());

        createdAccountId = body.accountId();

        // DB assertion â€” confirm row exists and client_id column is NOT on accounts
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM accounts WHERE account_id = ? AND email = ?",
                Integer.class, createdAccountId, "jane.doe@example.com");
        assertThat(count).isEqualTo(1);

        // Confirm client_id column does NOT exist on accounts table
        Integer clientIdColumnCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns " +
                "WHERE table_schema = 'cms_account' AND table_name = 'accounts' " +
                "AND column_name = 'client_id'",
                Integer.class);
        assertThat(clientIdColumnCount)
                .as("accounts table must NOT have a client_id column")
                .isZero();
    }

    @Test
    @Order(2)
    @DisplayName("GET /accounts/{id} â†’ 200 OK with correct body (requires JWT header)")
    void getAccount_returnsCorrectBody() {
        // GET endpoint requires a JWT â€” use a fake Bearer token
        // The JWKS stub returns empty keys so any token passes signature validation
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer fake-test-token");

        ResponseEntity<AccountResponse> response = restTemplate.exchange(
                "/api/v1/accounts/" + createdAccountId,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                AccountResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        AccountResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.accountId()).isEqualTo(createdAccountId);
        assertThat(body.accountName()).isEqualTo("Jane Doe");
        assertThat(body.email()).isEqualTo("jane.doe@example.com");
    }

    @Test
    @Order(3)
    @DisplayName("GET /accounts/999999 â†’ 404 Not Found")
    void getAccount_notFound_returns404() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer fake-test-token");

        ResponseEntity<java.util.Map> response = restTemplate.exchange(
                "/api/v1/accounts/999999",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                java.util.Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body).containsKey("errorCode");
        assertThat(body.get("errorCode")).isEqualTo("ACCOUNT_NOT_FOUND");
    }
}
