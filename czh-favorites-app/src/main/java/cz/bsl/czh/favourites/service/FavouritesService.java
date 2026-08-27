package cz.bsl.czh.favourites.service;

// Grep anchor: favourites

import cz.bsl.czh.favourites.api.FavouriteGroupDto;
import cz.bsl.czh.favourites.api.FavouriteGroupPageDto;
import cz.bsl.czh.favourites.api.FavouriteWagerDto;
import cz.bsl.czh.favourites.api.FavouriteWagerPageDto;

import java.util.List;

/**
 * Business operations for favourite wagers and groups. Validates inputs, enforces quotas, and
 * delegates to the DAO layer. Throws {@link IllegalArgumentException} for HTTP 400 and
 * {@link java.util.NoSuchElementException} for HTTP 404.
 */
public interface FavouritesService {

    // --- Wager operations ---

    /** Create and persist a favourite wager. Validates limits before insert. */
    FavouriteWagerDto createWager(FavouriteWagerDto dto, String playerId);

    /** Get a single wager by id. Throws NoSuchElementException if not found or not owned by player. */
    FavouriteWagerDto getWager(long wagerId, String playerId);

    /** Update mutable fields (wagerName, groupNumber, wager JSON) of an existing wager. */
    FavouriteWagerDto updateWager(long wagerId, FavouriteWagerDto dto, String playerId);

    /** Delete a wager by id. Throws NoSuchElementException if not found or not owned by player. */
    void deleteWager(long wagerId, String playerId);

    /**
     * List wagers for a player, optionally filtered by groupNumber and/or gameName(s).
     * Returns a page DTO (totalCount = items.size()).
     */
    FavouriteWagerPageDto listWagers(String playerId, String groupNumber, List<String> gameNames);

    // --- Group operations ---

    /** Create a new favourite wager group. Enforces maxGroupIndex limit and duplicate check. */
    FavouriteGroupDto createGroup(FavouriteGroupDto dto, String playerId);

    /** Get a single group by groupNumber. Throws NoSuchElementException if not found. */
    FavouriteGroupDto getGroup(String groupNumber, String playerId);

    /** Update the group name. Throws NoSuchElementException if not found. */
    FavouriteGroupDto updateGroup(String groupNumber, FavouriteGroupDto dto, String playerId);

    /** Delete a group by groupNumber. Throws NoSuchElementException if not found. */
    void deleteGroup(String groupNumber, String playerId);

    /** List all groups for a player. */
    FavouriteGroupPageDto listGroups(String playerId);
}
