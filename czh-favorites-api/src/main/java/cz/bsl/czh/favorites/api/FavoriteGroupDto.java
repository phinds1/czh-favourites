package cz.bsl.czh.favorites.api;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * API DTO for a favorite wager group; used by the group endpoints
 * (create/get/update/delete/list).
 *
 * <p>Mirrors front end {@code FavoriteWagerGroupDTO} (gisorch) at wire level:
 * {@code id} is the player-visible group slot number ({@code 1..maxGroupIndex}) as a
 * string - the same value the wager-level {@code group.id} carries - and {@code name}
 * is the display name. The database surrogate key is not exposed.
 *
 * <p>A legacy group create body carries {@code name} only (see
 * {@code src/test/resources/legacy/sportkagroup.json}); the service assigns the next
 * free slot number.
 *
 * <p>Favorites-only additive attributes (never required on input; the front end
 * ignores them): {@code playerId}, {@code flags}, {@code createdAt}, {@code updatedAt}.
 *
 * <p>Grep anchor: favorites
 */
@JsonAutoDetect(fieldVisibility = Visibility.ANY, getterVisibility = Visibility.NONE,
        isGetterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE,
        creatorVisibility = Visibility.NONE)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FavoriteGroupDto {

    /** Player-visible group id (the group slot number 1..maxGroupIndex, as a string). Null on create. */
    private String id;
    /** Display name of the group (legacy field name; defaults to empty string). */
    private String name = "";

    // --- favorites-only additive attributes (optional) ---

    /** Owning player; set from the player context header, never taken from input. */
    private String playerId;
    /** Bitmask reserved for future status bits (bit 0 = soft-deleted). */
    private int flags;
    /** Set by the database on insert; null in create requests. */
    private Instant createdAt;
    /** Set by the database on update; null in create requests. */
    private Instant updatedAt;

    public FavoriteGroupDto() {
    }

    public FavoriteGroupDto(String id, String name) {
        this.id = id;
        this.name = name;
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

    public String getPlayerId() {
        return playerId;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
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
