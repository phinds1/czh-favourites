package cz.bsl.favorites.web;

// Grep anchor: favorites

import cz.bsl.czh.favorites.test.AbstractRestTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Validates error-handling contracts for {@link FavoritesController}.
 *
 * <ul>
 *   <li>Missing {@code X-Player-Id} header → 400</li>
 *   <li>Invalid (non-numeric) wager id → 400</li>
 *   <li>Non-existent wager id → 404</li>
 *   <li>Invalid JSON body → 400</li>
 *   <li>Missing required field → 400</li>
 * </ul>
 */
class FavoritesValidationTest extends AbstractRestTest {

    private static final String PLAYER = "validation-test-player";

    @Test
    void missingPlayerHeader_returns400() {
        // No X-Player-Id header
        ResponseEntity<String> response = restTemplate.exchange(
                "/v1/draw-games/favorite-wagers/wagers",
                HttpMethod.GET,
                new HttpEntity<>(new HttpHeaders()),
                String.class);

        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void getNonExistentWager_returns404() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/v1/draw-games/favorite-wagers/wagers/999999999",
                HttpMethod.GET,
                new HttpEntity<>(asPlayer(PLAYER)),
                String.class);

        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void getNonExistentGroup_returns404() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/v1/draw-games/favorite-wagers/groups/9",
                HttpMethod.GET,
                new HttpEntity<>(asPlayer(PLAYER)),
                String.class);

        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void createWagerWithMissingBoards_returns400() {
        String body = "{\"name\":\"x\",\"wager\":{\"gameName\":\"LOTTO\",\"stake\":1,\"price\":1,\"duration\":1}}";

        ResponseEntity<String> response = restTemplate.exchange(
                "/v1/draw-games/favorite-wagers/wagers",
                HttpMethod.POST,
                new HttpEntity<>(body, asPlayer(PLAYER)),
                String.class);

        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void createWagerWithBoardWithoutSelections_returns400() {
        String body = "{\"name\":\"x\",\"wager\":{\"gameName\":\"LOTTO\",\"stake\":1,\"price\":1," +
                "\"duration\":1,\"boards\":[{\"quickpick\":false}]}}";

        ResponseEntity<String> response = restTemplate.exchange(
                "/v1/draw-games/favorite-wagers/wagers",
                HttpMethod.POST,
                new HttpEntity<>(body, asPlayer(PLAYER)),
                String.class);

        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void createWagerLegacyBodyGroupByNameOnly_returns200() {
        // legacy front-end bodies reference the group by name only — the service
        // resolves or creates it
        String body = "{\"name\":\"Legacy Fave\",\"group\":{\"name\":\"Validation Group\"}," +
                "\"wager\":{\"gameName\":\"LOTTO\",\"stake\":1,\"price\":1,\"duration\":1," +
                "\"boards\":[{\"primarySelections\":[\"1\",\"2\",\"3\"]}]}}";

        ResponseEntity<String> response = restTemplate.exchange(
                "/v1/draw-games/favorite-wagers/wagers",
                HttpMethod.POST,
                new HttpEntity<>(body, asPlayer(PLAYER)),
                String.class);

        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void createGroupLegacyNameOnlyBody_returns200() {
        // the legacy group create body is {"name": "..."} — the slot number is auto-assigned
        String body = "{\"name\":\"My Group\"}";

        ResponseEntity<String> response = restTemplate.exchange(
                "/v1/draw-games/favorite-wagers/groups",
                HttpMethod.POST,
                new HttpEntity<>(body, asPlayer(PLAYER)),
                String.class);

        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void createGroupWithInvalidId_returns400() {
        // Group id must be numeric string (parsed to int by validator)
        String body = "{\"id\":\"not-a-number\",\"name\":\"My Group\"}";

        ResponseEntity<String> response = restTemplate.exchange(
                "/v1/draw-games/favorite-wagers/groups",
                HttpMethod.POST,
                new HttpEntity<>(body, asPlayer(PLAYER)),
                String.class);

        assertEquals(400, response.getStatusCode().value());
    }

    // Security finding 1: unbounded game-names CSV must be rejected at > 50 entries
    @Test
    void listWagersWithOversizedGameNamesCsv_returns400() {
        String manyGames = "GAME1,GAME2,GAME3,GAME4,GAME5,GAME6,GAME7,GAME8,GAME9,GAME10," +
                           "GAME11,GAME12,GAME13,GAME14,GAME15,GAME16,GAME17,GAME18,GAME19,GAME20," +
                           "GAME21,GAME22,GAME23,GAME24,GAME25,GAME26,GAME27,GAME28,GAME29,GAME30," +
                           "GAME31,GAME32,GAME33,GAME34,GAME35,GAME36,GAME37,GAME38,GAME39,GAME40," +
                           "GAME41,GAME42,GAME43,GAME44,GAME45,GAME46,GAME47,GAME48,GAME49,GAME50,GAME51";

        ResponseEntity<String> response = restTemplate.exchange(
                "/v1/draw-games/favorite-wagers/wagers?game-names=" + manyGames,
                HttpMethod.GET,
                new HttpEntity<>(asPlayer(PLAYER)),
                String.class);

        assertEquals(400, response.getStatusCode().value());
    }

    // Security finding 2: oversized X-Player-Id header must be rejected with 400
    @Test
    void oversizedPlayerHeader_returns400() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Player-Id", "x".repeat(65));
        headers.set("Content-Type", "application/json");

        ResponseEntity<String> response = restTemplate.exchange(
                "/v1/draw-games/favorite-wagers/wagers",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class);

        assertEquals(400, response.getStatusCode().value());
    }
}
