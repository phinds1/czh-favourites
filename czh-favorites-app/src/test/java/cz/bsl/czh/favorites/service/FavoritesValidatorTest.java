package cz.bsl.czh.favorites.service;

// Grep anchor: favorites

import cz.bsl.czh.favorites.api.FavoriteGroupDto;
import cz.bsl.czh.favorites.api.FavoriteWagerDto;
import cz.bsl.czh.favorites.api.WagerDto;
import cz.bsl.favorites.config.FavoritesProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

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
        dto.setGroupNumber("1");
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
    void validateWager_wagerNameTooLong_throws() {
        FavoriteWagerDto dto = wagerDto("LOTTO", "1");
        dto.setWagerName("W".repeat(256));
        assertThrows(IllegalArgumentException.class, () -> validator.validateWager(dto));
    }

    @Test
    void validateWager_groupNumberNonNumeric_throws() {
        FavoriteWagerDto dto = wagerDto("LOTTO", "abc");
        assertThrows(IllegalArgumentException.class, () -> validator.validateWager(dto));
    }

    @Test
    void validateWager_groupNumberBelowMin_throws() {
        FavoriteWagerDto dto = wagerDto("LOTTO", "0");
        assertThrows(IllegalArgumentException.class, () -> validator.validateWager(dto));
    }

    @Test
    void validateWager_groupNumberAboveMax_throws() {
        FavoriteWagerDto dto = wagerDto("LOTTO", "11");
        assertThrows(IllegalArgumentException.class, () -> validator.validateWager(dto));
    }

    @Test
    void validateWager_valid_noException() {
        FavoriteWagerDto dto = wagerDto("LOTTO", "5");
        validator.validateWager(dto);
    }

    // --- validateGroup ---

    @Test
    void validateGroup_nullDto_throws() {
        assertThrows(IllegalArgumentException.class, () -> validator.validateGroup(null));
    }

    @Test
    void validateGroup_nullGroupName_throws() {
        FavoriteGroupDto dto = new FavoriteGroupDto();
        dto.setGroupName(null);
        dto.setGroupNumber("1");
        assertThrows(IllegalArgumentException.class, () -> validator.validateGroup(dto));
    }

    @Test
    void validateGroup_groupNameTooLong_throws() {
        FavoriteGroupDto dto = new FavoriteGroupDto();
        dto.setGroupName("G".repeat(256));
        assertThrows(IllegalArgumentException.class, () -> validator.validateGroup(dto));
    }

    @Test
    void validateGroup_groupNumberNonNumeric_throws() {
        FavoriteGroupDto dto = new FavoriteGroupDto();
        dto.setGroupName("Group A");
        dto.setGroupNumber("xyz");
        assertThrows(IllegalArgumentException.class, () -> validator.validateGroup(dto));
    }

    @Test
    void validateGroup_valid_noException() {
        FavoriteGroupDto dto = new FavoriteGroupDto();
        dto.setGroupName("My Group");
        dto.setGroupNumber("3");
        validator.validateGroup(dto);
    }

    @Test
    void validateGroup_emptyGroupName_noException() {
        FavoriteGroupDto dto = new FavoriteGroupDto();
        dto.setGroupNumber("1");
        dto.setGroupName("");
        validator.validateGroup(dto);
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

    private static FavoriteWagerDto wagerDto(String gameName, String groupNumber) {
        FavoriteWagerDto dto = new FavoriteWagerDto();
        dto.setGroupNumber(groupNumber);
        WagerDto wager = new WagerDto(gameName, 100L, 100L, 1, null, List.of());
        dto.setWager(wager);
        return dto;
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
