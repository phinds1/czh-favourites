package cz.bsl.czh.favourites.test;

// Grep anchor: favourites

import cz.bsl.czh.favourites.config.TestDatabaseConfig;
import cz.bsl.favourites.FavouritesApplication;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.io.IOException;

/**
 * Base class for favourites REST integration tests. Boots the full {@link FavouritesApplication}
 * on a random port against HSQLDB (via {@link TestDatabaseConfig}) and provides a
 * {@link RestTemplate} that does NOT throw on 4xx/5xx — tests read the response body and assert
 * status codes directly.
 *
 * <p>Spring Boot 4 removed {@code TestRestTemplate}; this is the equivalent pattern (from
 * {@code czh-money}'s {@code AbstractIntegrationTest}).
 *
 * <p>Subclasses add a {@code @Test} method for every endpoint they exercise. Use
 * {@link #asPlayer(String)} to build request headers for player-facing endpoints.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {FavouritesApplication.class, TestDatabaseConfig.class}
)
@ActiveProfiles("test")
public abstract class AbstractRestTest {

    @LocalServerPort
    protected int port;

    @LocalManagementPort
    protected int managementPort;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected JamcrestUtils jamcrest;

    protected RestTemplate restTemplate;
    protected RestTemplate managementRestTemplate;

    @BeforeEach
    void setUpRestTemplate() {
        restTemplate = new RestTemplate();
        restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse response) throws IOException {
                return false;
            }
        });
        restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory("http://localhost:" + port));

        managementRestTemplate = new RestTemplate();
        managementRestTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse response) throws IOException {
                return false;
            }
        });
        managementRestTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory("http://localhost:" + managementPort));
    }

    @AfterEach
    void resetJamcrest() {
        jamcrest.reset();
    }

    /**
     * Returns headers with {@code X-Player-Id} set. Use for all player-facing endpoint calls.
     * The gateway pre-authenticates the player before forwarding to this service.
     */
    protected HttpHeaders asPlayer(String playerId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Player-Id", playerId);
        return headers;
    }
}
