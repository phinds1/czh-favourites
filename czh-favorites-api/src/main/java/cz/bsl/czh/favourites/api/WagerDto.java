package cz.bsl.czh.favourites.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Compact wager sub-object stored inside {@code FavouriteWagerDto} and serialised as
 * {@code WAGER_JSON} in the database. Carries only the fields required to replay a
 * favourite wager; the full {@code WagerDTO} lifecycle fields (status, rejectReason, etc.)
 * are intentionally excluded.
 *
 * <p>{@code boards} is a list of board stacks; each board stack is a list of integer picks.
 * Depth and pick values are not validated here — input validation is the service layer's
 * responsibility (SP5).
 *
 * <p>Grep anchor: favourites
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class WagerDto {

    private String gameName;
    private long stake;
    private long price;
    private int duration;
    private String serialNumber;
    /** Board stacks: outer list = stacks, inner list = pick integers for that board. */
    private List<List<Integer>> boards;

    public WagerDto() {
    }

    public WagerDto(String gameName, long stake, long price, int duration,
                    String serialNumber, List<List<Integer>> boards) {
        this.gameName = gameName;
        this.stake = stake;
        this.price = price;
        this.duration = duration;
        this.serialNumber = serialNumber;
        this.boards = boards;
    }

    public String getGameName() {
        return gameName;
    }

    public void setGameName(String gameName) {
        this.gameName = gameName;
    }

    public long getStake() {
        return stake;
    }

    public void setStake(long stake) {
        this.stake = stake;
    }

    public long getPrice() {
        return price;
    }

    public void setPrice(long price) {
        this.price = price;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public List<List<Integer>> getBoards() {
        return boards;
    }

    public void setBoards(List<List<Integer>> boards) {
        this.boards = boards;
    }
}
