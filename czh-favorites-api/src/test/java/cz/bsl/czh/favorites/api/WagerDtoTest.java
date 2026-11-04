package cz.bsl.czh.favorites.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Jackson round-trip tests for {@link WagerDto}.
 *
 * <p>The golden legacy tests pin the front-end (gisorch {@code WagerDTO}) wire
 * contract: the wager sub-object of a legacy favorite body must deserialize and
 * re-serialize unchanged, including {@code addonWagers},
 * {@code boards[].primarySelections}, {@code drawNames} and {@code type}. The
 * fixture is a byte-identical copy of the legacy front-end body
 * ({@code src/test/resources/legacy/sportka.json}).
 *
 * <p>Grep anchor: favorites
 */
class WagerDtoTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // --- golden legacy front-end contract ---

    @Test
    void legacyWager_roundTripsUnchanged() throws Exception {
        JsonNode input = FixtureJson.readResource("/legacy/sportka.json", MAPPER).get("wager");

        WagerDto dto = MAPPER.treeToValue(input, WagerDto.class);
        JsonNode output = MAPPER.valueToTree(dto);

        FixtureJson.assertJsonEquals(MAPPER, input, output);
    }

    @Test
    void legacyWager_keepsTypeDrawNamesBoardsAndAddonWagers() throws Exception {
        JsonNode input = FixtureJson.readResource("/legacy/sportka.json", MAPPER).get("wager");

        WagerDto dto = MAPPER.treeToValue(input, WagerDto.class);
        JsonNode output = MAPPER.valueToTree(dto);

        assertEquals("NORMAL", output.path("type").asText());
        assertEquals(3, output.path("duration").asInt());
        assertEquals("Sportka", output.path("gameName").asText());
        assertEquals(9000L, output.path("price").asLong());
        assertEquals(Arrays.asList("WED", "FRI", "SUN"), toStringList(output.path("drawNames")));

        JsonNode board = output.path("boards").get(0);
        assertEquals("default", board.path("systemBetType").asText());
        assertEquals("NORMAL", board.path("betType").asText());
        assertFalse(board.path("quickpick").asBoolean());
        assertEquals(Arrays.asList("1", "2", "3", "4", "5", "6"),
                toStringList(board.path("primarySelections")));

        JsonNode addon = output.path("addonWagers").get(0);
        assertEquals("Sance", addon.path("gameName").asText());
        assertFalse(addon.path("addonPlayed").asBoolean());
        JsonNode addonBoard = addon.path("boards").get(0);
        assertFalse(addonBoard.path("quickpick").asBoolean());
        assertEquals(Arrays.asList("1", "2", "3", "4", "5", "6"),
                toStringList(addonBoard.path("primarySelections")));
    }

    @Test
    void serverAssignedFields_areAcceptedOnInput() throws Exception {
        JsonNode input = MAPPER.readTree("{\"gameName\":\"Sportka\",\"id\":\"ESTE-1\",\"status\":\"WON\"," +
                "\"requestId\":\"req-1\",\"digest\":\"d1\",\"transactionTime\":1730000000000," +
                "\"signature\":\"sig\",\"revision\":\"r1\",\"drawStartDate\":1730000100000," +
                "\"drawEndDate\":1730000200000," +
                "\"prizes\":[{\"prizeTierId\":\"p1\",\"amount\":100,\"status\":\"PAID\",\"type\":\"FIXED\"}]}");

        WagerDto dto = MAPPER.treeToValue(input, WagerDto.class);
        JsonNode output = MAPPER.valueToTree(dto);

        // the DTO holds server-assigned fields; the service layer strips them before storing
        assertEquals("WON", output.path("status").asText());
        assertEquals("req-1", output.path("requestId").asText());
        assertEquals("d1", output.path("digest").asText());
        assertEquals(1730000000000L, output.path("transactionTime").asLong());
        assertEquals("sig", output.path("signature").asText());
        assertEquals(1, output.path("prizes").size());
        assertEquals("p1", output.path("prizes").get(0).path("prizeTierId").asText());
    }

    // --- structure round trips (new WagerDto shape) ---

    @Test
    void roundTrip_withBoards() throws Exception {
        WagerDto original = sportkaWager();

        String json = MAPPER.writeValueAsString(original);
        WagerDto result = MAPPER.readValue(json, WagerDto.class);

        assertEquals("Sportka", result.getGameName());
        assertEquals(Long.valueOf(9000L), result.getPrice());
        assertEquals(Integer.valueOf(3), result.getDuration());
        assertEquals(WagerType.NORMAL, result.getType());
        assertEquals(1, result.getBoards().size());
        assertEquals("default", result.getBoards().get(0).getSystemBetType());
        assertEquals(Arrays.asList("1", "2", "3", "4", "5", "6"),
                result.getBoards().get(0).getPrimarySelections());
        assertEquals(1, result.getAddonWagers().size());
        assertEquals("Sance", result.getAddonWagers().get(0).getGameName());
    }

    @Test
    void roundTrip_emptyBoards() throws Exception {
        WagerDto original = new WagerDto();
        original.setGameName("KENO");
        original.setBoards(Collections.emptyList());

        String json = MAPPER.writeValueAsString(original);
        WagerDto result = MAPPER.readValue(json, WagerDto.class);

        assertNotNull(result.getBoards());
        assertTrue(result.getBoards().isEmpty());
    }

    @Test
    void roundTrip_nullFields_omittedOnWire() throws Exception {
        WagerDto original = new WagerDto();
        original.setGameName("LOTTO");
        original.setSerialNumber(null);
        original.setBoards(Collections.singletonList(board("1", "2", "3")));

        String json = MAPPER.writeValueAsString(original);
        JsonNode tree = MAPPER.readTree(json);
        WagerDto result = MAPPER.readValue(json, WagerDto.class);

        assertFalse(tree.has("serialNumber"), "null fields must be omitted (sparse wire shape)");
        assertFalse(tree.has("stake"));
        assertNull(result.getSerialNumber());
        assertEquals(Arrays.asList("1", "2", "3"), result.getBoards().get(0).getSelections());
    }

    @Test
    void unknownFieldsIgnored() throws Exception {
        String json = "{\"gameName\":\"LOTTO\",\"boards\":[{\"selections\":[\"1\",\"2\",\"3\"]," +
                "\"unknownField\":\"ignored\"}],\"unknownTopLevel\":\"ignored\"}";

        WagerDto result = MAPPER.readValue(json, WagerDto.class);

        assertEquals("LOTTO", result.getGameName());
        assertEquals(1, result.getBoards().size());
        assertEquals(Arrays.asList("1", "2", "3"), result.getBoards().get(0).getSelections());
    }

    private static WagerDto sportkaWager() throws Exception {
        return MAPPER.treeToValue(
                FixtureJson.readResource("/legacy/sportka.json", MAPPER).get("wager"), WagerDto.class);
    }

    private static BoardDto board(String... selections) {
        BoardDto board = new BoardDto();
        board.setSelections(Arrays.asList(selections));
        return board;
    }

    private static List<String> toStringList(JsonNode array) {
        List<String> values = new ArrayList<>();
        array.forEach(node -> values.add(node.asText()));
        return values;
    }
}
