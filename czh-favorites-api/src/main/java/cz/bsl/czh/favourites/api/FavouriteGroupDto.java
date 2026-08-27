package cz.bsl.czh.favourites.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

/**
 * API DTO for a favourite wager group. Returned by GET and POST group endpoints.
 *
 * <p>Groups are lightweight containers; wager data is stored in {@link FavouriteWagerDto}.
 *
 * <p>Grep anchor: favourites
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FavouriteGroupDto {

    /** Database surrogate key. Null on create; set on read. */
    private Long id;
    private String playerId;
    /** Numeric group slot (1–10). Stored as VARCHAR to match DB column. */
    private String groupNumber;
    /** Display name chosen by the player. Defaults to empty string. */
    private String groupName = "";
    /** Bitmask reserved for future status bits (bit 0 = soft-deleted). */
    private int flags;
    /** Set by the database on insert; null in create requests. */
    private Instant createdAt;
    /** Updated whenever the group name changes. */
    private Instant updatedAt;

    public FavouriteGroupDto() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
