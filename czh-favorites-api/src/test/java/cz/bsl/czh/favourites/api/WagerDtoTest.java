package cz.bsl.czh.favourites.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Jackson round-trip tests for {@link WagerDto}.
 *
 * <p>Grep anchor: favourites
 */
class WagerDtoTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void roundTrip_withBoards() throws Exception {
        List<List<Integer>> boards = Arrays.asList(
                Arrays.asList(1, 2, 3, 4, 5, 6),
                Arrays.asList(7, 8, 9, 10, 11, 12),
                Arrays.asList(13, 14, 15, 16, 17, 18)
        );
        WagerDto original = new WagerDto("LOTTO", 100L, 200L, 3, "SN-001", boards);

        String json = MAPPER.writeValueAsString(original);
        WagerDto result = MAPPER.readValue(json, WagerDto.class);

        assertEquals(original.getGameName(), result.getGameName());
        assertEquals(original.getStake(), result.getStake());
        assertEquals(original.getPrice(), result.getPrice());
        assertEquals(original.getDuration(), result.getDuration());
        assertEquals(original.getSerialNumber(), result.getSerialNumber());
        assertEquals(original.getBoards(), result.getBoards());
    }

    @Test
    void roundTrip_emptyBoards() throws Exception {
        WagerDto original = new WagerDto("KENO", 50L, 50L, 1, null, Collections.emptyList());

        String json = MAPPER.writeValueAsString(original);
        WagerDto result = MAPPER.readValue(json, WagerDto.class);

        assertNotNull(result.getBoards());
        assertTrue(result.getBoards().isEmpty());
    }

    @Test
    void roundTrip_nullSerialNumber() throws Exception {
        WagerDto original = new WagerDto("LOTTO", 100L, 100L, 2, null,
                Collections.singletonList(Arrays.asList(1, 2, 3)));

        String json = MAPPER.writeValueAsString(original);
        WagerDto result = MAPPER.readValue(json, WagerDto.class);

        assertNull(result.getSerialNumber());
        assertEquals(original.getBoards(), result.getBoards());
    }

    @Test
    void unknownFieldsIgnored() throws Exception {
        String json = "{\"gameName\":\"LOTTO\",\"stake\":100,\"price\":200,\"duration\":3," +
                "\"serialNumber\":null,\"boards\":[[1,2,3]],\"unknownField\":\"ignored\"}";

        WagerDto result = MAPPER.readValue(json, WagerDto.class);

        assertEquals("LOTTO", result.getGameName());
        assertEquals(1, result.getBoards().size());
        assertEquals(Arrays.asList(1, 2, 3), result.getBoards().get(0));
    }
}
