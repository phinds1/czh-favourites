package cz.bsl.czh.favorites.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Jackson round-trip tests for {@link FavoriteGroupDto}.
 *
 * <p>The golden legacy tests pin the front-end group wire contract: a group body is
 * {@code {"name": "..."}} (the fixture is the byte-identical legacy
 * {@code group/sportkagroup.json}), and {@code id} is a string on the wire.
 *
 * <p>Grep anchor: favorites
 */
class FavoriteGroupDtoTest {

    private static ObjectMapper mapper;

    @BeforeAll
    static void setUp() {
        mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    // --- golden legacy front-end contract ---

    @Test
    void legacyGroupBody_roundTrips() throws Exception {
        JsonNode input = FixtureJson.readResource("/legacy/sportkagroup.json", mapper);

        FavoriteGroupDto dto = mapper.treeToValue(input, FavoriteGroupDto.class);
        JsonNode output = mapper.valueToTree(dto);

        assertEquals(input.get("name"), output.get("name"));
        assertEquals("V Group", dto.getName());
    }

    @Test
    void wire_idIsString() throws Exception {
        JsonNode input = mapper.readTree("{\"id\":\"3\",\"name\":\"My Lucky Group\"}");

        FavoriteGroupDto dto = mapper.treeToValue(input, FavoriteGroupDto.class);
        JsonNode output = mapper.valueToTree(dto);

        assertTrue(output.path("id").isTextual(), "id must be a JSON string on the wire");
        assertEquals("3", output.path("id").asText());
        assertEquals("3", dto.getId());
    }

    @Test
    void wire_usesNameNotGroupName() throws Exception {
        FavoriteGroupDto original = new FavoriteGroupDto("3", "My Lucky Group");

        String json = mapper.writeValueAsString(original);
        JsonNode tree = mapper.readTree(json);

        assertTrue(tree.has("name"), "legacy front end reads the group display name from 'name'");
        assertFalse(tree.has("groupName"), "'groupName' is not a legacy wire field");
        assertFalse(tree.has("groupNumber"), "'groupNumber' is not a legacy wire field");
    }

    // --- structure round trips ---

    @Test
    void fullRoundTrip() throws Exception {
        FavoriteGroupDto original = new FavoriteGroupDto("7", "My Lucky Group");
        original.setPlayerId("player-789");
        original.setFlags(0);
        original.setCreatedAt(Instant.parse("2025-03-01T08:00:00Z"));
        original.setUpdatedAt(Instant.parse("2025-03-10T12:00:00Z"));

        String json = mapper.writeValueAsString(original);
        FavoriteGroupDto result = mapper.readValue(json, FavoriteGroupDto.class);

        assertEquals(original.getId(), result.getId());
        assertEquals(original.getPlayerId(), result.getPlayerId());
        assertEquals(original.getName(), result.getName());
        assertEquals(original.getFlags(), result.getFlags());
        assertEquals(original.getCreatedAt(), result.getCreatedAt());
        assertEquals(original.getUpdatedAt(), result.getUpdatedAt());
    }

    @Test
    void nullFields_omittedOnWire() throws Exception {
        FavoriteGroupDto original = new FavoriteGroupDto();
        original.setPlayerId("player-001");

        String json = mapper.writeValueAsString(original);
        JsonNode tree = mapper.readTree(json);
        FavoriteGroupDto result = mapper.readValue(json, FavoriteGroupDto.class);

        assertNull(result.getId());
        assertNull(result.getCreatedAt());
        assertNull(result.getUpdatedAt());
        assertEquals("player-001", result.getPlayerId());
        assertFalse(tree.has("id"), "null fields must be omitted (sparse wire shape)");
        assertFalse(tree.has("createdAt"));
        assertTrue(tree.has("name"), "name defaults to empty string, not null");
    }

    @Test
    void unknownFieldsIgnored() throws Exception {
        String json = "{\"id\":\"5\",\"playerId\":\"p1\",\"name\":\"Test\"," +
                "\"flags\":0,\"createdAt\":null,\"updatedAt\":null,\"legacyProp\":\"ignored\"}";

        FavoriteGroupDto result = mapper.readValue(json, FavoriteGroupDto.class);

        assertEquals("5", result.getId());
        assertEquals("Test", result.getName());
    }
}
