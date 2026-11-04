package cz.bsl.czh.favorites.api;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Represents a prize for a draw
 *
 * <p>Mirrors front end {@code PrizeDTO} (gisorch); not shared by design - keep in sync.
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
public class PrizeDto {
    /**
     * Name of the game the ticket that win a prize for
     */
    private String gameName;
    /**
     * Brand name associated with the game name
     */
    private String brandName;
    /**
     * Draw id ticket that win for
     */
    private String drawId;
    /**
     * Date of the draw
     */
    private Long drawTime;
    /**
     * Name of the result set the ticket that win for
     */
    private String resultSet;
    /**
     * Name of the division the ticket that win for
     */
    private String prizeTierName;
    /**
     * Id of the division the ticket that win for
     */
    private String prizeTierId;
    /**
     * Number of shares the ticket that in within the division
     */
    private Integer shares;
    /**
     * Amount the ticket that win within the division
     */
    private Long amount;
    /**
     * The status of the prize
     */
    private PrizeStatus status;
    /**
     * Type of prize the ticket won
     */
    private PrizeType type;

    /**
     * Merchandise prize description
     */
    private String merchandiseDescription;

    public String getGameName() {
        return gameName;
    }

    public void setGameName(String gameName) {
        this.gameName = gameName;
    }

    public String getBrandName() {
        return brandName;
    }

    public void setBrandName(String brandName) {
        this.brandName = brandName;
    }

    public String getDrawId() {
        return drawId;
    }

    public void setDrawId(String drawId) {
        this.drawId = drawId;
    }

    public Long getDrawTime() {
        return drawTime;
    }

    public void setDrawTime(Long drawTime) {
        this.drawTime = drawTime;
    }

    public String getResultSet() {
        return resultSet;
    }

    public void setResultSet(String resultSet) {
        this.resultSet = resultSet;
    }

    public String getPrizeTierName() {
        return prizeTierName;
    }

    public void setPrizeTierName(String prizeTierName) {
        this.prizeTierName = prizeTierName;
    }

    public String getPrizeTierId() {
        return prizeTierId;
    }

    public void setPrizeTierId(String prizeTierId) {
        this.prizeTierId = prizeTierId;
    }

    public Integer getShares() {
        return shares;
    }

    public void setShares(Integer shares) {
        this.shares = shares;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public PrizeStatus getStatus() {
        return status;
    }

    public void setStatus(PrizeStatus status) {
        this.status = status;
    }

    public PrizeType getType() {
        return type;
    }

    public void setType(PrizeType type) {
        this.type = type;
    }

    public String getMerchandiseDescription() {
        return merchandiseDescription;
    }

    public void setMerchandiseDescription(String merchandiseDescription) {
        this.merchandiseDescription = merchandiseDescription;
    }

}
