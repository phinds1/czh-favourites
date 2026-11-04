package cz.bsl.czh.favorites.api;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * Represents an active promotion for a given game.  It is assume the data for an inactive promotion will not be provided.
 *
 * <p>Mirrors front end {@code PromotionDTO} (gisorch); not shared by design - keep in sync.
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
public class PromotionDto {
    /**
     * Promotion name
     */
    private String name;
    /**
     * Text description of the promotion
     */
    private String description;
    /**
     * Timestamp of when the promotion will start
     */
    private Long startTime;
    /**
     * Timestamp of when the promotion will end
     */
    private Long endTime;
    /**
     * The type of promotion such as voucher or coupon
     */
    private String type;
    /**
     * The prize type of the promotion such as cash or discount
     */
    private String prizeType;
    /**
     * The voucher or coupon number
     */
    private String id;
    /**
     * The amount discounted from the purchase price in cents
     */
    private Long discountAmount;
    /**
     * The cash prize amount in cents
     */
    private Long cashPrizeAmount;
    /**
     * The promotion serial number
     */
    private String serialNumber;
    /**
     * The promotion draw number
     */
    private String drawId;
    /**
     * The promotion draw date
     */
    private String drawDate;
    /**
     * The creation time
     */
    private String transactionTime;
    /**
     * List of text regions to print
     */
    private List<TextDto> text;
    /**
     * List of wagers and/or free tickets attached to the promotion
     */
    private List<WagerDto> wagers;
    /**
     * The promotion state(active,inactive)
     */
    private String state;

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }



    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getStartTime() {
        return startTime;
    }

    public void setStartTime(Long startTime) {
        this.startTime = startTime;
    }

    public Long getEndTime() {
        return endTime;
    }

    public void setEndTime(Long endTime) {
        this.endTime = endTime;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPrizeType() {
        return prizeType;
    }

    public void setPrizeType(String prizeType) {
        this.prizeType = prizeType;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(Long discountAmount) {
        this.discountAmount = discountAmount;
    }

    public Long getCashPrizeAmount() {
        return cashPrizeAmount;
    }

    public void setCashPrizeAmount(Long cashPrizeAmount) {
        this.cashPrizeAmount = cashPrizeAmount;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getDrawId() {
        return drawId;
    }

    public void setDrawId(String drawId) {
        this.drawId = drawId;
    }

    public String getDrawDate() {
        return drawDate;
    }

    public void setDrawDate(String drawDate) {
        this.drawDate = drawDate;
    }

    public String getTransactionTime() {
        return transactionTime;
    }

    public void setTransactionTime(String transactionTime) {
        this.transactionTime = transactionTime;
    }

    public List<TextDto> getText() {
        return text;
    }

    public void setText(List<TextDto> text) {
        this.text = text;
    }

    public List<WagerDto> getWagers() {
        return wagers;
    }

    public void setWagers(List<WagerDto> wagers) {
        this.wagers = wagers;
    }

}
