package cz.bsl.czh.favorites.service;

// Grep anchor: favorites

import cz.bsl.czh.favorites.api.BoardDto;
import cz.bsl.czh.favorites.api.FavoriteGroupDto;
import cz.bsl.czh.favorites.api.FavoriteWagerDto;
import cz.bsl.czh.favorites.api.FavoriteWagerGroupDto;
import cz.bsl.czh.favorites.api.WagerDto;
import cz.bsl.favorites.config.FavoritesProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FavoritesValidatorTest {

    private FavoritesValidator validator;

    @BeforeEach
    void setUp() {
        FavoritesProperties props = new FavoritesProperties();
        // defaults: minGroupIndex=1, maxGroupIndex=10
        validator = new FavoritesValidator(props);
    }

    // --- validateWager ---

    @Test
    void validateWager_nullWager_throws() {
        assertThrows(IllegalArgumentException.class, () -> validator.validateWager(null));
    }

    @Test
    void validateWager_nullWagerSubObject_throws() {
        FavoriteWagerDto dto = new FavoriteWagerDto();
        assertThrows(IllegalArgumentException.class, () -> validator.validateWager(dto));
    }

    @Test
    void validateWager_blankGameName_throws() {
        FavoriteWagerDto dto = wagerDto("", "1");
        assertThrows(IllegalArgumentException.class, () -> validator.validateWager(dto));
    }

    @Test
    void validateWager_gameNameTooLong_throws() {
        FavoriteWagerDto dto = wagerDto("A".repeat(65), "1");
        assertThrows(IllegalArgumentException.class, () -> validator.validateWager(dto));
    }

    @Test
    void validateWager_nameTooLong_throws() {
        FavoriteWagerDto dto = wagerDto("LOTTO", "1");
        dto.setName("W".repeat(256));
        assertThrows(IllegalArgumentException.class, () -> validator.validateWager(dto));
    }

    @Test
    void validateWager_groupIdNonNumeric_throws() {
        FavoriteWagerDto dto = wagerDto("LOTTO", "abc");
        assertThrows(IllegalArgumentException.class, () -> validator.validateWager(dto));
    }

    @Test
    void validateWager_groupIdBelowMin_throws() {
        FavoriteWagerDto dto = wagerDto("LOTTO", "0");
        assertThrows(IllegalArgumentException.class, () -> validator.validateWager(dto));
    }

    @Test
    void validateWager_groupIdAboveMax_throws() {
        FavoriteWagerDto dto = wagerDto("LOTTO", "11");
        assertThrows(IllegalArgumentException.class, () -> validator.validateWager(dto));
    }

    @Test
    void validateWager_groupNameTooLong_throws() {
        FavoriteWagerDto dto = wagerDto("LOTTO", "1");
        dto.getGroup().setName("G".repeat(256));
        assertThrows(IllegalArgumentException.class, () -> validator.validateWager(dto));
    }

    @Test
    void validateWager_withoutGroup_isValid() {
        // legacy front end always sends a group, but the service must resolve/create
        // one when absent (auto-assign) — validation passes
        FavoriteWagerDto dto = new FavoriteWagerDto();
        dto.setName("N");
        dto.setWager(wager("LOTTO"));
        assertDoesNotThrow(() -> validator.validateWager(dto));
    }

    @Test
    void validateWager_groupWithoutId_isValid() {
        FavoriteWagerDto dto = wagerDto("LOTTO", null);
        dto.getGroup().setName("Sportka Group");
        assertDoesNotThrow(() -> validator.validateWager(dto));
    }

    @Test
    void validateWager_valid_noException() {
        FavoriteWagerDto dto = wagerDto("LOTTO", "5");
        assertDoesNotThrow(() -> validator.validateWager(dto));
    }

    // --- validateWager: boards ---

    @Test
    void validateWager_nullBoards_throws() {
        FavoriteWagerDto dto = new FavoriteWagerDto();
        WagerDto wager = wager("LOTTO");
        wager.setBoards(null);
        dto.setWager(wager);
        assertThrows(IllegalArgumentException.class, () -> validator.validateWager(dto));
    }

    @Test
    void validateWager_emptyBoards_throws() {
        FavoriteWagerDto dto = new FavoriteWagerDto();
        WagerDto wager = wager("LOTTO");
        wager.setBoards(List.of());
        dto.setWager(wager);
        assertThrows(IllegalArgumentException.class, () -> validator.validateWager(dto));
    }

    @Test
    void validateWager_boardWithoutSelections_throws() {
        FavoriteWagerDto dto = new FavoriteWagerDto();
        WagerDto wager = wager("LOTTO");
        BoardDto empty = new BoardDto();
        empty.setQuickpick(false);
        wager.setBoards(List.of(empty));
        dto.setWager(wager);
        assertThrows(IllegalArgumentException.class, () -> validator.validateWager(dto));
    }

    @Test
    void validateWager_boardWithPrimarySelections_isValid() {
        FavoriteWagerDto dto = new FavoriteWagerDto();
        WagerDto wager = wager("Sportka");
        BoardDto board = new BoardDto();
        board.setPrimarySelections(List.of("1", "2", "3", "4", "5", "6"));
        wager.setBoards(List.of(board));
        dto.setWager(wager);
        assertDoesNotThrow(() -> validator.validateWager(dto));
    }

    @Test
    void validateWager_boardWithMultipleSelections_isValid() {
        FavoriteWagerDto dto = new FavoriteWagerDto();
        WagerDto wager = wager("Keno");
        BoardDto board = new BoardDto();
        board.setPrimaryMultipleSelections(List.of(List.of("1", "2"), List.of("3", "4")));
        wager.setBoards(List.of(board));
        dto.setWager(wager);
        assertDoesNotThrow(() -> validator.validateWager(dto));
    }

    @Test
    void validateWager_addonBoardsValidatedToo_throwsOnEmptyAddonBoard() {
        FavoriteWagerDto dto = new FavoriteWagerDto();
        WagerDto wager = wager("Sportka");
        wager.setBoards(List.of(boardWithSelections()));

        WagerDto addon = new WagerDto();
        addon.setGameName("Sance");
        addon.setBoards(List.of());
        wager.setAddonWagers(List.of(addon));

        dto.setWager(wager);
        assertThrows(IllegalArgumentException.class, () -> validator.validateWager(dto));
    }

    // --- validateGroup ---

    @Test
    void validateGroup_nullDto_throws() {
        assertThrows(IllegalArgumentException.class, () -> validator.validateGroup(null));
    }

    @Test
    void validateGroup_nullName_throws() {
        FavoriteGroupDto dto = new FavoriteGroupDto();
        dto.setName(null);
        assertThrows(IllegalArgumentException.class, () -> validator.validateGroup(dto));
    }

    @Test
    void validateGroup_nameTooLong_throws() {
        FavoriteGroupDto dto = new FavoriteGroupDto();
        dto.setName("G".repeat(256));
        assertThrows(IllegalArgumentException.class, () -> validator.validateGroup(dto));
    }

    @Test
    void validateGroup_idNonNumeric_throws() {
        FavoriteGroupDto dto = new FavoriteGroupDto();
        dto.setName("Group A");
        dto.setId("xyz");
        assertThrows(IllegalArgumentException.class, () -> validator.validateGroup(dto));
    }

    @Test
    void validateGroup_valid_noException() {
        FavoriteGroupDto dto = new FavoriteGroupDto();
        dto.setName("My Group");
        dto.setId("3");
        assertDoesNotThrow(() -> validator.validateGroup(dto));
    }

    @Test
    void validateGroup_legacyBodyNameOnly_noException() {
        // the legacy group create body is {"name": "V Group"} — no id (auto-assigned)
        FavoriteGroupDto dto = new FavoriteGroupDto();
        dto.setName("V Group");
        assertDoesNotThrow(() -> validator.validateGroup(dto));
    }

    @Test
    void validateGroup_emptyName_noException() {
        FavoriteGroupDto dto = new FavoriteGroupDto();
        dto.setName("");
        assertDoesNotThrow(() -> validator.validateGroup(dto));
    }

    // --- validateId ---

    @Test
    void validateId_blank_throws() {
        assertThrows(IllegalArgumentException.class, () -> validator.validateId(""));
    }

    @Test
    void validateId_null_throws() {
        assertThrows(IllegalArgumentException.class, () -> validator.validateId(null));
    }

    @Test
    void validateId_negative_throws() {
        assertThrows(IllegalArgumentException.class, () -> validator.validateId("-1"));
    }

    @Test
    void validateId_zero_throws() {
        assertThrows(IllegalArgumentException.class, () -> validator.validateId("0"));
    }

    @Test
    void validateId_valid_returnsLong() {
        long id = validator.validateId("42");
        assertEquals(42L, id);
    }

    // --- helpers ---

    private static FavoriteWagerDto wagerDto(String gameName, String groupId) {
        FavoriteWagerDto dto = new FavoriteWagerDto();
        dto.setName("N");
        dto.setGroup(groupId == null ? new FavoriteWagerGroupDto() : new FavoriteWagerGroupDto(groupId, "G"));
        dto.setWager(wager(gameName));
        return dto;
    }

    private static WagerDto wager(String gameName) {
        WagerDto wager = new WagerDto();
        wager.setGameName(gameName);
        wager.setStake(100L);
        wager.setPrice(100L);
        wager.setDuration(1);
        wager.setBoards(List.of(boardWithSelections()));
        return wager;
    }

    private static BoardDto boardWithSelections() {
        BoardDto board = new BoardDto();
        board.setSelections(List.of("1", "2", "3"));
        return board;
    }

    // --- parseGameNamesCsv ---

    @Test
    void parseGameNamesCsv_nullAndBlank_returnEmpty() {
        assertEquals(List.of(), FavoritesValidator.parseGameNamesCsv(null));
        assertEquals(List.of(), FavoritesValidator.parseGameNamesCsv("   "));
    }

    @Test
    void parseGameNamesCsv_stripsSpacesAndDropsEmpties() {
        assertEquals(List.of("A", "B"), FavoritesValidator.parseGameNamesCsv(" A , ,B,"));
    }

    @Test
    void parseGameNamesCsv_fiftyOk_fiftyOneThrows() {
        String fifty = String.join(",", java.util.Collections.nCopies(50, "G"));
        assertEquals(50, FavoritesValidator.parseGameNamesCsv(fifty).size());
        assertThrows(IllegalArgumentException.class,
                () -> FavoritesValidator.parseGameNamesCsv(fifty + ",G"));
    }
}
