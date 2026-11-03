package cz.bsl.czh.favorites.api;

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

    @Test
    void fullRoundTrip() throws Exception {
        WagerDto wager = new WagerDto("LOTTO", 100L, 200L, 5, "SN-999",
                Arrays.asList(
                        Arrays.asList(3, 7, 12, 19, 34, 42),
                        Arrays.asList(1, 5, 9, 15, 28, 39)
                ));

        FavoriteWagerDto original = new FavoriteWagerDto();
        original.setId(42L);
        original.setPlayerId("player-123");
        original.setGroupNumber("2");
        original.setGameName("LOTTO");
        original.setWagerName("Lucky numbers");
        original.setFlags(0);
        original.setCreatedAt(Instant.parse("2025-01-15T10:30:00Z"));
        original.setWager(wager);

        String json = mapper.writeValueAsString(original);
        FavoriteWagerDto result = mapper.readValue(json, FavoriteWagerDto.class);

        assertEquals(original.getId(), result.getId());
        assertEquals(original.getPlayerId(), result.getPlayerId());
        assertEquals(original.getGroupNumber(), result.getGroupNumber());
        assertEquals(original.getGameName(), result.getGameName());
        assertEquals(original.getWagerName(), result.getWagerName());
        assertEquals(original.getFlags(), result.getFlags());
        assertEquals(original.getCreatedAt(), result.getCreatedAt());
        assertNotNull(result.getWager());
        assertEquals(original.getWager().getGameName(), result.getWager().getGameName());
        assertEquals(original.getWager().getBoards(), result.getWager().getBoards());
    }

    @Test
    void nullableFieldsRoundTrip() throws Exception {
        FavoriteWagerDto original = new FavoriteWagerDto();
        original.setPlayerId("player-456");
        original.setGroupNumber("1");
        original.setGameName("KENO");

        String json = mapper.writeValueAsString(original);
        FavoriteWagerDto result = mapper.readValue(json, FavoriteWagerDto.class);

        assertNull(result.getId());
        assertNull(result.getCreatedAt());
        assertNull(result.getWager());
        assertEquals("player-456", result.getPlayerId());
    }

    @Test
    void unknownFieldsIgnored() throws Exception {
        String json = "{\"id\":1,\"playerId\":\"p1\",\"groupNumber\":\"1\",\"gameName\":\"LOTTO\"," +
                "\"wagerName\":\"\",\"flags\":0,\"createdAt\":null,\"wager\":null," +
                "\"legacyField\":\"should-be-ignored\"}";

        FavoriteWagerDto result = mapper.readValue(json, FavoriteWagerDto.class);

        assertEquals(1L, result.getId());
        assertEquals("LOTTO", result.getGameName());
    }

    @Test
    void wagerSubObjectRoundTrip() throws Exception {
        WagerDto wager = new WagerDto("EUROJACKPOT", 250L, 500L, 1, null,
                Arrays.asList(
                        Arrays.asList(5, 10, 15, 20, 25),
                        Arrays.asList(3, 6, 9, 12, 15)
                ));

        FavoriteWagerDto original = new FavoriteWagerDto();
        original.setPlayerId("p1");
        original.setGroupNumber("1");
        original.setGameName("EUROJACKPOT");
        original.setWager(wager);

        String json = mapper.writeValueAsString(original);
        FavoriteWagerDto result = mapper.readValue(json, FavoriteWagerDto.class);

        assertNotNull(result.getWager());
        assertEquals(2, result.getWager().getBoards().size());
        assertEquals(Arrays.asList(5, 10, 15, 20, 25), result.getWager().getBoards().get(0));
        assertEquals(Arrays.asList(3, 6, 9, 12, 15), result.getWager().getBoards().get(1));
        assertNull(result.getWager().getSerialNumber());
    }
}
