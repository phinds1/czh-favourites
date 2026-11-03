package cz.bsl.czh.favorites.service;

// Grep anchor: favorites

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import cz.bsl.czh.favorites.api.FavoriteGroupDto;
import cz.bsl.czh.favorites.api.FavoriteGroupPageDto;
import cz.bsl.czh.favorites.api.FavoriteWagerDto;
import cz.bsl.czh.favorites.api.FavoriteWagerPageDto;
import cz.bsl.czh.favorites.api.WagerDto;
import cz.bsl.czh.favorites.dao.FavoriteGroupRecord;
import cz.bsl.czh.favorites.dao.FavoriteWagerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FavoritesConverterTest {

    private FavoritesConverter converter;

    @BeforeEach
    void setUp() {
        ObjectMapper mapper = JsonMapper.builder().build();
        converter = new FavoritesConverter(mapper);
    }

    @Test
    void toRecord_wager_setsAllFields() {
        WagerDto wager = new WagerDto("LOTTO", 100L, 100L, 1, "SN1",
                List.of(List.of(1, 2, 3)));
        FavoriteWagerDto dto = new FavoriteWagerDto();
        dto.setPlayerId("should-be-ignored");
        dto.setGroupNumber("1");
        dto.setWagerName("My Lotto");
        dto.setFlags(99);
        dto.setWager(wager);

        FavoriteWagerRecord record = converter.toRecord(dto, "player-42");

        assertEquals("player-42", record.getPlayerId(), "playerId must come from header param, not DTO");
        assertEquals("1", record.getGroupNumber());
        assertEquals("LOTTO", record.getGameName());
        assertEquals("My Lotto", record.getWagerName());
        assertEquals(0, record.getFlags(), "flags must always be 0 on create");
        assertNotNull(record.getWagerJson());
        assertTrue(record.getWagerJson().contains("LOTTO"), "wagerJson must contain gameName");
    }

    @Test
    void toDto_wager_roundTrip() {
        FavoriteWagerRecord record = new FavoriteWagerRecord();
        record.setFavWagerId(7L);
        record.setPlayerId("player-1");
        record.setGroupNumber("2");
        record.setGameName("KENO");
        record.setWagerName("My Keno");
        record.setFlags(0);
        record.setCreatedAt(Instant.parse("2024-01-01T10:00:00Z"));
        record.setWagerJson("{\"gameName\":\"KENO\",\"stake\":50,\"price\":50,\"duration\":1,\"serialNumber\":\"SN2\",\"boards\":[[10,20,30]]}");

        FavoriteWagerDto dto = converter.toDto(record);

        assertEquals(7L, dto.getId());
        assertEquals("player-1", dto.getPlayerId());
        assertEquals("2", dto.getGroupNumber());
        assertNotNull(dto.getWager());
        assertEquals(List.of(List.of(10, 20, 30)), dto.getWager().getBoards(), "boards must round-trip correctly");
    }

    @Test
    void toRecord_wager_flagsAlwaysZero() {
        WagerDto wager = new WagerDto("GAME", 10L, 10L, 1, null, List.of());
        FavoriteWagerDto dto = new FavoriteWagerDto();
        dto.setFlags(99);
        dto.setGroupNumber("1");
        dto.setWager(wager);

        FavoriteWagerRecord record = converter.toRecord(dto, "p1");

        assertEquals(0, record.getFlags());
    }

    @Test
    void toRecord_group_setsAllFields() {
        FavoriteGroupDto dto = new FavoriteGroupDto();
        dto.setGroupNumber("3");
        dto.setGroupName("Lucky Group");
        dto.setFlags(5);

        FavoriteGroupRecord record = converter.toRecord(dto, "player-99");

        assertEquals("player-99", record.getPlayerId());
        assertEquals("3", record.getGroupNumber());
        assertEquals("Lucky Group", record.getGroupName());
        assertEquals(0, record.getFlags(), "flags must always be 0 on create");
    }

    @Test
    void toDto_group_roundTrip() {
        FavoriteGroupRecord record = new FavoriteGroupRecord();
        record.setFavGroupId(11L);
        record.setPlayerId("player-5");
        record.setGroupNumber("4");
        record.setGroupName("Friday Group");
        record.setFlags(0);
        record.setCreatedAt(Instant.parse("2024-02-01T00:00:00Z"));
        record.setUpdatedAt(Instant.parse("2024-02-02T00:00:00Z"));

        FavoriteGroupDto dto = converter.toDto(record);

        assertEquals(11L, dto.getId());
        assertEquals("player-5", dto.getPlayerId());
        assertEquals("4", dto.getGroupNumber());
        assertEquals("Friday Group", dto.getGroupName());
        assertNotNull(dto.getCreatedAt());
        assertNotNull(dto.getUpdatedAt());
    }

    @Test
    void toWagerPage_setsItemsAndCount() {
        FavoriteWagerRecord r = new FavoriteWagerRecord();
        r.setFavWagerId(1L);
        r.setPlayerId("p");
        r.setGroupNumber("1");
        r.setGameName("G");
        r.setWagerName("");
        r.setWagerJson("{\"gameName\":\"G\",\"stake\":1,\"price\":1,\"duration\":1,\"boards\":[]}");

        FavoriteWagerPageDto page = converter.toWagerPage(List.of(r));

        assertEquals(1, page.getTotalCount());
        assertEquals(1, page.getItems().size());
    }

    @Test
    void toGroupPage_setsItemsAndCount() {
        FavoriteGroupRecord r = new FavoriteGroupRecord();
        r.setFavGroupId(1L);
        r.setPlayerId("p");
        r.setGroupNumber("1");
        r.setGroupName("G");

        FavoriteGroupPageDto page = converter.toGroupPage(List.of(r));

        assertEquals(1, page.getTotalCount());
        assertEquals(1, page.getItems().size());
    }
}
