package cz.bsl.favorites.web;

// Grep anchor: favorites

import cz.bsl.czh.favorites.test.AbstractRestTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for the 4 read-only admin endpoints in {@link AdminFavoritesController}.
 *
 * <p>Seed data is created via the player API ({@code /v1/draw-games/favorite-wagers/...}) in {@code @BeforeEach},
 * then each test hits the admin paths without an {@code X-Player-Id} header — admin endpoints
 * accept {@code playerId} as a path variable.
 */
class AdminFavoritesControllerTest extends AbstractRestTest {

    private static final String ADMIN_TEST_PLAYER = "admin-test-player-001";
    private static final String GROUP             = "3";
    private static final String GAME              = "LOTTO";

    private long seededWagerId;

    @BeforeEach
    void seedData() {
        // Seed group via the legacy name-only body (idempotent: duplicate slot -> 400 tolerated)
        String groupBody = "{\"id\":\"" + GROUP + "\",\"name\":\"Admin Test Group\"}";
        restTemplate.exchange(
                "/v1/draw-games/favorite-wagers/groups",
                HttpMethod.POST,
                new HttpEntity<>(groupBody, asPlayer(ADMIN_TEST_PLAYER)),
                String.class);

        // Seed wager and capture its id (legacy contract: 200 + string id)
        String wagerBody = "{\"name\":\"Admin Test Wager\"," +
                "\"group\":{\"id\":\"" + GROUP + "\",\"name\":\"Admin Test Group\"}," +
                "\"wager\":{\"type\":\"NORMAL\",\"gameName\":\"" + GAME + "\",\"stake\":100,\"price\":100," +
                "\"duration\":1,\"drawNames\":[\"WED\"],\"serialNumber\":\"ADMIN-SN1\"," +
                "\"boards\":[{\"primarySelections\":[\"1\",\"2\",\"3\"]}]}}";
        ResponseEntity<String> wagerResp = restTemplate.exchange(
                "/v1/draw-games/favorite-wagers/wagers",
                HttpMethod.POST,
                new HttpEntity<>(wagerBody, asPlayer(ADMIN_TEST_PLAYER)),
                String.class);

        if (wagerResp.getStatusCode().value() == 200) {
            jamcrest.validateJson(wagerResp.getBody(), "favorites/wager/create.resp.js");
            seededWagerId = Long.parseLong(String.valueOf(jamcrest.access("$.id")));
        }
    }

    /** Admin requests have no X-Player-Id — just Content-Type. */
    private HttpHeaders adminHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    @Test
    void listWagers_returns200PageWithSeededWager() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/admin/v1/player/" + ADMIN_TEST_PLAYER + "/draw-games/favorite-wagers/wagers",
                HttpMethod.GET,
                new HttpEntity<>(adminHeaders()),
                String.class);

        assertEquals(200, response.getStatusCode().value());
        jamcrest.validateJson(response.getBody(), "favorites/admin/wagers.resp.js");
        int count = (int) jamcrest.access("$.totalCount");
        assertTrue(count >= 1, "Expected at least 1 seeded wager, got: " + count);
    }

    @Test
    void listWagers_groupNumberFilter_returnsMatchesOnly() {
        String base = "/admin/v1/player/" + ADMIN_TEST_PLAYER + "/draw-games/favorite-wagers/wagers";
        ResponseEntity<String> hit = restTemplate.exchange(base + "?groupNumber=" + GROUP,
                HttpMethod.GET, new HttpEntity<>(adminHeaders()), String.class);
        assertEquals(200, hit.getStatusCode().value());
        assertTrue(jamcrestCount(hit.getBody()) >= 1);

        ResponseEntity<String> bad = restTemplate.exchange(base + "?groupNumber=abc",
                HttpMethod.GET, new HttpEntity<>(adminHeaders()), String.class);
        assertEquals(400, bad.getStatusCode().value());
    }

    private int jamcrestCount(String body) {
        jamcrest.validateJson(body, "favorites/admin/wagers.resp.js");
        return (int) jamcrest.access("$.totalCount");
    }

    @Test
    void getWager_returns200WithBody() {
        assertTrue(seededWagerId > 0, "seededWagerId not captured — seed failed");

        ResponseEntity<String> response = restTemplate.exchange(
                "/admin/v1/player/" + ADMIN_TEST_PLAYER + "/draw-games/favorite-wagers/wagers/" + seededWagerId,
                HttpMethod.GET,
                new HttpEntity<>(adminHeaders()),
                String.class);

        assertEquals(200, response.getStatusCode().value());
        jamcrest.validateJson(response.getBody(), "favorites/admin/wager-get.resp.js");
    }

    @Test
    void getWager_unknownId_returns404() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/admin/v1/player/" + ADMIN_TEST_PLAYER + "/draw-games/favorite-wagers/wagers/999999999",
                HttpMethod.GET,
                new HttpEntity<>(adminHeaders()),
                String.class);

        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void listGroups_returns200PageWithSeededGroup() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/admin/v1/player/" + ADMIN_TEST_PLAYER + "/draw-games/favorite-wagers/groups",
                HttpMethod.GET,
                new HttpEntity<>(adminHeaders()),
                String.class);

        assertEquals(200, response.getStatusCode().value());
        jamcrest.validateJson(response.getBody(), "favorites/admin/groups.resp.js");
        int count = (int) jamcrest.access("$.totalCount");
        assertTrue(count >= 1, "Expected at least 1 seeded group, got: " + count);
    }

    @Test
    void getGroup_returns200WithBody() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/admin/v1/player/" + ADMIN_TEST_PLAYER + "/draw-games/favorite-wagers/groups/" + GROUP,
                HttpMethod.GET,
                new HttpEntity<>(adminHeaders()),
                String.class);

        assertEquals(200, response.getStatusCode().value());
        jamcrest.validateJson(response.getBody(), "favorites/admin/group-get.resp.js");
    }

    @Test
    void getGroup_unknownGroupNumber_returns404() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/admin/v1/player/" + ADMIN_TEST_PLAYER + "/draw-games/favorite-wagers/groups/9",
                HttpMethod.GET,
                new HttpEntity<>(adminHeaders()),
                String.class);

        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void listWagers_oversizedPlayerId_returns400() {
        String longPlayerId = "x".repeat(65);

        ResponseEntity<String> response = restTemplate.exchange(
                "/admin/v1/player/" + longPlayerId + "/draw-games/favorite-wagers/wagers",
                HttpMethod.GET,
                new HttpEntity<>(adminHeaders()),
                String.class);

        assertEquals(400, response.getStatusCode().value());
    }
}
