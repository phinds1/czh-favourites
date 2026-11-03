package cz.bsl.czh.favorites.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Jackson round-trip tests for {@link FavoriteGroupDto}.
 *
 * <p>Grep anchor: favorites
 */
class FavoriteGroupDtoTest {

    private static ObjectMapper mapper;

    @BeforeAll
    static void setUp() {
        mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Test
    void fullRoundTrip() throws Exception {
        FavoriteGroupDto original = new FavoriteGroupDto();
        original.setId(7L);
        original.setPlayerId("player-789");
        original.setGroupNumber("3");
        original.setGroupName("My Lucky Group");
        original.setFlags(0);
        original.setCreatedAt(Instant.parse("2025-03-01T08:00:00Z"));
        original.setUpdatedAt(Instant.parse("2025-03-10T12:00:00Z"));

        String json = mapper.writeValueAsString(original);
        FavoriteGroupDto result = mapper.readValue(json, FavoriteGroupDto.class);

        assertEquals(original.getId(), result.getId());
        assertEquals(original.getPlayerId(), result.getPlayerId());
        assertEquals(original.getGroupNumber(), result.getGroupNumber());
        assertEquals(original.getGroupName(), result.getGroupName());
        assertEquals(original.getFlags(), result.getFlags());
        assertEquals(original.getCreatedAt(), result.getCreatedAt());
        assertEquals(original.getUpdatedAt(), result.getUpdatedAt());
    }

    @Test
    void nullTimestampsRoundTrip() throws Exception {
        FavoriteGroupDto original = new FavoriteGroupDto();
        original.setPlayerId("player-001");
        original.setGroupNumber("1");

        String json = mapper.writeValueAsString(original);
        FavoriteGroupDto result = mapper.readValue(json, FavoriteGroupDto.class);

        assertNull(result.getId());
        assertNull(result.getCreatedAt());
        assertNull(result.getUpdatedAt());
        assertEquals("player-001", result.getPlayerId());
    }

    @Test
    void unknownFieldsIgnored() throws Exception {
        String json = "{\"id\":5,\"playerId\":\"p1\",\"groupNumber\":\"2\",\"groupName\":\"Test\"," +
                "\"flags\":0,\"createdAt\":null,\"updatedAt\":null,\"legacyProp\":\"ignored\"}";

        FavoriteGroupDto result = mapper.readValue(json, FavoriteGroupDto.class);

        assertEquals(5L, result.getId());
        assertEquals("Test", result.getGroupName());
    }
}
