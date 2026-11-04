package cz.bsl.czh.favorites.api;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * The group a favorite wager is assigned to: {@code id} + {@code name}.
 *
 * <p>Mirrors front end {@code FavoriteWagerGroupDTO} (gisorch); not shared by design - keep in sync.
 *
 * <p>The {@code id} is the player-visible group slot number ({@code 1..maxGroupIndex}),
 * not the database surrogate key. A legacy create body supplies the group by
 * {@code name} only; the service resolves or creates the group (favorites behaviour).
 *
 * <p>Grep anchor: favorites
 */
@JsonAutoDetect(fieldVisibility = Visibility.ANY, getterVisibility = Visibility.NONE,
        isGetterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE,
        creatorVisibility = Visibility.NONE)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FavoriteWagerGroupDto {

    /** The group id. The group id does not change if the group name is changed. */
    private String id;

    /** The group name. */
    private String name;

    public FavoriteWagerGroupDto() {
    }

    public FavoriteWagerGroupDto(String id, String name) {
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
}
