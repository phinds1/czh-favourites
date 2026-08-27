package cz.bsl.czh.favourites.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

/**
 * API DTO for a favourite wager. Returned by GET and POST wager endpoints.
 *
 * <p>The {@code wager} sub-object is serialised as {@code WAGER_JSON} in the database;
 * all other fields map to promoted columns in {@code GIS_FAV_WAGER}.
 *
 * <p>Validation (non-blank gameName, groupNumber bounds, etc.) is enforced by the
 * service layer (SP5) — not here.
 *
 * <p>Grep anchor: favourites
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FavouriteWagerDto {

    /** Database surrogate key. Null on create; set on read. */
    private Long id;
    private String playerId;
    private String groupNumber;
    private String gameName;
    /** Display name chosen by the player. Defaults to empty string. */
    private String wagerName = "";
    /** Bitmask reserved for future status bits (bit 0 = soft-deleted). */
    private int flags;
    /** Set by the database on insert; null in create requests. */
    private Instant createdAt;
    /** The wager sub-object stored as WAGER_JSON. */
    private WagerDto wager;

    public FavouriteWagerDto() {
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

    public WagerDto getWager() {
        return wager;
    }

    public void setWager(WagerDto wager) {
        this.wager = wager;
    }
}
