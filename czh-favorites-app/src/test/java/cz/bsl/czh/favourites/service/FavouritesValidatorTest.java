package cz.bsl.czh.favourites.service;

// Grep anchor: favourites

import cz.bsl.czh.favourites.api.FavouriteGroupDto;
import cz.bsl.czh.favourites.api.FavouriteWagerDto;
import cz.bsl.czh.favourites.api.WagerDto;
import cz.bsl.favourites.config.FavouritesProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FavouritesValidatorTest {

    private FavouritesValidator validator;

    @BeforeEach
    void setUp() {
        FavouritesProperties props = new FavouritesProperties();
        // defaults: minGroupIndex=1, maxGroupIndex=10
        validator = new FavouritesValidator(props);
    }

    // --- validateWager ---

    @Test
    void validateWager_nullWager_throws() {
        assertThrows(IllegalArgumentException.class, () -> validator.validateWager(null));
    }

    @Test
    void validateWager_nullWagerSubObject_throws() {
        FavouriteWagerDto dto = new FavouriteWagerDto();
        dto.setGroupNumber("1");
        assertThrows(IllegalArgumentException.class, () -> validator.validateWager(dto));
    }

    @Test
    void validateWager_blankGameName_throws() {
        FavouriteWagerDto dto = wagerDto("", "1");
        assertThrows(IllegalArgumentException.class, () -> validator.validateWager(dto));
    }

    @Test
    void validateWager_gameNameTooLong_throws() {
        FavouriteWagerDto dto = wagerDto("A".repeat(65), "1");
        assertThrows(IllegalArgumentException.class, () -> validator.validateWager(dto));
    }

    @Test
    void validateWager_wagerNameTooLong_throws() {
        FavouriteWagerDto dto = wagerDto("LOTTO", "1");
        dto.setWagerName("W".repeat(256));
        assertThrows(IllegalArgumentException.class, () -> validator.validateWager(dto));
    }

    @Test
    void validateWager_groupNumberNonNumeric_throws() {
        FavouriteWagerDto dto = wagerDto("LOTTO", "abc");
        assertThrows(IllegalArgumentException.class, () -> validator.validateWager(dto));
    }

    @Test
    void validateWager_groupNumberBelowMin_throws() {
        FavouriteWagerDto dto = wagerDto("LOTTO", "0");
        assertThrows(IllegalArgumentException.class, () -> validator.validateWager(dto));
    }

    @Test
    void validateWager_groupNumberAboveMax_throws() {
        FavouriteWagerDto dto = wagerDto("LOTTO", "11");
        assertThrows(IllegalArgumentException.class, () -> validator.validateWager(dto));
    }

    @Test
    void validateWager_valid_noException() {
        FavouriteWagerDto dto = wagerDto("LOTTO", "5");
        validator.validateWager(dto);
    }

    // --- validateGroup ---

    @Test
    void validateGroup_nullDto_throws() {
        assertThrows(IllegalArgumentException.class, () -> validator.validateGroup(null));
    }

    @Test
    void validateGroup_nullGroupName_throws() {
        FavouriteGroupDto dto = new FavouriteGroupDto();
        dto.setGroupName(null);
        dto.setGroupNumber("1");
        assertThrows(IllegalArgumentException.class, () -> validator.validateGroup(dto));
    }

    @Test
    void validateGroup_groupNameTooLong_throws() {
        FavouriteGroupDto dto = new FavouriteGroupDto();
        dto.setGroupName("G".repeat(256));
        assertThrows(IllegalArgumentException.class, () -> validator.validateGroup(dto));
    }

    @Test
    void validateGroup_groupNumberNonNumeric_throws() {
        FavouriteGroupDto dto = new FavouriteGroupDto();
        dto.setGroupName("Group A");
        dto.setGroupNumber("xyz");
        assertThrows(IllegalArgumentException.class, () -> validator.validateGroup(dto));
    }

    @Test
    void validateGroup_valid_noException() {
        FavouriteGroupDto dto = new FavouriteGroupDto();
        dto.setGroupName("My Group");
        dto.setGroupNumber("3");
        validator.validateGroup(dto);
    }

    @Test
    void validateGroup_emptyGroupName_noException() {
        FavouriteGroupDto dto = new FavouriteGroupDto();
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

    private static FavouriteWagerDto wagerDto(String gameName, String groupNumber) {
        FavouriteWagerDto dto = new FavouriteWagerDto();
        dto.setGroupNumber(groupNumber);
        WagerDto wager = new WagerDto(gameName, 100L, 100L, 1, null, List.of());
        dto.setWager(wager);
        return dto;
    }
}
