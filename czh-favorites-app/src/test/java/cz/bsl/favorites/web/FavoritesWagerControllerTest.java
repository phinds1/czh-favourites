package cz.bsl.favorites.web;

// Grep anchor: favorites

import cz.bsl.czh.favorites.test.AbstractRestTest;
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
 * <p>Legacy front-end contract (gisorch): POST returns 200 with a string {@code id}; the
 * body carries {@code name}, {@code group {id, name}} and the full wager structure; a
 * group referenced by id that does not exist yet is created on the fly.
 */
class FavoritesWagerControllerTest extends AbstractRestTest {

    private static final String PLAYER = "wager-ctrl-test-player";
    private static final String GROUP  = "1";
    private static final String GAME   = "LOTTO";

    /** POST the legacy sportka-style body; returns the created favorite's string id. */
    private String createWager() {
        String body = jamcrest.loadJson("favorites/wager/create.req.js",
                "groupNumber", GROUP,
                "gameName", GAME);
        ResponseEntity<String> created = restTemplate.exchange(
                "/v1/draw-games/favorite-wagers/wagers",
                HttpMethod.POST,
                new HttpEntity<>(body, asPlayer(PLAYER)),
                String.class);
        assertEquals(200, created.getStatusCode().value(), "legacy contract: POST returns 200");
        jamcrest.validateJson(created.getBody(), "favorites/wager/create.resp.js");
        return String.valueOf(jamcrest.access("$.id"));
    }

    @Test
    void createWager_returns200WithBody() {
        String id = createWager();

        assertNotNull(id);
        assertTrue(Long.parseLong(id) > 0, "id must be the numeric key as a string, got: " + id);
    }

    @Test
    void createWager_roundTripsLegacyWager() {
        String body = jamcrest.loadJson("favorites/wager/create.req.js",
                "groupNumber", GROUP,
                "gameName", GAME);
        ResponseEntity<String> created = restTemplate.exchange(
                "/v1/draw-games/favorite-wagers/wagers",
                HttpMethod.POST,
                new HttpEntity<>(body, asPlayer(PLAYER)),
                String.class);
        assertEquals(200, created.getStatusCode().value());
        jamcrest.validateJson(created.getBody(), "favorites/wager/create.resp.js");

        String id = String.valueOf(jamcrest.access("$.id"));
        ResponseEntity<String> response = restTemplate.exchange(
                "/v1/draw-games/favorite-wagers/wagers/" + id,
                HttpMethod.GET,
                new HttpEntity<>(asPlayer(PLAYER)),
                String.class);

        assertEquals(200, response.getStatusCode().value());
        jamcrest.validateJson(response.getBody(), "favorites/wager/get.resp.js");
        // the wager round-trips verbatim: boards are objects with primarySelections,
        // drawNames and type survive
        assertTrue(response.getBody().contains("primarySelections"), "boards must be board objects");
        assertTrue(response.getBody().contains("drawNames"), "drawNames must survive");
        assertTrue(response.getBody().contains("NORMAL"), "type must survive");
    }

    @Test
    void getWager_returns200WithBody() {
        String id = createWager();

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
        String id = createWager();

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
        assertTrue(response.getBody().contains("Updated Name"), "name should be updated");
    }

    @Test
    void deleteWager_returns204_thenGet_returns404() {
        String id = createWager();

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
    void listWagers_returns200PageWithFavoriteWagers() {
        for (int i = 0; i < 2; i++) {
            createWager();
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
        assertTrue(response.getBody().contains("favoriteWagers"), "legacy list field name");
    }
}
