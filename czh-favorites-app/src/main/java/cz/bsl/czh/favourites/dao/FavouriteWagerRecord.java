package cz.bsl.czh.favourites.dao;

import java.time.Instant;

/**
 * Row-level domain object mapping {@code GIS_FAV_WAGER}. Not a DTO — carries no Jackson
 * annotations. {@code wagerJson} is the raw JSON string stored in {@code WAGER_JSON};
 * the service layer (SP5) owns {@code ObjectMapper} serialisation/deserialisation.
 *
 * <p>Grep anchor: favourites
 */
public class FavouriteWagerRecord {

    /** {@code FAV_WAGER_ID} — null before insert, set after. */
    private Long favWagerId;
    private String playerId;
    private String groupNumber;
    private String gameName;
    private String wagerName;
    /** FLAGS bitmask — bit 0 reserved for soft-delete. */
    private int flags;
    /** {@code CREATED_AT} — NOT NULL DEFAULT CURRENT_TIMESTAMP; never null after read. */
    private Instant createdAt;
    /** {@code UPDATED_AT} — NOT NULL DEFAULT CURRENT_TIMESTAMP; never null after read. */
    private Instant updatedAt;
    /**
     * Raw JSON string stored in {@code WAGER_JSON VARCHAR(30000)}.
     * Callers serialise a {@link cz.bsl.czh.favourites.api.WagerDto} before insert
     * and deserialise after read. The DAO stores and retrieves the string as-is.
     */
    private String wagerJson;

    public FavouriteWagerRecord() {
    }

    public Long getFavWagerId() {
        return favWagerId;
    }

    public void setFavWagerId(Long favWagerId) {
        this.favWagerId = favWagerId;
    }

    public String getPlayerId() {
        return playerId;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    public String getGroupNumber() {
        return groupNumber;
    }

    public void setGroupNumber(String groupNumber) {
        this.groupNumber = groupNumber;
    }

    public String getGameName() {
        return gameName;
    }

    public void setGameName(String gameName) {
        this.gameName = gameName;
    }

    public String getWagerName() {
        return wagerName;
    }

    public void setWagerName(String wagerName) {
        this.wagerName = wagerName;
    }

    public int getFlags() {
        return flags;
    }

    public void setFlags(int flags) {
        this.flags = flags;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getWagerJson() {
        return wagerJson;
    }

    public void setWagerJson(String wagerJson) {
        this.wagerJson = wagerJson;
    }
}
