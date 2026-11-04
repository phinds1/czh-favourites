package cz.bsl.czh.favorites.api;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * API DTO for a favorite wager; returned by GET and POST wager endpoints.
 *
 * <p>Mirrors front end {@code FavoriteWagerDTO} (gisorch): {@code id} (string), {@code name},
 * {@code group} ({@link FavoriteWagerGroupDto}) and the {@code wager} sub-object. The wager
 * is serialized as {@code WAGER_JSON} in the database; the other fields map to promoted
 * columns in {@code GIS_FAV_WAGER}.
 *
 * <p>Favorites-only additive attributes (never required on input, informational on output;
 * the front end ignores them): {@code playerId}, {@code gameName} (derived from
 * {@code wager.gameName}), {@code flags}, {@code createdAt}.
 *
 * <p>Validation (non-blank name/gameName, board selections, size limits) is enforced by the
 * service layer (SP5) - not here.
 *
 * <p>Grep anchor: favorites
 */
@JsonAutoDetect(fieldVisibility = Visibility.ANY, getterVisibility = Visibility.NONE,
        isGetterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE,
        creatorVisibility = Visibility.NONE)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FavoriteWagerDto {

    /** Player-visible favorite id (the database surrogate key, as a string). Null on create; set on read. */
    private String id;
    /** Display name chosen by the player (legacy field name; defaults to empty string). */
    private String name = "";
    /** Group the favorite is assigned to ({@code id} = group slot number, {@code name}). */
    private FavoriteWagerGroupDto group;
    /** The wager sub-object stored as WAGER_JSON. */
    private WagerDto wager;

    // --- favorites-only additive attributes (optional) ---

    /** Owning player; set from the player context header, never taken from input. */
    private String playerId;
    /** Promoted column derived from {@code wager.gameName}; optional in responses, ignored on input. */
    private String gameName;
    /** Bitmask reserved for future status bits (bit 0 = soft-deleted). */
    private int flags;
    /** Set by the database on insert; null in create requests. */
    private Instant createdAt;

    public FavoriteWagerDto() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public FavoriteWagerGroupDto getGroup() {
        return group;
    }

    public void setGroup(FavoriteWagerGroupDto group) {
        this.group = group;
    }

    public WagerDto getWager() {
        return wager;
    }

    public void setWager(WagerDto wager) {
        this.wager = wager;
    }

    public String getPlayerId() {
        return playerId;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    public String getGameName() {
        return gameName;
    }

    public void setGameName(String gameName) {
        this.gameName = gameName;
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
}
