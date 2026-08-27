package cz.bsl.czh.favourites.dao;

import java.util.List;

/**
 * Persistence boundary for {@code GIS_FAV_GROUP}. All SQL is in the implementation;
 * callers never construct query strings.
 *
 * <p>Grep anchor: favourites
 */
public interface FavouritesGroupDao {

    /**
     * Insert a new group. Returns the generated {@code FAV_GROUP_ID}.
     * Throws {@code DuplicateKeyException} on duplicate {@code (PLAYER_ID, GROUP_NUMBER)} —
     * do not catch; the service layer handles it as HTTP 409.
     */
    long insert(FavouriteGroupRecord group);

    /** All groups for a player, ordered by {@code GROUP_NUMBER}. */
    List<FavouriteGroupRecord> findByPlayer(String playerId);

    /**
     * Single group by player + groupNumber.
     * Returns {@code null} if absent.
     */
    FavouriteGroupRecord findByPlayerAndGroup(String playerId, String groupNumber);

    /**
     * Update {@code GROUP_NAME}; sets {@code UPDATED_AT = CURRENT_TIMESTAMP}.
     * Returns the number of rows updated (0 if the id does not exist).
     */
    int updateName(long favGroupId, String groupName);

    /**
     * Delete by {@code FAV_GROUP_ID}.
     * Returns the number of rows deleted (0 if the id does not exist).
     */
    int delete(long favGroupId);

    /** Count of groups owned by {@code playerId}. */
    int countByPlayer(String playerId);
}
