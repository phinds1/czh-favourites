package cz.bsl.czh.favorites.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Jackson round-trip tests for {@link FavoriteWagerDto}.
 *
 * <p>Grep anchor: favorites
 */
class FavoriteWagerDtoTest {

    private static ObjectMapper mapper;

    @BeforeAll
    static void setUp() {
        mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    // --- golden legacy front-end contract (gisorch FavoriteWagerDTO wire shape) ---

    @Test
    void legacyBody_roundTripsNameGroupAndWager() throws Exception {
        JsonNode input = FixtureJson.readResource("/legacy/sportka.json", mapper);

        FavoriteWagerDto dto = mapper.treeToValue(input, FavoriteWagerDto.class);
        JsonNode output = mapper.valueToTree(dto);

        assertEquals("Sportka Favorite", output.path("name").asText());
        assertEquals("Sportka Group", output.path("group").path("name").asText());
        FixtureJson.assertJsonEquals(mapper, input.get("wager"), output.path("wager"));
    }

    @Test
    void legacyBody_groupMayCarryIdAndName() throws Exception {
        JsonNode input = mapper.readTree("{\"name\":\"N\",\"group\":{\"id\":\"2\",\"name\":\"G\"}," +
                "\"wager\":{\"gameName\":\"Sportka\",\"boards\":[{\"primarySelections\":[\"1\"]}]}}");

        FavoriteWagerDto dto = mapper.treeToValue(input, FavoriteWagerDto.class);
        JsonNode output = mapper.valueToTree(dto);

        assertEquals("2", output.path("group").path("id").asText());
        assertEquals("G", output.path("group").path("name").asText());
    }

    @Test
    void wire_idIsString() throws Exception {
        JsonNode input = mapper.readTree(
                "{\"id\":\"12345\",\"name\":\"N\",\"wager\":{\"gameName\":\"Sportka\"}}");

        FavoriteWagerDto dto = mapper.treeToValue(input, FavoriteWagerDto.class);
        JsonNode output = mapper.valueToTree(dto);

        assertTrue(output.path("id").isTextual(), "id must be a JSON string on the wire");
        assertEquals("12345", output.path("id").asText());
    }

    @Test
    void fullRoundTrip() throws Exception {
        WagerDto wager = new WagerDto();
        wager.setGameName("LOTTO");
        wager.setStake(100L);
        wager.setPrice(200L);
        wager.setDuration(5);
        wager.setSerialNumber("SN-999");
        wager.setBoards(Arrays.asList(board("3", "7", "12"), board("1", "5", "9")));

        FavoriteWagerDto original = new FavoriteWagerDto();
        original.setId("42");
        original.setPlayerId("player-123");
        original.setGroup(new FavoriteWagerGroupDto("2", "My Group"));
        original.setGameName("LOTTO");
        original.setName("Lucky numbers");
        original.setFlags(0);
        original.setCreatedAt(Instant.parse("2025-01-15T10:30:00Z"));
        original.setWager(wager);

        String json = mapper.writeValueAsString(original);
        FavoriteWagerDto result = mapper.readValue(json, FavoriteWagerDto.class);

        assertEquals(original.getId(), result.getId());
        assertEquals(original.getPlayerId(), result.getPlayerId());
        assertEquals(original.getGroup().getId(), result.getGroup().getId());
        assertEquals(original.getGroup().getName(), result.getGroup().getName());
        assertEquals(original.getGameName(), result.getGameName());
        assertEquals(original.getName(), result.getName());
        assertEquals(original.getFlags(), result.getFlags());
        assertEquals(original.getCreatedAt(), result.getCreatedAt());
        assertNotNull(result.getWager());
        assertEquals(original.getWager().getGameName(), result.getWager().getGameName());
        assertEquals(original.getWager().getBoards().size(), result.getWager().getBoards().size());
        assertEquals(original.getWager().getBoards().get(0).getSelections(),
                result.getWager().getBoards().get(0).getSelections());
    }

    @Test
    void nullableFieldsRoundTrip() throws Exception {
        FavoriteWagerDto original = new FavoriteWagerDto();
        original.setPlayerId("player-456");
        original.setGameName("KENO");

        String json = mapper.writeValueAsString(original);
        FavoriteWagerDto result = mapper.readValue(json, FavoriteWagerDto.class);

        assertNull(result.getId());
        assertNull(result.getGroup());
        assertNull(result.getCreatedAt());
        assertNull(result.getWager());
        assertEquals("player-456", result.getPlayerId());
    }

    @Test
    void unknownFieldsIgnored() throws Exception {
        String json = "{\"id\":\"1\",\"playerId\":\"p1\",\"gameName\":\"LOTTO\"," +
                "\"flags\":0,\"createdAt\":null,\"wager\":null," +
                "\"legacyField\":\"should-be-ignored\"}";

        FavoriteWagerDto result = mapper.readValue(json, FavoriteWagerDto.class);

        assertEquals("1", result.getId());
        assertEquals("LOTTO", result.getGameName());
    }

    @Test
    void wagerSubObjectRoundTrip() throws Exception {
        WagerDto wager = new WagerDto();
        wager.setGameName("EUROJACKPOT");
        wager.setBoards(Arrays.asList(board("5", "10", "15"), board("3", "6", "9")));

        FavoriteWagerDto original = new FavoriteWagerDto();
        original.setPlayerId("p1");
        original.setName("Euro Favorite");
        original.setGameName("EUROJACKPOT");
        original.setWager(wager);

        String json = mapper.writeValueAsString(original);
        FavoriteWagerDto result = mapper.readValue(json, FavoriteWagerDto.class);

        assertNotNull(result.getWager());
        assertEquals(2, result.getWager().getBoards().size());
        assertEquals(Arrays.asList("5", "10", "15"),
                result.getWager().getBoards().get(0).getSelections());
        assertEquals(Arrays.asList("3", "6", "9"),
                result.getWager().getBoards().get(1).getSelections());
        assertNull(result.getWager().getSerialNumber());
    }

    private static BoardDto board(String... selections) {
        BoardDto board = new BoardDto();
        board.setSelections(Arrays.asList(selections));
        return board;
    }
}
