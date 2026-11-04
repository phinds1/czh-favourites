package cz.bsl.czh.favorites.api;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Mirrors front end {@code StakeDTO} (gisorch); not shared by design - keep in sync.
 * Favorites extensions: unknown JSON input properties are ignored; null fields
 * are omitted on output (sparse wire shape, so a legacy body round-trips unchanged).
 *
 * <p>Grep anchor: favorites
 */
@JsonAutoDetect(fieldVisibility = Visibility.ANY, getterVisibility = Visibility.NONE,
        isGetterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE,
        creatorVisibility = Visibility.NONE)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StakeDto {
    /**
     * The id of the stake such as BIG or SMALL
     */
    private String id;
    /**
     * The wagered amount in cents
     */
    private Long stake;
    /**
     * The accepted amount in cents
     */
    private Long acceptedStake;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getStake() {
        return stake;
    }

    public void setStake(Long stake) {
        this.stake = stake;
    }

    public Long getAcceptedStake() {
        return acceptedStake;
    }

    public void setAcceptedStake(Long acceptedStake) {
        this.acceptedStake = acceptedStake;
    }
}
