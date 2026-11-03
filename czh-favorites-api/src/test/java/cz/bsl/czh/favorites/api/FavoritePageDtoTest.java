package cz.bsl.czh.favorites.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Jackson round-trip tests for {@link FavoriteWagerPageDto} and {@link FavoriteGroupPageDto}.
 *
 * <p>Grep anchor: favorites
 */
class FavoritePageDtoTest {

    private static ObjectMapper mapper;

    @BeforeAll
    static void setUp() {
        mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    // --- FavoriteWagerPageDto ---

    @Test
    void wagerPage_withItemsRoundTrip() throws Exception {
        FavoriteWagerDto w1 = new FavoriteWagerDto();
        w1.setId(1L);
        w1.setPlayerId("player-1");
        w1.setGroupNumber("1");
        w1.setGameName("LOTTO");

        FavoriteWagerDto w2 = new FavoriteWagerDto();
        w2.setId(2L);
        w2.setPlayerId("player-1");
        w2.setGroupNumber("1");
        w2.setGameName("KENO");

        FavoriteWagerPageDto original = new FavoriteWagerPageDto(Arrays.asList(w1, w2), 2);

        String json = mapper.writeValueAsString(original);
        FavoriteWagerPageDto result = mapper.readValue(json, FavoriteWagerPageDto.class);

        assertEquals(2, result.getTotalCount());
        assertEquals(2, result.getItems().size());
        assertEquals("LOTTO", result.getItems().get(0).getGameName());
        assertEquals("KENO", result.getItems().get(1).getGameName());
    }

    @Test
    void wagerPage_emptyRoundTrip() throws Exception {
        FavoriteWagerPageDto original = new FavoriteWagerPageDto(Collections.emptyList(), 0);

        String json = mapper.writeValueAsString(original);
        FavoriteWagerPageDto result = mapper.readValue(json, FavoriteWagerPageDto.class);

        assertEquals(0, result.getTotalCount());
        assertNotNull(result.getItems());
        assertTrue(result.getItems().isEmpty());
    }

    // --- FavoriteGroupPageDto ---

    @Test
    void groupPage_withItemsRoundTrip() throws Exception {
        FavoriteGroupDto g1 = new FavoriteGroupDto();
        g1.setId(1L);
        g1.setPlayerId("player-1");
        g1.setGroupNumber("1");
        g1.setGroupName("Alpha");

        FavoriteGroupDto g2 = new FavoriteGroupDto();
        g2.setId(2L);
        g2.setPlayerId("player-1");
        g2.setGroupNumber("2");
        g2.setGroupName("Beta");

        FavoriteGroupPageDto original = new FavoriteGroupPageDto(Arrays.asList(g1, g2), 2);

        String json = mapper.writeValueAsString(original);
        FavoriteGroupPageDto result = mapper.readValue(json, FavoriteGroupPageDto.class);

        assertEquals(2, result.getTotalCount());
        assertEquals(2, result.getItems().size());
        assertEquals("Alpha", result.getItems().get(0).getGroupName());
        assertEquals("Beta", result.getItems().get(1).getGroupName());
    }

    @Test
    void groupPage_emptyRoundTrip() throws Exception {
        FavoriteGroupPageDto original = new FavoriteGroupPageDto(Collections.emptyList(), 0);

        String json = mapper.writeValueAsString(original);
        FavoriteGroupPageDto result = mapper.readValue(json, FavoriteGroupPageDto.class);

        assertEquals(0, result.getTotalCount());
        assertNotNull(result.getItems());
        assertTrue(result.getItems().isEmpty());
    }
}
