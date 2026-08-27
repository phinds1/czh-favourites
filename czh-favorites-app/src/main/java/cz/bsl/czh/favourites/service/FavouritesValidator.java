package cz.bsl.czh.favourites.service;

// Grep anchor: favourites

import cz.bsl.czh.favourites.api.FavouriteGroupDto;
import cz.bsl.czh.favourites.api.FavouriteWagerDto;
import cz.bsl.favourites.config.FavouritesProperties;
import org.springframework.stereotype.Component;

/**
 * Pre-condition validation for favourite wager and group operations. All checks throw
 * {@link IllegalArgumentException} with a human-readable message; the controller maps this
 * to HTTP 400.
 *
 * <p>No Hibernate Validator, no {@code ConstraintViolation} — plain {@code if} checks.
 */
@Component
public class FavouritesValidator {

    private static final int MAX_GAME_NAME_LENGTH  = 64;
    private static final int MAX_WAGER_NAME_LENGTH = 255;
    private static final int MAX_GROUP_NAME_LENGTH = 255;

    private final FavouritesProperties properties;

    public FavouritesValidator(FavouritesProperties properties) {
        this.properties = properties;
    }

    /**
     * Validate a wager create/update request.
     *
     * @throws IllegalArgumentException with a message suitable for HTTP 400 if any rule fails.
     */
    public void validateWager(FavouriteWagerDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Wager request must not be null");
        }
        if (dto.getWager() == null) {
            throw new IllegalArgumentException("Wager sub-object must not be null");
        }
        String gameName = dto.getWager().getGameName();
        if (gameName == null || gameName.isBlank()) {
            throw new IllegalArgumentException("Wager gameName must not be blank");
        }
        if (gameName.length() > MAX_GAME_NAME_LENGTH) {
            throw new IllegalArgumentException("Wager gameName exceeds maximum length of " + MAX_GAME_NAME_LENGTH);
        }
        if (dto.getWagerName() != null && dto.getWagerName().length() > MAX_WAGER_NAME_LENGTH) {
            throw new IllegalArgumentException("wagerName exceeds maximum length of " + MAX_WAGER_NAME_LENGTH);
        }
        validateGroupNumber(dto.getGroupNumber());
    }

    /**
     * Validate a group create/update request.
     *
     * @throws IllegalArgumentException with a message suitable for HTTP 400 if any rule fails.
     */
    public void validateGroup(FavouriteGroupDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Group request must not be null");
        }
        if (dto.getGroupName() == null) {
            throw new IllegalArgumentException("groupName must not be null (empty string is valid)");
        }
        if (dto.getGroupName().length() > MAX_GROUP_NAME_LENGTH) {
            throw new IllegalArgumentException("groupName exceeds maximum length of " + MAX_GROUP_NAME_LENGTH);
        }
        if (dto.getGroupNumber() != null) {
            validateGroupNumber(dto.getGroupNumber());
        } else {
            throw new IllegalArgumentException("groupNumber must not be null");
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
}
