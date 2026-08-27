package cz.bsl.czh.favourites.dao;

import java.time.Instant;

/**
 * Row-level domain object mapping {@code GIS_FAV_GROUP}. Not a DTO — carries no Jackson
 * annotations. The service layer (SP5) converts between this and {@link cz.bsl.czh.favourites.api.FavouriteGroupDto}.
 *
 * <p>Grep anchor: favourites
 */
public class FavouriteGroupRecord {

    /** {@code FAV_GROUP_ID} — null before insert, set after. */
    private Long favGroupId;
    private String playerId;
    private String groupNumber;
    private String groupName;
    /** FLAGS bitmask — bit 0 reserved for soft-delete. */
    private int flags;
    /** {@code CREATED_AT} — NOT NULL DEFAULT CURRENT_TIMESTAMP; never null after read. */
    private Instant createdAt;
    /** {@code UPDATED_AT} — NOT NULL DEFAULT CURRENT_TIMESTAMP; never null after read. */
    private Instant updatedAt;

    public FavouriteGroupRecord() {
    }

    public Long getFavGroupId() {
        return favGroupId;
    }

    public void setFavGroupId(Long favGroupId) {
        this.favGroupId = favGroupId;
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

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
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
}
