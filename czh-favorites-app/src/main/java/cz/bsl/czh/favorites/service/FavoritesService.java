package cz.bsl.czh.favorites.service;

// Grep anchor: favorites

import cz.bsl.czh.favorites.api.FavoriteGroupDto;
import cz.bsl.czh.favorites.api.FavoriteGroupPageDto;
import cz.bsl.czh.favorites.api.FavoriteWagerDto;
import cz.bsl.czh.favorites.api.FavoriteWagerPageDto;

import java.util.List;

/**
 * Business operations for favorite wagers and groups. Validates inputs, enforces quotas, and
 * delegates to the DAO layer. Throws {@link IllegalArgumentException} for HTTP 400 and
 * {@link java.util.NoSuchElementException} for HTTP 404.
 */
public interface FavoritesService {

    // --- Wager operations ---

    /** Create and persist a favorite wager. Validates limits before insert. */
    FavoriteWagerDto createWager(FavoriteWagerDto dto, String playerId);

    /** Get a single wager by id. Throws NoSuchElementException if not found or not owned by player. */
    FavoriteWagerDto getWager(long wagerId, String playerId);

    /** Update mutable fields (wagerName, groupNumber, wager JSON) of an existing wager. */
    FavoriteWagerDto updateWager(long wagerId, FavoriteWagerDto dto, String playerId);

    /** Delete a wager by id. Throws NoSuchElementException if not found or not owned by player. */
    void deleteWager(long wagerId, String playerId);

    /**
     * List wagers for a player, optionally filtered by groupNumber and/or gameName(s).
     * Returns a page DTO (totalCount = items.size()).
     */
    FavoriteWagerPageDto listWagers(String playerId, String groupNumber, List<String> gameNames);

    // --- Group operations ---

    /** Create a new favorite wager group. Enforces maxGroupIndex limit and duplicate check. */
    FavoriteGroupDto createGroup(FavoriteGroupDto dto, String playerId);

    /** Get a single group by groupNumber. Throws NoSuchElementException if not found. */
    FavoriteGroupDto getGroup(String groupNumber, String playerId);

    /** Update the group name. Throws NoSuchElementException if not found. */
    FavoriteGroupDto updateGroup(String groupNumber, FavoriteGroupDto dto, String playerId);

    /** Delete a group by groupNumber. Throws NoSuchElementException if not found. */
    void deleteGroup(String groupNumber, String playerId);

    /** List all groups for a player. */
    FavoriteGroupPageDto listGroups(String playerId);
}
