package cz.bsl.czh.favorites.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test helper for golden-JSON round-trip assertions against the legacy front-end
 * wire contract.
 *
 * <p>Golden fixtures are byte-identical copies of the legacy request bodies
 * ({@code src/test/resources/legacy/}). Trees are canonicalized (serialize +
 * re-parse) before comparison so numeric representation does not matter: a
 * fixture parses {@code 9000} as an int node while a DTO holding {@code Long}
 * serializes to a long node, and Jackson node equality is numeric-type-strict.
 *
 * <p>Grep anchor: favorites
 */
final class FixtureJson {

    private FixtureJson() {
    }

    /** Reads a classpath resource as a JSON tree; fails the test when missing. */
    static JsonNode readResource(String path, ObjectMapper mapper) {
        try (InputStream in = FixtureJson.class.getResourceAsStream(path)) {
            assertNotNull(in, "fixture " + path + " not on the test classpath");
            return mapper.readTree(in);
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read fixture " + path, e);
        }
    }

    /**
     * Asserts two JSON trees are equal after numeric canonicalization.
     * Object key order does not matter; numeric scalar types do not matter.
     */
    static void assertJsonEquals(ObjectMapper mapper, JsonNode expected, JsonNode actual) {
        JsonNode exp = canonicalize(mapper, expected);
        JsonNode act = canonicalize(mapper, actual);
        if (!exp.equals(act)) {
            fail("JSON round-trip mismatch:\nexpected: " + exp + "\nactual:   " + act);
        }
    }

    private static JsonNode canonicalize(ObjectMapper mapper, JsonNode node) {
        try {
            return mapper.readTree(mapper.writeValueAsString(node));
        } catch (IOException e) {
            throw new UncheckedIOException("cannot canonicalize JSON tree", e);
        }
    }
}
