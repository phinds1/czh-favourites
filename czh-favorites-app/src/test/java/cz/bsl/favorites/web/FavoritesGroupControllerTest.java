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
 *
 * <p>Legacy front-end contract (gisorch): a group create body carries {@code name} only;
 * the service auto-assigns the slot number and returns it as the string {@code id}.
 */
class FavoritesGroupControllerTest extends AbstractRestTest {

    private static final String PLAYER = "group-ctrl-test-player";

    @AfterEach
    void cleanUpGroups() {
        jdbcTemplate.update("DELETE FROM GIS_FAV_WAGER WHERE PLAYER_ID = ?", PLAYER);
        jdbcTemplate.update("DELETE FROM GIS_FAV_GROUP WHERE PLAYER_ID = ?", PLAYER);
    }

    /** POST the legacy name-only body; returns the auto-assigned slot id as a string. */
    private String createGroup(String groupName) {
        String body = jamcrest.loadJson("favorites/group/create.req.js", "groupName", groupName);
        ResponseEntity<String> response = restTemplate.exchange(
                "/v1/draw-games/favorite-wagers/groups",
                HttpMethod.POST,
                new HttpEntity<>(body, asPlayer(PLAYER)),
                String.class);
        assertEquals(200, response.getStatusCode().value(), "legacy contract: POST returns 200");
        jamcrest.validateJson(response.getBody(), "favorites/group/create.resp.js");
        return String.valueOf(jamcrest.access("$.id"));
    }

    @Test
    void createGroup_legacyNameOnlyBody_returns200WithAutoAssignedId() {
        String id = createGroup("My Lotto Group");

        assertNotNull(id);
        assertTrue(Long.parseLong(id) > 0, "id must be the auto-assigned slot number, got: " + id);
    }

    @Test
    void createGroup_withExplicitId_returns200WithThatId() {
        String body = "{\"id\":\"2\",\"name\":\"Explicit Group\"}";

        ResponseEntity<String> response = restTemplate.exchange(
                "/v1/draw-games/favorite-wagers/groups",
                HttpMethod.POST,
                new HttpEntity<>(body, asPlayer(PLAYER)),
                String.class);

        assertEquals(200, response.getStatusCode().value());
        jamcrest.validateJson(response.getBody(), "favorites/group/create.resp.js");
        assertEquals("2", String.valueOf(jamcrest.access("$.id")));
    }

    @Test
    void createGroup_duplicateId_returns400() {
        createGroup("First Group");

        String body = "{\"id\":\"1\",\"name\":\"Duplicate Group\"}";
        ResponseEntity<String> response = restTemplate.exchange(
                "/v1/draw-games/favorite-wagers/groups",
                HttpMethod.POST,
                new HttpEntity<>(body, asPlayer(PLAYER)),
                String.class);

        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void getGroup_returns200WithBody() {
        String id = createGroup("My Lotto Group");

        ResponseEntity<String> response = restTemplate.exchange(
                "/v1/draw-games/favorite-wagers/groups/" + id,
                HttpMethod.GET,
                new HttpEntity<>(asPlayer(PLAYER)),
                String.class);

        assertEquals(200, response.getStatusCode().value());
        jamcrest.validateJson(response.getBody(), "favorites/group/get.resp.js");
    }

    @Test
    void updateGroup_returns200WithUpdatedName() {
        String id = createGroup("My Lotto Group");

        String updateBody = jamcrest.loadJson("favorites/group/update.req.js");
        ResponseEntity<String> response = restTemplate.exchange(
                "/v1/draw-games/favorite-wagers/groups/" + id,
                HttpMethod.PUT,
                new HttpEntity<>(updateBody, asPlayer(PLAYER)),
                String.class);

        assertEquals(200, response.getStatusCode().value());
        jamcrest.validateJson(response.getBody(), "favorites/group/get.resp.js");
        assertTrue(response.getBody().contains("Updated Group Name"), "group name should be updated");
    }

    @Test
    void deleteGroup_returns204_thenGet_returns404() {
        String id = createGroup("My Lotto Group");

        ResponseEntity<String> deleteResp = restTemplate.exchange(
                "/v1/draw-games/favorite-wagers/groups/" + id,
                HttpMethod.DELETE,
                new HttpEntity<>(asPlayer(PLAYER)),
                String.class);
        assertEquals(204, deleteResp.getStatusCode().value());

        ResponseEntity<String> getAfterDelete = restTemplate.exchange(
                "/v1/draw-games/favorite-wagers/groups/" + id,
                HttpMethod.GET,
                new HttpEntity<>(asPlayer(PLAYER)),
                String.class);
        assertEquals(404, getAfterDelete.getStatusCode().value());
    }

    @Test
    void listGroups_returns200PageWithFavoriteWagerGroups() {
        createGroup("Group A");
        createGroup("Group B");

        ResponseEntity<String> response = restTemplate.exchange(
                "/v1/draw-games/favorite-wagers/groups",
                HttpMethod.GET,
                new HttpEntity<>(asPlayer(PLAYER)),
                String.class);

        assertEquals(200, response.getStatusCode().value());
        jamcrest.validateJson(response.getBody(), "favorites/group/list.resp.js");
        assertTrue(response.getBody().contains("favoriteWagerGroups"), "legacy list field name");
    }
}
