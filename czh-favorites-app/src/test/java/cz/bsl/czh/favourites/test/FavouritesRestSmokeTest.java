package cz.bsl.czh.favourites.test;

// Grep anchor: favourites

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Smoke test — verifies the Spring context boots cleanly, the HSQLDB schema tables exist, and the
 * Actuator health endpoint responds 200. Extends {@link AbstractRestTest} so the full application
 * context (app + {@code TestDatabaseConfig}) is loaded.
 */
class FavouritesRestSmokeTest extends AbstractRestTest {

    @Test
    void contextLoads_tablesExist() {
        int groupCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE UPPER(TABLE_NAME) = 'GIS_FAV_GROUP'",
                Integer.class);
        assertEquals(1, groupCount, "GIS_FAV_GROUP table must exist");

        int wagerCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE UPPER(TABLE_NAME) = 'GIS_FAV_WAGER'",
                Integer.class);
        assertEquals(1, wagerCount, "GIS_FAV_WAGER table must exist");
    }

    @Test
    void actuatorHealth_returns200() {
        ResponseEntity<String> response = managementRestTemplate.getForEntity("/actuator/health", String.class);
        assertEquals(200, response.getStatusCode().value(),
                "Actuator /health must return 200");
    }

    @Test
    void actuatorPrometheus_exposesFavouritesCounters_afterWagerOp() {
        // Fire one wager create to populate the favourites_wager_operation_total counter
        String groupBody = "{\"groupNumber\":\"5\",\"groupName\":\"Prometheus Test Group\"}";
        restTemplate.exchange("/favourites/groups", org.springframework.http.HttpMethod.POST,
                new org.springframework.http.HttpEntity<>(groupBody, asPlayer("prometheus-smoke-player")),
                String.class);

        String wagerBody = "{\"groupNumber\":\"5\",\"wagerName\":\"Prometheus Test\","
                + "\"wager\":{\"gameName\":\"LOTTO\",\"stake\":1,\"price\":1,\"duration\":1,"
                + "\"serialNumber\":\"PROM1\",\"boards\":[[1,2,3]]}}";
        restTemplate.exchange("/favourites/wagers", org.springframework.http.HttpMethod.POST,
                new org.springframework.http.HttpEntity<>(wagerBody, asPlayer("prometheus-smoke-player")),
                String.class);

        ResponseEntity<String> promResp = managementRestTemplate.getForEntity("/actuator/prometheus", String.class);
        assertEquals(200, promResp.getStatusCode().value());
        Assertions.assertTrue(
                promResp.getBody() != null && promResp.getBody().contains("favourites_wager_operation_total"),
                "Prometheus endpoint must expose favourites_wager_operation_total after a wager operation");
    }

    @Test
    void guiIndex_returns200WithCorrectTitle() {
        ResponseEntity<String> response = restTemplate.getForEntity("/gui/index.html", String.class);
        assertEquals(200, response.getStatusCode().value());
        Assertions.assertTrue(
                response.getBody() != null && response.getBody().contains("czh-favourites Vision"),
                "GUI index.html must contain the application title");
    }
}
