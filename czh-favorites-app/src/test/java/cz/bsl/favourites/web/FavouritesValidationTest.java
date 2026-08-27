package cz.bsl.favourites.web;

// Grep anchor: favourites

import cz.bsl.czh.favourites.test.AbstractRestTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Validates error-handling contracts for {@link FavouritesController}.
 *
 * <ul>
 *   <li>Missing {@code X-Player-Id} header → 400</li>
 *   <li>Invalid (non-numeric) wager id → 400</li>
 *   <li>Non-existent wager id → 404</li>
 *   <li>Invalid JSON body → 400</li>
 *   <li>Missing required field → 400</li>
 * </ul>
 */
class FavouritesValidationTest extends AbstractRestTest {

    private static final String PLAYER = "validation-test-player";

    @Test
    void missingPlayerHeader_returns400() {
        // No X-Player-Id header
        ResponseEntity<String> response = restTemplate.exchange(
                "/favourites/wagers",
                HttpMethod.GET,
                new HttpEntity<>(new HttpHeaders()),
                String.class);

        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void getNonExistentWager_returns404() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/favourites/wagers/999999999",
                HttpMethod.GET,
                new HttpEntity<>(asPlayer(PLAYER)),
                String.class);

        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void getNonExistentGroup_returns404() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/favourites/groups/9",
                HttpMethod.GET,
                new HttpEntity<>(asPlayer(PLAYER)),
                String.class);

        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void createWagerWithMissingGroupNumber_returns400() {
        String body = "{\"wagerName\":\"x\",\"wager\":{\"gameName\":\"LOTTO\",\"stake\":1,\"price\":1,\"duration\":1,\"serialNumber\":\"S1\"}}";

        ResponseEntity<String> response = restTemplate.exchange(
                "/favourites/wagers",
                HttpMethod.POST,
                new HttpEntity<>(body, asPlayer(PLAYER)),
                String.class);

        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void createGroupWithMissingGroupNumber_returns400() {
        String body = "{\"groupName\":\"My Group\"}";

        ResponseEntity<String> response = restTemplate.exchange(
                "/favourites/groups",
                HttpMethod.POST,
                new HttpEntity<>(body, asPlayer(PLAYER)),
                String.class);

        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void createGroupWithInvalidGroupNumber_returns400() {
        // Group number must be numeric string (parsed to int by validator)
        String body = "{\"groupNumber\":\"not-a-number\",\"groupName\":\"My Group\"}";

        ResponseEntity<String> response = restTemplate.exchange(
                "/favourites/groups",
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
                "/favourites/wagers?game-names=" + manyGames,
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
                "/favourites/wagers",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class);

        assertEquals(400, response.getStatusCode().value());
    }
}
