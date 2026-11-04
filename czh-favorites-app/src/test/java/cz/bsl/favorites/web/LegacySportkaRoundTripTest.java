package cz.bsl.favorites.web;

// Grep anchor: favorites

import cz.bsl.czh.favorites.test.AbstractRestTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Acceptance test for the DTO alignment plan: the legacy front-end favorite body
 * (byte-identical copy of the gis rest-test fixture {@code sportka.json}) must
 * post (200), read back identically, update its name and delete cleanly.
 *
 * <p>Mirrors the integrated smoke flow described in
 * {@code czh-gis-integrated/.requirements/integrations/favorites-integration.md} §4:
 * POST legacy body -&gt; 200 with string {@code id} -&gt; GET equals input -&gt;
 * PUT with changed name -&gt; DELETE 204 -&gt; GET 404.
 */
class LegacySportkaRoundTripTest extends AbstractRestTest {

    private static final String PLAYER = "legacy-sportka-roundtrip-player";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private String legacyBody() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/favorites/legacy/sportka.json")) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void legacyBody_postsReadsUpdatesAndDeletes() throws Exception {
        // --- POST: 200 with a string id ---
        ResponseEntity<String> created = restTemplate.exchange(
                "/v1/draw-games/favorite-wagers/wagers",
                HttpMethod.POST,
                new HttpEntity<>(legacyBody(), asPlayer(PLAYER)),
                String.class);
        assertEquals(200, created.getStatusCode().value(),
                "legacy contract: POST returns 200; got body: " + created.getBody());

        JsonNode createJson = MAPPER.readTree(created.getBody());
        String id = createJson.path("id").asString();
        assertTrue(!id.isBlank() && id.chars().allMatch(Character::isDigit),
                "id must be the numeric key as a JSON string, got: " + id);

        // --- GET: the wager round-trips identically ---
        ResponseEntity<String> fetched = restTemplate.exchange(
                "/v1/draw-games/favorite-wagers/wagers/" + id,
                HttpMethod.GET,
                new HttpEntity<>(asPlayer(PLAYER)),
                String.class);
        assertEquals(200, fetched.getStatusCode().value());

        JsonNode request = MAPPER.readTree(legacyBody());
        JsonNode response = MAPPER.readTree(fetched.getBody());
        assertEquals("Sportka Favorite", response.path("name").asString());
        assertEquals("Sportka Group", response.path("group").path("name").asString());
        assertEquals("1", response.path("group").path("id").asString(),
                "group.id is the auto-assigned slot number as a string");
        assertJsonEquals(request.path("wager"), response.path("wager"));

        // --- PUT: rename via the full legacy body ---
        String updateBody = legacyBody().replace("Sportka Favorite", "Sportka Favorite Updated");
        ResponseEntity<String> updated = restTemplate.exchange(
                "/v1/draw-games/favorite-wagers/wagers/" + id,
                HttpMethod.PUT,
                new HttpEntity<>(updateBody, asPlayer(PLAYER)),
                String.class);
        assertEquals(200, updated.getStatusCode().value());
        assertEquals("Sportka Favorite Updated",
                MAPPER.readTree(updated.getBody()).path("name").asString());

        // --- DELETE: 204, then GET: 404 ---
        ResponseEntity<String> deleted = restTemplate.exchange(
                "/v1/draw-games/favorite-wagers/wagers/" + id,
                HttpMethod.DELETE,
                new HttpEntity<>(asPlayer(PLAYER)),
                String.class);
        assertEquals(204, deleted.getStatusCode().value());

        ResponseEntity<String> afterDelete = restTemplate.exchange(
                "/v1/draw-games/favorite-wagers/wagers/" + id,
                HttpMethod.GET,
                new HttpEntity<>(asPlayer(PLAYER)),
                String.class);
        assertEquals(404, afterDelete.getStatusCode().value());
    }

    /**
     * JSON-tree equality after numeric canonicalization (serialize + re-parse) so that
     * {@code 9000} parsed as int equals {@code 9000} serialized from a long. Key order
     * does not matter; null-vs-absent does not matter for this comparison because the
     * stored wager is sparse (NON_NULL) like the request.
     */
    private static void assertJsonEquals(JsonNode expected, JsonNode actual) {
        JsonNode exp = canon(expected);
        JsonNode act = canon(actual);
        assertTrue(exp.equals(act),
                "wager round-trip mismatch\nexpected: " + exp + "\nactual:   " + act);
    }

    private static JsonNode canon(JsonNode node) {
        return MAPPER.readTree(MAPPER.writeValueAsString(node));
    }
}
