package cz.bsl.czh.favorites.test;

// Grep anchor: favorites

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
class FavoritesRestSmokeTest extends AbstractRestTest {

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
    void actuatorPrometheus_exposesFavoritesCounters_afterWagerOp() {
        // Fire one wager create (legacy wire shape) to populate the favorites_wager_operation_total counter
        String wagerBody = "{\"name\":\"Prometheus Test\","
                + "\"group\":{\"id\":\"5\",\"name\":\"Prometheus Test Group\"},"
                + "\"wager\":{\"type\":\"NORMAL\",\"gameName\":\"LOTTO\",\"stake\":1,\"price\":1,\"duration\":1,"
                + "\"serialNumber\":\"PROM1\",\"boards\":[{\"primarySelections\":[\"1\",\"2\",\"3\"]}]}}";
        restTemplate.exchange("/v1/draw-games/favorite-wagers/wagers", org.springframework.http.HttpMethod.POST,
                new org.springframework.http.HttpEntity<>(wagerBody, asPlayer("prometheus-smoke-player")),
                String.class);

        ResponseEntity<String> promResp = managementRestTemplate.getForEntity("/actuator/prometheus", String.class);
        assertEquals(200, promResp.getStatusCode().value());
        Assertions.assertTrue(
                promResp.getBody() != null && promResp.getBody().contains("favorites_wager_operation_total"),
                "Prometheus endpoint must expose favorites_wager_operation_total after a wager operation");
    }

    @Test
    void guiIndex_returns200WithCorrectTitle() {
        // The GUI is served on the management context (port 9291 / GuiResourceConfig), not the app
        // context — fetch it via the management RestTemplate. No X-Vision-Base header → the
        // vision-base meta stays empty (direct-on-port pass-through).
        ResponseEntity<String> response = managementRestTemplate.getForEntity("/gui/index.html", String.class);
        assertEquals(200, response.getStatusCode().value());
        Assertions.assertTrue(
                response.getBody() != null && response.getBody().contains("czh-favorites Vision"),
                "GUI index.html must contain the application title");
        Assertions.assertTrue(
                response.getBody().contains("<meta name=\"vision-base\" content=\"\">"),
                "GUI index.html must carry the empty vision-base meta placeholder (no header): " + response.getBody());
    }
}
