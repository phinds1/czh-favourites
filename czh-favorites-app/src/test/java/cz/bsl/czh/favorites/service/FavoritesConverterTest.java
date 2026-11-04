package cz.bsl.czh.favorites.service;

// Grep anchor: favorites

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import cz.bsl.czh.favorites.api.BoardDto;
import cz.bsl.czh.favorites.api.FavoriteGroupDto;
import cz.bsl.czh.favorites.api.FavoriteGroupPageDto;
import cz.bsl.czh.favorites.api.FavoriteWagerDto;
import cz.bsl.czh.favorites.api.FavoriteWagerGroupDto;
import cz.bsl.czh.favorites.api.FavoriteWagerPageDto;
import cz.bsl.czh.favorites.api.WagerDto;
import cz.bsl.czh.favorites.api.WagerType;
import cz.bsl.czh.favorites.dao.FavoriteGroupRecord;
import cz.bsl.czh.favorites.dao.FavoriteWagerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FavoritesConverterTest {

    private FavoritesConverter converter;

    @BeforeEach
    void setUp() {
        ObjectMapper mapper = JsonMapper.builder().build();
        converter = new FavoritesConverter(mapper);
    }

    // --- wager: DTO -> record ---

    @Test
    void toRecord_wager_mapsLegacyFields() {
        FavoriteWagerDto dto = new FavoriteWagerDto();
        dto.setPlayerId("should-be-ignored");
        dto.setName("My Lotto");
        dto.setFlags(99);
        dto.setGroup(new FavoriteWagerGroupDto("2", "My Group"));
        dto.setWager(sportkaWager());

        FavoriteWagerRecord record = converter.toRecord(dto, "player-42", "2");

        assertEquals("player-42", record.getPlayerId(), "playerId must come from header param, not DTO");
        assertEquals("2", record.getGroupNumber(), "group number is the resolved wire group.id");
        assertEquals("Sportka", record.getGameName(), "gameName is derived from wager.gameName");
        assertEquals("My Lotto", record.getWagerName(), "wire name maps to the WAGER_NAME column");
        assertEquals(0, record.getFlags(), "flags must always be 0 on create");
        assertNotNull(record.getWagerJson());
    }

    @Test
    void toRecord_wager_keepsPlayerChosenFieldsInJson() {
        FavoriteWagerDto dto = new FavoriteWagerDto();
        dto.setName("N");
        WagerDto wager = sportkaWager();
        wager.setSerialNumber("SN-001");
        dto.setWager(wager);

        String json = converter.toRecord(dto, "p1", "1").getWagerJson();

        assertTrue(json.contains("\"type\":\"NORMAL\""), "type is player-chosen and must survive");
        assertTrue(json.contains("\"gameName\":\"Sportka\""));
        assertTrue(json.contains("\"primarySelections\":[\"1\",\"2\",\"3\",\"4\",\"5\",\"6\"]"));
        assertTrue(json.contains("\"addonWagers\":[{"), "addon wagers must survive");
        assertTrue(json.contains("\"serialNumber\":\"SN-001\""));
        assertTrue(json.contains("\"drawNames\":[\"WED\",\"FRI\",\"SUN\"]"));
    }

    @Test
    void toRecord_wager_stripsServerAssignedFields() {
        WagerDto wager = sportkaWager();
        wager.setId("ESTE-1");
        wager.setStatus("WON");
        wager.setRejectReason("no");
        wager.setRequestId("req-1");
        wager.setDigest("d1");
        wager.setTransactionTime(1730000000000L);
        wager.setSignature("sig");
        wager.setRevision("r1");
        wager.setPrizes(List.of());
        wager.setFreeTicket(new WagerDto());
        wager.setDiscountAmount(10L);
        wager.setTaxes(List.of());

        FavoriteWagerDto dto = new FavoriteWagerDto();
        dto.setName("N");
        dto.setWager(wager);

        String json = converter.toRecord(dto, "p1", "1").getWagerJson();

        assertFalse(json.contains("ESTE-1"), "server-assigned wager id must be stripped");
        assertFalse(json.contains("WON"), "status must be stripped");
        assertFalse(json.contains("rejectReason"));
        assertFalse(json.contains("req-1"), "requestId must be stripped");
        assertFalse(json.contains("digest"), "digest must be stripped");
        assertFalse(json.contains("transactionTime"), "transactionTime must be stripped");
        assertFalse(json.contains("signature"), "signature must be stripped");
        assertFalse(json.contains("revision"));
        assertFalse(json.contains("prizes"));
        assertFalse(json.contains("freeTicket"));
        assertFalse(json.contains("discountAmount"));
        assertFalse(json.contains("taxes"));
    }

    @Test
    void toRecord_wager_stripsAddonServerAssignedFields() {
        WagerDto addon = new WagerDto();
        addon.setGameName("Sance");
        addon.setStatus("WON");
        addon.setBoards(List.of(board("1", "2")));
        WagerDto wager = new WagerDto();
        wager.setGameName("Sportka");
        wager.setBoards(List.of(board("1", "2")));
        wager.setAddonWagers(List.of(addon));

        FavoriteWagerDto dto = new FavoriteWagerDto();
        dto.setName("N");
        dto.setWager(wager);

        String json = converter.toRecord(dto, "p1", "1").getWagerJson();

        assertTrue(json.contains("Sance"), "the addon itself must survive");
        assertFalse(json.contains("WON"), "addon server-assigned fields must be stripped too");
    }

    @Test
    void toRecord_wager_nullWager_throws() {
        FavoriteWagerDto dto = new FavoriteWagerDto();
        assertThrows(IllegalArgumentException.class, () -> converter.toRecord(dto, "p1", "1"));
    }

    @Test
    void toRecord_wager_oversizeJson_throws() {
        List<String> picks = new java.util.ArrayList<>();
        for (int i = 0; i < 4000; i++) {
            picks.add("number-" + i);
        }
        WagerDto wager = new WagerDto();
        wager.setGameName("G");
        wager.setBoards(List.of(board(picks.toArray(new String[0]))));

        FavoriteWagerDto dto = new FavoriteWagerDto();
        dto.setName("N");
        dto.setWager(wager);

        assertThrows(IllegalArgumentException.class, () -> converter.toRecord(dto, "p1", "1"));
    }

    // --- wager: record -> DTO ---

    @Test
    void toDto_wager_idIsStringAndGroupCarriesSlotNumber() {
        FavoriteWagerRecord record = new FavoriteWagerRecord();
        record.setFavWagerId(7L);
        record.setPlayerId("player-1");
        record.setGroupNumber("2");
        record.setGameName("KENO");
        record.setWagerName("My Keno");
        record.setFlags(0);
        record.setCreatedAt(Instant.parse("2024-01-01T10:00:00Z"));
        record.setWagerJson("{\"gameName\":\"KENO\",\"stake\":50,\"price\":50,\"duration\":1," +
                "\"serialNumber\":\"SN2\",\"boards\":[{\"selections\":[\"10\",\"20\",\"30\"]}]}");

        FavoriteWagerDto dto = converter.toDto(record);

        assertEquals("7", dto.getId(), "wire id is the surrogate key as a string");
        assertEquals("player-1", dto.getPlayerId());
        assertEquals("2", dto.getGroup().getId(), "wire group.id is the slot number");
        assertEquals("My Keno", dto.getName());
        assertEquals("KENO", dto.getGameName());
        assertNotNull(dto.getWager());
        assertEquals(List.of("10", "20", "30"), dto.getWager().getBoards().get(0).getSelections(),
                "boards must round-trip correctly");
    }

    @Test
    void toDto_wager_nullGroupNumber_omitsGroup() {
        FavoriteWagerRecord record = new FavoriteWagerRecord();
        record.setFavWagerId(1L);
        record.setPlayerId("p");
        record.setGameName("G");
        record.setWagerName("");
        record.setWagerJson("{\"gameName\":\"G\",\"stake\":1,\"price\":1,\"duration\":1," +
                "\"boards\":[{\"selections\":[\"1\"]}]}");

        FavoriteWagerDto dto = converter.toDto(record);

        assertNull(dto.getGroup());
    }

    // --- group ---

    @Test
    void toRecord_group_setsAllFields() {
        FavoriteGroupDto dto = new FavoriteGroupDto();
        dto.setName("Lucky Group");
        dto.setFlags(5);

        FavoriteGroupRecord record = converter.toRecord(dto, "player-99", "3");

        assertEquals("player-99", record.getPlayerId());
        assertEquals("3", record.getGroupNumber(), "resolved slot number, not a DTO field");
        assertEquals("Lucky Group", record.getGroupName());
        assertEquals(0, record.getFlags(), "flags must always be 0 on create");
    }

    @Test
    void toDto_group_idIsSlotNumber() {
        FavoriteGroupRecord record = new FavoriteGroupRecord();
        record.setFavGroupId(11L);
        record.setPlayerId("player-5");
        record.setGroupNumber("4");
        record.setGroupName("Friday Group");
        record.setFlags(0);
        record.setCreatedAt(Instant.parse("2024-02-01T00:00:00Z"));
        record.setUpdatedAt(Instant.parse("2024-02-02T00:00:00Z"));

        FavoriteGroupDto dto = converter.toDto(record);

        assertEquals("4", dto.getId(), "wire id is the group slot number, not the surrogate key");
        assertEquals("player-5", dto.getPlayerId());
        assertEquals("Friday Group", dto.getName());
        assertNotNull(dto.getCreatedAt());
        assertNotNull(dto.getUpdatedAt());
    }

    // --- pages ---

    @Test
    void toWagerPage_setsFavoriteWagersAndCount() {
        FavoriteWagerRecord r = new FavoriteWagerRecord();
        r.setFavWagerId(1L);
        r.setPlayerId("p");
        r.setGroupNumber("1");
        r.setGameName("G");
        r.setWagerName("");
        r.setWagerJson("{\"gameName\":\"G\",\"stake\":1,\"price\":1,\"duration\":1," +
                "\"boards\":[{\"selections\":[\"1\"]}]}");

        FavoriteWagerPageDto page = converter.toWagerPage(List.of(r));

        assertEquals(1, page.getTotalCount());
        assertEquals(1, page.getFavoriteWagers().size());
    }

    @Test
    void toGroupPage_setsFavoriteWagerGroupsAndCount() {
        FavoriteGroupRecord r = new FavoriteGroupRecord();
        r.setFavGroupId(1L);
        r.setPlayerId("p");
        r.setGroupNumber("1");
        r.setGroupName("G");

        FavoriteGroupPageDto page = converter.toGroupPage(List.of(r));

        assertEquals(1, page.getTotalCount());
        assertEquals(1, page.getFavoriteWagerGroups().size());
    }

    // --- helpers ---

    /** The legacy sportka wager: type/drawNames/gameName/price/addonWagers/boards. */
    private static WagerDto sportkaWager() {
        WagerDto addon = new WagerDto();
        addon.setGameName("Sance");
        addon.setAddonPlayed(false);
        addon.setBoards(List.of(board("1", "2", "3", "4", "5", "6")));

        WagerDto wager = new WagerDto();
        wager.setType(WagerType.NORMAL);
        wager.setDuration(3);
        wager.setDrawNames(List.of("WED", "FRI", "SUN"));
        wager.setGameName("Sportka");
        wager.setPrice(9000L);
        wager.setAddonWagers(List.of(addon));
        wager.setBoards(List.of(board("1", "2", "3", "4", "5", "6")));
        return wager;
    }

    private static BoardDto board(String... selections) {
        BoardDto board = new BoardDto();
        board.setPrimarySelections(List.of(selections));
        return board;
    }
}
