package cz.bsl.czh.favorites.service;

// Grep anchor: favorites

import cz.bsl.czh.favorites.api.BoardDto;
import cz.bsl.czh.favorites.api.FavoriteGroupDto;
import cz.bsl.czh.favorites.api.FavoriteWagerDto;
import cz.bsl.czh.favorites.api.FavoriteWagerGroupDto;
import cz.bsl.czh.favorites.api.WagerDto;
import cz.bsl.favorites.config.FavoritesProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Pre-condition validation for favorite wager and group operations. All checks throw
 * {@link IllegalArgumentException} with a human-readable message; the controller maps this
 * to HTTP 400.
 *
 * <p>Wire contract (legacy front end, gisorch): the display name is {@code name}; the group
 * travels as {@code group.id}/{@code group.name} on wagers and as {@code id} on the group
 * resource. Group ids are OPTIONAL on input: a legacy create body supplies {@code name} only
 * and the service auto-assigns the slot number. Wager boards must be non-empty and every
 * board must carry at least one selection (any of the selection fields).
 *
 * <p>No Hibernate Validator, no {@code ConstraintViolation} — plain {@code if} checks.
 */
@Component
public class FavoritesValidator {

    private static final int MAX_GAME_NAME_LENGTH  = 64;
    private static final int MAX_WAGER_NAME_LENGTH = 255;
    private static final int MAX_GROUP_NAME_LENGTH = 255;

    private final FavoritesProperties properties;

    public FavoritesValidator(FavoritesProperties properties) {
        this.properties = properties;
    }

    /**
     * Parse a comma-separated {@code game-names} query value.
     *
     * @return stripped, non-empty entries; empty list for null/blank input
     * @throws IllegalArgumentException if more than 50 entries are supplied
     */
    public static List<String> parseGameNamesCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        List<String> names = Arrays.stream(csv.split(","))
                .map(String::strip)
                .filter(s -> !s.isEmpty())
                .toList();
        if (names.size() > 50) {
            throw new IllegalArgumentException("game-names query parameter must not exceed 50 entries");
        }
        return names;
    }

    /**
     * Validate a wager create/update request.
     *
     * @throws IllegalArgumentException with a message suitable for HTTP 400 if any rule fails.
     */
    public void validateWager(FavoriteWagerDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Wager request must not be null");
        }
        WagerDto wager = dto.getWager();
        if (wager == null) {
            throw new IllegalArgumentException("Wager sub-object must not be null");
        }
        String gameName = wager.getGameName();
        if (gameName == null || gameName.isBlank()) {
            throw new IllegalArgumentException("Wager gameName must not be blank");
        }
        if (gameName.length() > MAX_GAME_NAME_LENGTH) {
            throw new IllegalArgumentException("Wager gameName exceeds maximum length of " + MAX_GAME_NAME_LENGTH);
        }
        if (dto.getName() != null && dto.getName().length() > MAX_WAGER_NAME_LENGTH) {
            throw new IllegalArgumentException("name exceeds maximum length of " + MAX_WAGER_NAME_LENGTH);
        }
        validateBoards(wager);
        if (dto.getGroup() != null) {
            validateWagerGroup(dto.getGroup());
        }
        // the group object itself is optional: the service resolves or creates it
        // (a legacy create body may carry the group by name only)
    }

    /**
     * Validate a group create/update request. The group id (slot number) is optional:
     * absent means the service auto-assigns the next free slot (legacy bodies are
     * {@code {"name": "..."}}).
     *
     * @throws IllegalArgumentException with a message suitable for HTTP 400 if any rule fails.
     */
    public void validateGroup(FavoriteGroupDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Group request must not be null");
        }
        if (dto.getName() == null) {
            throw new IllegalArgumentException("name must not be null (empty string is valid)");
        }
        if (dto.getName().length() > MAX_GROUP_NAME_LENGTH) {
            throw new IllegalArgumentException("name exceeds maximum length of " + MAX_GROUP_NAME_LENGTH);
        }
        if (dto.getId() != null && !dto.getId().isBlank()) {
            validateGroupNumber(dto.getId());
        }
    }

    /**
     * Validate that {@code id} is a positive long.
     *
     * @return the parsed id value
     * @throws IllegalArgumentException if blank, non-numeric, or not positive
     */
    public long validateId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Id must not be blank");
        }
        try {
            long value = Long.parseLong(id.strip());
            if (value <= 0) {
                throw new IllegalArgumentException("Id must be a positive integer, got: " + id);
            }
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Id is not a valid integer: " + id);
        }
    }

    /**
     * Validate that {@code groupNumber} is numeric and within the configured group index range.
     *
     * @throws IllegalArgumentException if blank, non-numeric, or out of range
     */
    public void validateGroupNumber(String groupNumber) {
        if (groupNumber == null || groupNumber.isBlank()) {
            throw new IllegalArgumentException("groupNumber must not be blank");
        }
        int value;
        try {
            value = Integer.parseInt(groupNumber.strip());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("groupNumber is not a valid integer: " + groupNumber);
        }
        int min = properties.getMinGroupIndex();
        int max = properties.getMaxGroupIndex();
        if (value < min || value > max) {
            throw new IllegalArgumentException(
                    "groupNumber " + value + " is outside allowed range [" + min + ", " + max + "]");
        }
    }

    /** Group object of a wager: optional id (when given: numeric, in range), name length. */
    private void validateWagerGroup(FavoriteWagerGroupDto group) {
        if (group.getId() != null && !group.getId().isBlank()) {
            validateGroupNumber(group.getId());
        }
        if (group.getName() != null && group.getName().length() > MAX_GROUP_NAME_LENGTH) {
            throw new IllegalArgumentException("group name exceeds maximum length of " + MAX_GROUP_NAME_LENGTH);
        }
    }

    /** Boards of the main wager and of every addon wager must be present and carry selections. */
    private void validateBoards(WagerDto wager) {
        requireBoards(wager.getBoards());
        if (wager.getAddonWagers() != null) {
            for (WagerDto addon : wager.getAddonWagers()) {
                requireBoards(addon.getBoards());
            }
        }
    }

    private void requireBoards(List<BoardDto> boards) {
        if (boards == null || boards.isEmpty()) {
            throw new IllegalArgumentException("Wager boards must not be empty");
        }
        for (BoardDto board : boards) {
            if (!hasSelections(board)) {
                throw new IllegalArgumentException("Each board must carry at least one selection");
            }
        }
    }

    private static boolean hasSelections(BoardDto board) {
        return hasValues(board.getSelections())
                || hasValues(board.getPrimarySelections())
                || hasValues(board.getSecondarySelections())
                || hasValues(board.getTertiarySelections())
                || hasValues(board.getPrimaryMultipleSelections())
                || hasValues(board.getSecondaryMultipleSelections())
                || hasValues(board.getTertiaryMultipleSelections());
    }

    private static boolean hasValues(List<?> values) {
        return values != null && !values.isEmpty();
    }
}
