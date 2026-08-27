package cz.bsl.czh.favourites.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Jackson round-trip tests for {@link FavouriteWagerPageDto} and {@link FavouriteGroupPageDto}.
 *
 * <p>Grep anchor: favourites
 */
class FavouritePageDtoTest {

    private static ObjectMapper mapper;

    @BeforeAll
    static void setUp() {
        mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    // --- FavouriteWagerPageDto ---

    @Test
    void wagerPage_withItemsRoundTrip() throws Exception {
        FavouriteWagerDto w1 = new FavouriteWagerDto();
        w1.setId(1L);
        w1.setPlayerId("player-1");
        w1.setGroupNumber("1");
        w1.setGameName("LOTTO");

        FavouriteWagerDto w2 = new FavouriteWagerDto();
        w2.setId(2L);
        w2.setPlayerId("player-1");
        w2.setGroupNumber("1");
        w2.setGameName("KENO");

        FavouriteWagerPageDto original = new FavouriteWagerPageDto(Arrays.asList(w1, w2), 2);

        String json = mapper.writeValueAsString(original);
        FavouriteWagerPageDto result = mapper.readValue(json, FavouriteWagerPageDto.class);

        assertEquals(2, result.getTotalCount());
        assertEquals(2, result.getItems().size());
        assertEquals("LOTTO", result.getItems().get(0).getGameName());
        assertEquals("KENO", result.getItems().get(1).getGameName());
    }

    @Test
    void wagerPage_emptyRoundTrip() throws Exception {
        FavouriteWagerPageDto original = new FavouriteWagerPageDto(Collections.emptyList(), 0);

        String json = mapper.writeValueAsString(original);
        FavouriteWagerPageDto result = mapper.readValue(json, FavouriteWagerPageDto.class);

        assertEquals(0, result.getTotalCount());
        assertNotNull(result.getItems());
        assertTrue(result.getItems().isEmpty());
    }

    // --- FavouriteGroupPageDto ---

    @Test
    void groupPage_withItemsRoundTrip() throws Exception {
        FavouriteGroupDto g1 = new FavouriteGroupDto();
        g1.setId(1L);
        g1.setPlayerId("player-1");
        g1.setGroupNumber("1");
        g1.setGroupName("Alpha");

        FavouriteGroupDto g2 = new FavouriteGroupDto();
        g2.setId(2L);
        g2.setPlayerId("player-1");
        g2.setGroupNumber("2");
        g2.setGroupName("Beta");

        FavouriteGroupPageDto original = new FavouriteGroupPageDto(Arrays.asList(g1, g2), 2);

        String json = mapper.writeValueAsString(original);
        FavouriteGroupPageDto result = mapper.readValue(json, FavouriteGroupPageDto.class);

        assertEquals(2, result.getTotalCount());
        assertEquals(2, result.getItems().size());
        assertEquals("Alpha", result.getItems().get(0).getGroupName());
        assertEquals("Beta", result.getItems().get(1).getGroupName());
    }

    @Test
    void groupPage_emptyRoundTrip() throws Exception {
        FavouriteGroupPageDto original = new FavouriteGroupPageDto(Collections.emptyList(), 0);

        String json = mapper.writeValueAsString(original);
        FavouriteGroupPageDto result = mapper.readValue(json, FavouriteGroupPageDto.class);

        assertEquals(0, result.getTotalCount());
        assertNotNull(result.getItems());
        assertTrue(result.getItems().isEmpty());
    }
}
