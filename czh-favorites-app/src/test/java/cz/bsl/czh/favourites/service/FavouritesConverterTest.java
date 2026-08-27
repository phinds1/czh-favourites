package cz.bsl.czh.favourites.service;

// Grep anchor: favourites

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import cz.bsl.czh.favourites.api.FavouriteGroupDto;
import cz.bsl.czh.favourites.api.FavouriteGroupPageDto;
import cz.bsl.czh.favourites.api.FavouriteWagerDto;
import cz.bsl.czh.favourites.api.FavouriteWagerPageDto;
import cz.bsl.czh.favourites.api.WagerDto;
import cz.bsl.czh.favourites.dao.FavouriteGroupRecord;
import cz.bsl.czh.favourites.dao.FavouriteWagerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FavouritesConverterTest {

    private FavouritesConverter converter;

    @BeforeEach
    void setUp() {
        ObjectMapper mapper = JsonMapper.builder().build();
        converter = new FavouritesConverter(mapper);
    }

    @Test
    void toRecord_wager_setsAllFields() {
        WagerDto wager = new WagerDto("LOTTO", 100L, 100L, 1, "SN1",
                List.of(List.of(1, 2, 3)));
        FavouriteWagerDto dto = new FavouriteWagerDto();
        dto.setPlayerId("should-be-ignored");
        dto.setGroupNumber("1");
        dto.setWagerName("My Lotto");
        dto.setFlags(99);
        dto.setWager(wager);

        FavouriteWagerRecord record = converter.toRecord(dto, "player-42");

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
        FavouriteWagerRecord record = new FavouriteWagerRecord();
        record.setFavWagerId(7L);
        record.setPlayerId("player-1");
        record.setGroupNumber("2");
        record.setGameName("KENO");
        record.setWagerName("My Keno");
        record.setFlags(0);
        record.setCreatedAt(Instant.parse("2024-01-01T10:00:00Z"));
        record.setWagerJson("{\"gameName\":\"KENO\",\"stake\":50,\"price\":50,\"duration\":1,\"serialNumber\":\"SN2\",\"boards\":[[10,20,30]]}");

        FavouriteWagerDto dto = converter.toDto(record);

        assertEquals(7L, dto.getId());
        assertEquals("player-1", dto.getPlayerId());
        assertEquals("2", dto.getGroupNumber());
        assertNotNull(dto.getWager());
        assertEquals(List.of(List.of(10, 20, 30)), dto.getWager().getBoards(), "boards must round-trip correctly");
    }

    @Test
    void toRecord_wager_flagsAlwaysZero() {
        WagerDto wager = new WagerDto("GAME", 10L, 10L, 1, null, List.of());
        FavouriteWagerDto dto = new FavouriteWagerDto();
        dto.setFlags(99);
        dto.setGroupNumber("1");
        dto.setWager(wager);

        FavouriteWagerRecord record = converter.toRecord(dto, "p1");

        assertEquals(0, record.getFlags());
    }

    @Test
    void toRecord_group_setsAllFields() {
        FavouriteGroupDto dto = new FavouriteGroupDto();
        dto.setGroupNumber("3");
        dto.setGroupName("Lucky Group");
        dto.setFlags(5);

        FavouriteGroupRecord record = converter.toRecord(dto, "player-99");

        assertEquals("player-99", record.getPlayerId());
        assertEquals("3", record.getGroupNumber());
        assertEquals("Lucky Group", record.getGroupName());
        assertEquals(0, record.getFlags(), "flags must always be 0 on create");
    }

    @Test
    void toDto_group_roundTrip() {
        FavouriteGroupRecord record = new FavouriteGroupRecord();
        record.setFavGroupId(11L);
        record.setPlayerId("player-5");
        record.setGroupNumber("4");
        record.setGroupName("Friday Group");
        record.setFlags(0);
        record.setCreatedAt(Instant.parse("2024-02-01T00:00:00Z"));
        record.setUpdatedAt(Instant.parse("2024-02-02T00:00:00Z"));

        FavouriteGroupDto dto = converter.toDto(record);

        assertEquals(11L, dto.getId());
        assertEquals("player-5", dto.getPlayerId());
        assertEquals("4", dto.getGroupNumber());
        assertEquals("Friday Group", dto.getGroupName());
        assertNotNull(dto.getCreatedAt());
        assertNotNull(dto.getUpdatedAt());
    }

    @Test
    void toWagerPage_setsItemsAndCount() {
        FavouriteWagerRecord r = new FavouriteWagerRecord();
        r.setFavWagerId(1L);
        r.setPlayerId("p");
        r.setGroupNumber("1");
        r.setGameName("G");
        r.setWagerName("");
        r.setWagerJson("{\"gameName\":\"G\",\"stake\":1,\"price\":1,\"duration\":1,\"boards\":[]}");

        FavouriteWagerPageDto page = converter.toWagerPage(List.of(r));

        assertEquals(1, page.getTotalCount());
        assertEquals(1, page.getItems().size());
    }

    @Test
    void toGroupPage_setsItemsAndCount() {
        FavouriteGroupRecord r = new FavouriteGroupRecord();
        r.setFavGroupId(1L);
        r.setPlayerId("p");
        r.setGroupNumber("1");
        r.setGroupName("G");

        FavouriteGroupPageDto page = converter.toGroupPage(List.of(r));

        assertEquals(1, page.getTotalCount());
        assertEquals(1, page.getItems().size());
    }
}
