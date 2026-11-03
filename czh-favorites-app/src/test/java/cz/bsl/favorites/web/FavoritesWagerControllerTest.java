package cz.bsl.favorites.web;

// Grep anchor: favorites

import cz.bsl.czh.favorites.test.AbstractRestTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for the 5 wager endpoints in {@link FavoritesController}.
 * Extends {@link AbstractRestTest} — boots the full Spring context on a random port.
 *
 * <p>Each test is self-contained: {@code @BeforeEach} creates a group so wager CRUD can proceed.
 * DB is shared across the test run (HSQLDB persistent connection) so player IDs are unique per test.
 */
class FavoritesWagerControllerTest extends AbstractRestTest {

    private static final String PLAYER = "wager-ctrl-test-player";
    private static final String GROUP  = "1";
    private static final String GAME   = "LOTTO";

    @BeforeEach
    void seedGroup() {
        String body = "{\"groupNumber\":\"" + GROUP + "\",\"groupName\":\"Test Group\"}";
        ResponseEntity<String> resp = restTemplate.exchange(
                "/v1/draw-games/favorite-wagers/groups",
                HttpMethod.POST,
                new HttpEntity<>(body, asPlayer(PLAYER)),
                String.class);
        // 201 (first run) or 400 duplicate (subsequent runs when context is reused) — both are fine
        assertTrue(resp.getStatusCode().value() == 201 || resp.getStatusCode().value() == 400,
                "Group seed returned unexpected status: " + resp.getStatusCode());
    }

    @Test
    void createWager_returns201WithBody() {
        String body = jamcrest.loadJson("favorites/wager/create.req.js",
                "groupNumber", GROUP,
                "gameName", GAME);

        ResponseEntity<String> response = restTemplate.exchange(
                "/v1/draw-games/favorite-wagers/wagers",
                HttpMethod.POST,
                new HttpEntity<>(body, asPlayer(PLAYER)),
                String.class);

        assertEquals(201, response.getStatusCode().value());
        assertNotNull(response.getHeaders().getLocation());
        jamcrest.validateJson(response.getBody(), "favorites/wager/create.resp.js");
    }

    @Test
    void getWager_returns200WithBody() {
        String createBody = jamcrest.loadJson("favorites/wager/create.req.js",
                "groupNumber", GROUP,
                "gameName", GAME);
        ResponseEntity<String> created = restTemplate.exchange(
                "/v1/draw-games/favorite-wagers/wagers",
                HttpMethod.POST,
                new HttpEntity<>(createBody, asPlayer(PLAYER)),
                String.class);
        assertEquals(201, created.getStatusCode().value());
        jamcrest.validateJson(created.getBody(), "favorites/wager/create.resp.js");
        long id = (long) (int) jamcrest.access("$.id");

        ResponseEntity<String> response = restTemplate.exchange(
                "/v1/draw-games/favorite-wagers/wagers/" + id,
                HttpMethod.GET,
                new HttpEntity<>(asPlayer(PLAYER)),
                String.class);

        assertEquals(200, response.getStatusCode().value());
        jamcrest.validateJson(response.getBody(), "favorites/wager/get.resp.js");
    }

    @Test
    void updateWager_returns200WithUpdatedFields() {
        String createBody = jamcrest.loadJson("favorites/wager/create.req.js",
                "groupNumber", GROUP,
                "gameName", GAME);
        ResponseEntity<String> created = restTemplate.exchange(
                "/v1/draw-games/favorite-wagers/wagers",
                HttpMethod.POST,
                new HttpEntity<>(createBody, asPlayer(PLAYER)),
                String.class);
        assertEquals(201, created.getStatusCode().value());
        jamcrest.validateJson(created.getBody(), "favorites/wager/create.resp.js");
        long id = (long) (int) jamcrest.access("$.id");

        String updateBody = jamcrest.loadJson("favorites/wager/update.req.js",
                "groupNumber", GROUP,
                "gameName", GAME);
        ResponseEntity<String> response = restTemplate.exchange(
                "/v1/draw-games/favorite-wagers/wagers/" + id,
                HttpMethod.PUT,
                new HttpEntity<>(updateBody, asPlayer(PLAYER)),
                String.class);

        assertEquals(200, response.getStatusCode().value());
        jamcrest.validateJson(response.getBody(), "favorites/wager/get.resp.js");
        assertTrue(response.getBody().contains("Updated Name"), "wagerName should be updated");
    }

    @Test
    void deleteWager_returns204_thenGet_returns404() {
        String createBody = jamcrest.loadJson("favorites/wager/create.req.js",
                "groupNumber", GROUP,
                "gameName", GAME);
        ResponseEntity<String> created = restTemplate.exchange(
                "/v1/draw-games/favorite-wagers/wagers",
                HttpMethod.POST,
                new HttpEntity<>(createBody, asPlayer(PLAYER)),
                String.class);
        assertEquals(201, created.getStatusCode().value());
        jamcrest.validateJson(created.getBody(), "favorites/wager/create.resp.js");
        long id = (long) (int) jamcrest.access("$.id");

        ResponseEntity<String> deleteResp = restTemplate.exchange(
                "/v1/draw-games/favorite-wagers/wagers/" + id,
                HttpMethod.DELETE,
                new HttpEntity<>(asPlayer(PLAYER)),
                String.class);
        assertEquals(204, deleteResp.getStatusCode().value());

        ResponseEntity<String> getAfterDelete = restTemplate.exchange(
                "/v1/draw-games/favorite-wagers/wagers/" + id,
                HttpMethod.GET,
                new HttpEntity<>(asPlayer(PLAYER)),
                String.class);
        assertEquals(404, getAfterDelete.getStatusCode().value());
    }

    @Test
    void listWagers_returns200PageWithItems() {
        // Create two wagers
        for (int i = 0; i < 2; i++) {
            String body = jamcrest.loadJson("favorites/wager/create.req.js",
                    "groupNumber", GROUP,
                    "gameName", GAME);
            ResponseEntity<String> resp = restTemplate.exchange(
                    "/v1/draw-games/favorite-wagers/wagers",
                    HttpMethod.POST,
                    new HttpEntity<>(body, asPlayer(PLAYER)),
                    String.class);
            assertEquals(201, resp.getStatusCode().value());
        }

        ResponseEntity<String> response = restTemplate.exchange(
                "/v1/draw-games/favorite-wagers/wagers",
                HttpMethod.GET,
                new HttpEntity<>(asPlayer(PLAYER)),
                String.class);

        assertEquals(200, response.getStatusCode().value());
        jamcrest.validateJson(response.getBody(), "favorites/wager/list.resp.js");
        int count = (int) jamcrest.access("$.totalCount");
        assertTrue(count >= 2, "Expected at least 2 wagers in list, got: " + count);
    }
}
