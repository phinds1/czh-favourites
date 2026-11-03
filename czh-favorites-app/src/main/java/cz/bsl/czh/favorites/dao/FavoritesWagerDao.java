package cz.bsl.czh.favorites.dao;

import java.util.List;

/**
 * Persistence boundary for {@code GIS_FAV_WAGER}. All SQL is in the implementation.
 * The {@code wagerJson} field on {@link FavoriteWagerRecord} is an opaque JSON string;
 * callers (service layer, SP5) own {@code ObjectMapper} round-trips.
 *
 * <p>Grep anchor: favorites
 */
public interface FavoritesWagerDao {

    /**
     * Insert a new wager. Returns the generated {@code FAV_WAGER_ID}.
     * {@code record.wagerJson} must be set by the caller before passing in.
     */
    long insert(FavoriteWagerRecord wager);

    /**
     * Find by {@code FAV_WAGER_ID}.
     * Returns {@code null} if absent.
     */
    FavoriteWagerRecord findById(long favWagerId);

    /** All wagers for a player, ordered by {@code FAV_WAGER_ID}. */
    List<FavoriteWagerRecord> findByPlayer(String playerId);

    /** All wagers in a specific group for a player, ordered by {@code FAV_WAGER_ID}. */
    List<FavoriteWagerRecord> findByPlayerAndGroup(String playerId, String groupNumber);

    /** All wagers for a player with a specific game name, ordered by {@code FAV_WAGER_ID}. */
    List<FavoriteWagerRecord> findByPlayerAndGame(String playerId, String gameName);

    /**
     * Update mutable columns: {@code WAGER_NAME}, {@code FLAGS}, {@code WAGER_JSON}.
     * Sets {@code UPDATED_AT = CURRENT_TIMESTAMP}.
     * Returns rows updated (0 if id does not exist).
     */
    int update(FavoriteWagerRecord wager);

    /**
     * Delete by {@code FAV_WAGER_ID}.
     * Returns rows deleted (0 if id does not exist).
     */
    int delete(long favWagerId);

    /** Total wager count for a player. */
    int countByPlayer(String playerId);
}
