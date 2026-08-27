package cz.bsl.favourites.web;

// Grep anchor: favourites

import cz.bsl.czh.favourites.test.AbstractRestTest;
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
 * Integration tests for the 4 read-only admin endpoints in {@link AdminFavouritesController}.
 *
 * <p>Seed data is created via the player API ({@code /favourites/...}) in {@code @BeforeEach},
 * then each test hits the admin paths without an {@code X-Player-Id} header — admin endpoints
 * accept {@code playerId} as a path variable.
 */
class AdminFavouritesControllerTest extends AbstractRestTest {

    private static final String ADMIN_TEST_PLAYER = "admin-test-player-001";
    private static final String GROUP             = "3";
    private static final String GAME              = "LOTTO";

    private long seededWagerId;

    @BeforeEach
    void seedData() {
        // Seed group (idempotent — duplicate returns 400 which we tolerate)
        String groupBody = "{\"groupNumber\":\"" + GROUP + "\",\"groupName\":\"Admin Test Group\"}";
        restTemplate.exchange(
                "/favourites/groups",
                HttpMethod.POST,
                new HttpEntity<>(groupBody, asPlayer(ADMIN_TEST_PLAYER)),
                String.class);

        // Seed wager and capture its id
        String wagerBody = "{\"groupNumber\":\"" + GROUP + "\",\"wagerName\":\"Admin Test Wager\"," +
                "\"wager\":{\"gameName\":\"" + GAME + "\",\"stake\":100,\"price\":100,\"duration\":1," +
                "\"serialNumber\":\"ADMIN-SN1\",\"boards\":[[1,2,3]]}}";
        ResponseEntity<String> wagerResp = restTemplate.exchange(
                "/favourites/wagers",
                HttpMethod.POST,
                new HttpEntity<>(wagerBody, asPlayer(ADMIN_TEST_PLAYER)),
                String.class);

        if (wagerResp.getStatusCode().value() == 201) {
            jamcrest.validateJson(wagerResp.getBody(), "favourites/wager/create.resp.js");
            seededWagerId = (long) (int) jamcrest.access("$.id");
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
                "/admin/favourites/players/" + ADMIN_TEST_PLAYER + "/wagers",
                HttpMethod.GET,
                new HttpEntity<>(adminHeaders()),
                String.class);

        assertEquals(200, response.getStatusCode().value());
        jamcrest.validateJson(response.getBody(), "favourites/admin/wagers.resp.js");
        int count = (int) jamcrest.access("$.totalCount");
        assertTrue(count >= 1, "Expected at least 1 seeded wager, got: " + count);
    }

    @Test
    void getWager_returns200WithBody() {
        assertTrue(seededWagerId > 0, "seededWagerId not captured — seed failed");

        ResponseEntity<String> response = restTemplate.exchange(
                "/admin/favourites/players/" + ADMIN_TEST_PLAYER + "/wagers/" + seededWagerId,
                HttpMethod.GET,
                new HttpEntity<>(adminHeaders()),
                String.class);

        assertEquals(200, response.getStatusCode().value());
        jamcrest.validateJson(response.getBody(), "favourites/admin/wager-get.resp.js");
    }

    @Test
    void getWager_unknownId_returns404() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/admin/favourites/players/" + ADMIN_TEST_PLAYER + "/wagers/999999999",
                HttpMethod.GET,
                new HttpEntity<>(adminHeaders()),
                String.class);

        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void listGroups_returns200PageWithSeededGroup() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/admin/favourites/players/" + ADMIN_TEST_PLAYER + "/groups",
                HttpMethod.GET,
                new HttpEntity<>(adminHeaders()),
                String.class);

        assertEquals(200, response.getStatusCode().value());
        jamcrest.validateJson(response.getBody(), "favourites/admin/groups.resp.js");
        int count = (int) jamcrest.access("$.totalCount");
        assertTrue(count >= 1, "Expected at least 1 seeded group, got: " + count);
    }

    @Test
    void getGroup_returns200WithBody() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/admin/favourites/players/" + ADMIN_TEST_PLAYER + "/groups/" + GROUP,
                HttpMethod.GET,
                new HttpEntity<>(adminHeaders()),
                String.class);

        assertEquals(200, response.getStatusCode().value());
        jamcrest.validateJson(response.getBody(), "favourites/admin/group-get.resp.js");
    }

    @Test
    void getGroup_unknownGroupNumber_returns404() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/admin/favourites/players/" + ADMIN_TEST_PLAYER + "/groups/9",
                HttpMethod.GET,
                new HttpEntity<>(adminHeaders()),
                String.class);

        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void listWagers_oversizedPlayerId_returns400() {
        String longPlayerId = "x".repeat(65);

        ResponseEntity<String> response = restTemplate.exchange(
                "/admin/favourites/players/" + longPlayerId + "/wagers",
                HttpMethod.GET,
                new HttpEntity<>(adminHeaders()),
                String.class);

        assertEquals(400, response.getStatusCode().value());
    }
}
