package cz.bsl.favorites.web;

// Grep anchor: favorites

import cz.bsl.czh.favorites.test.AbstractRestTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for the 5 group endpoints in {@link FavoritesController}.
 */
class FavoritesGroupControllerTest extends AbstractRestTest {

    private static final String PLAYER = "group-ctrl-test-player";
    private static final String GROUP  = "2";

    @AfterEach
    void cleanUpGroups() {
        jdbcTemplate.update("DELETE FROM GIS_FAV_WAGER WHERE PLAYER_ID = ?", PLAYER);
        jdbcTemplate.update("DELETE FROM GIS_FAV_GROUP WHERE PLAYER_ID = ?", PLAYER);
    }

    @Test
    void createGroup_returns201WithBody() {
        String body = jamcrest.loadJson("favorites/group/create.req.js",
                "groupNumber", GROUP);

        ResponseEntity<String> response = restTemplate.exchange(
                "/v1/draw-games/favorite-wagers/groups",
                HttpMethod.POST,
                new HttpEntity<>(body, asPlayer(PLAYER)),
                String.class);

        assertEquals(201, response.getStatusCode().value());
        assertNotNull(response.getHeaders().getLocation());
        jamcrest.validateJson(response.getBody(), "favorites/group/create.resp.js");
    }

    @Test
    void getGroup_returns200WithBody() {
        String createBody = jamcrest.loadJson("favorites/group/create.req.js",
                "groupNumber", GROUP);
        ResponseEntity<String> created = restTemplate.exchange(
                "/v1/draw-games/favorite-wagers/groups",
                HttpMethod.POST,
                new HttpEntity<>(createBody, asPlayer(PLAYER)),
                String.class);
        assertEquals(201, created.getStatusCode().value());
        jamcrest.validateJson(created.getBody(), "favorites/group/create.resp.js");

        ResponseEntity<String> response = restTemplate.exchange(
                "/v1/draw-games/favorite-wagers/groups/" + GROUP,
                HttpMethod.GET,
                new HttpEntity<>(asPlayer(PLAYER)),
                String.class);

        assertEquals(200, response.getStatusCode().value());
        jamcrest.validateJson(response.getBody(), "favorites/group/get.resp.js");
    }

    @Test
    void updateGroup_returns200WithUpdatedName() {
        String createBody = jamcrest.loadJson("favorites/group/create.req.js",
                "groupNumber", GROUP);
        ResponseEntity<String> created = restTemplate.exchange(
                "/v1/draw-games/favorite-wagers/groups",
                HttpMethod.POST,
                new HttpEntity<>(createBody, asPlayer(PLAYER)),
                String.class);
        assertEquals(201, created.getStatusCode().value());
        jamcrest.validateJson(created.getBody(), "favorites/group/create.resp.js");

        String updateBody = jamcrest.loadJson("favorites/group/update.req.js",
                "groupNumber", GROUP);
        ResponseEntity<String> response = restTemplate.exchange(
                "/v1/draw-games/favorite-wagers/groups/" + GROUP,
                HttpMethod.PUT,
                new HttpEntity<>(updateBody, asPlayer(PLAYER)),
                String.class);

        assertEquals(200, response.getStatusCode().value());
        jamcrest.validateJson(response.getBody(), "favorites/group/get.resp.js");
        assertTrue(response.getBody().contains("Updated Group Name"), "groupName should be updated");
    }

    @Test
    void deleteGroup_returns204_thenGet_returns404() {
        String createBody = jamcrest.loadJson("favorites/group/create.req.js",
                "groupNumber", GROUP);
        ResponseEntity<String> created = restTemplate.exchange(
                "/v1/draw-games/favorite-wagers/groups",
                HttpMethod.POST,
                new HttpEntity<>(createBody, asPlayer(PLAYER)),
                String.class);
        assertEquals(201, created.getStatusCode().value());
        jamcrest.validateJson(created.getBody(), "favorites/group/create.resp.js");

        ResponseEntity<String> deleteResp = restTemplate.exchange(
                "/v1/draw-games/favorite-wagers/groups/" + GROUP,
                HttpMethod.DELETE,
                new HttpEntity<>(asPlayer(PLAYER)),
                String.class);
        assertEquals(204, deleteResp.getStatusCode().value());

        ResponseEntity<String> getAfterDelete = restTemplate.exchange(
                "/v1/draw-games/favorite-wagers/groups/" + GROUP,
                HttpMethod.GET,
                new HttpEntity<>(asPlayer(PLAYER)),
                String.class);
        assertEquals(404, getAfterDelete.getStatusCode().value());
    }

    @Test
    void listGroups_returns200PageWithItems() {
        for (int i = 0; i < 2; i++) {
            String body = jamcrest.loadJson("favorites/group/create.req.js",
                    "groupNumber", GROUP);
            restTemplate.exchange(
                    "/v1/draw-games/favorite-wagers/groups",
                    HttpMethod.POST,
                    new HttpEntity<>(body, asPlayer(PLAYER)),
                    String.class);
        }

        ResponseEntity<String> response = restTemplate.exchange(
                "/v1/draw-games/favorite-wagers/groups",
                HttpMethod.GET,
                new HttpEntity<>(asPlayer(PLAYER)),
                String.class);

        assertEquals(200, response.getStatusCode().value());
        jamcrest.validateJson(response.getBody(), "favorites/group/list.resp.js");
    }
}
