package cz.bsl.czh.favorites.api;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * The board can contain single or multiple selections
 *
 * <p>Mirrors front end {@code BoardDTO} (gisorch); not shared by design - keep in sync.
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
public class BoardDto {
    /**
     * Type or modality of the bet
     */
    private String systemBetType;
    /**
     * Type of the bet
     */
    private String betType;
    /**
     * Indicates whether a board is favorite board or not
     */
    private Boolean favorite;
    /**
     * Specifying favorite board id
     */
    private String favoriteId;
    /**
     * If the board is or will be generated using random numbers provided by the system
     */
    private Boolean quickpick;
    /**
     * If the board will be played with multiplier
     */
    private Boolean multiplier;
    /**
     * The primary numbers of the board informed only when a single selection is made; It is a list of marks
     */
    private List<String> primarySelections;
    /**
     * The secondary numbers of the board informed only when a single selection is made; It is a list of marks
     */
    private List<String> secondarySelections;
    /**
     * The tertiary numbers of the board informed only when a single selection is made; It is a list of marks
     */
    private List<String> tertiarySelections;
    /**
     * The primary numbers of the board informed only when a multiple selection is made; It is a list of selections which each selections is a list of marks
     */
    private List<List<String>> primaryMultipleSelections;
    /**
     * The secondary numbers of the board informed only when a multiple selection is made; It is a list of selections which each selections is a list of marks
     */
    private List<List<String>> secondaryMultipleSelections;
    /**
     * The tertiary numbers of the board informed only when a multiple selection is made; It is a list of selections which each selections is a list of marks
     */
    private List<List<String>> tertiaryMultipleSelections;
    /**
     * Indicates that some or all of the primary selections were quick-picked
     * for the player.
     */
    private Boolean primarySelectionsQuickpicked;
    /**
     * Indicates that some or all of the secondary selections were quick-picked
     * for the player.
     */
    private Boolean secondarySelectionsQuickpicked;
    /**
     * Indicates that some or all of the tertiary selections were quick-picked
     * for the player.
     */
    private Boolean tertiarySelectionsQuickpicked;
    /**
     * Stake or wagered amount for this board.
     */
    private Long stake;
    /**
     * The numbers selected.
     */
    private List<String> selections;
    /**
     * List of stakes for games that allow mor than one stake per board
     */
    private List<StakeDto> stakes;
    /**
     * If true, the board was sold out.
     */
    private Boolean soldOut;
    /**
     * Indicates the included addon wagers are played when set.
     */
    private Boolean addonPlayed;
    /**
     * List of draw names the board is for.
     */
    private List<String> drawNames;
    /**
     * List of taxes charged for the board.
     */
    private List<TaxDto> taxes;
    /**
     * Number of selections quick-picked.
     */
    private Integer quickPickCount;
    /**
     * Number of secondary selections quick-picked.
     */
    private Integer secondaryQuickPickCount;
    /**
     * Number of tertiary selections quick-picked.
     */
    private Integer tertiaryQuickPickCount;
    /**
     * Owner of the board.
     */
    private String groupMemberId;
    /**
     * Potential prize amount for the board; returned by the transaction engine when the wager is placed.  Used by the
     * Cash Pop game.
     */
    private Long potentialPrizeAmount;

    public Boolean isFavorite() {
        return favorite;
    }

    public void setFavorite(Boolean favorite) {
        this.favorite = favorite;
    }

    public String getFavoriteId() {
        return favoriteId;
    }

    public void setFavoriteId(String favoriteId) {
        this.favoriteId = favoriteId;
    }

    public Boolean isQuickpick() {
        return quickpick;
    }

    public void setQuickpick(Boolean quickpick) {
        this.quickpick = quickpick;
    }

    public Boolean isMultiplier() {
        return multiplier;
    }

    public void setMultiplier(Boolean multiplier) {
        this.multiplier = multiplier;
    }

    public String getSystemBetType() {
        return systemBetType;
    }

    public void setSystemBetType(String systemBetType) {
        this.systemBetType = systemBetType;
    }

    public String getBetType() {
        return betType;
    }

    public void setBetType(String betType) {
        this.betType = betType;
    }

    public List<String> getPrimarySelections() {
        return primarySelections;
    }

    public void setPrimarySelections(List<String> primarySelections) {
        this.primarySelections = primarySelections;
    }

    public List<String> getSecondarySelections() {
        return secondarySelections;
    }

    public void setSecondarySelections(List<String> secondarySelections) {
        this.secondarySelections = secondarySelections;
    }

    public List<List<String>> getPrimaryMultipleSelections() {
        return primaryMultipleSelections;
    }

    public void setPrimaryMultipleSelections(List<List<String>> primaryMultipleSelections) {
        this.primaryMultipleSelections = primaryMultipleSelections;
    }

    public List<List<String>> getSecondaryMultipleSelections() {
        return secondaryMultipleSelections;
    }

    public void setSecondaryMultipleSelections(List<List<String>> secondaryMultipleSelections) {
        this.secondaryMultipleSelections = secondaryMultipleSelections;
    }

    public List<List<String>> getTertiaryMultipleSelections() {
        return tertiaryMultipleSelections;
    }

    public void setTertiaryMultipleSelections(List<List<String>> tertiaryMultipleSelections) {
        this.tertiaryMultipleSelections = tertiaryMultipleSelections;
    }

    public Boolean getPrimarySelectionsQuickpicked() {
        return primarySelectionsQuickpicked;
    }

    public void setPrimarySelectionsQuickpicked(Boolean primarySelectionsQuickpicked) {
        this.primarySelectionsQuickpicked = primarySelectionsQuickpicked;
    }

    public Boolean getSecondarySelectionsQuickpicked() {
        return secondarySelectionsQuickpicked;
    }

    public void setSecondarySelectionsQuickpicked(
            Boolean secondarySelectionsQuickpicked) {
        this.secondarySelectionsQuickpicked = secondarySelectionsQuickpicked;
    }

    public Long getStake() {
        return stake;
    }

    public void setStake(Long stake) {
        this.stake = stake;
    }

    public List<String> getSelections() {
        return selections;
    }

    public void setSelections(List<String> selections) {
        this.selections = selections;
    }

    public List<StakeDto> getStakes() {
        return stakes;
    }

    public void setStakes(List<StakeDto> stakes) {
        this.stakes = stakes;
    }

    public Boolean getSoldOut() {
        return soldOut;
    }

    public void setSoldOut(Boolean soldOut) {
        this.soldOut = soldOut;
    }

    public Boolean getAddonPlayed() {
        return addonPlayed;
    }

    public void setAddonPlayed(Boolean addonPlayed) {
        this.addonPlayed = addonPlayed;
    }

    public List<String> getDrawNames() {
        return drawNames;
    }

    public void setDrawNames(List<String> drawNames) {
        this.drawNames = drawNames;
    }

    public List<TaxDto> getTaxes() {
        return taxes;
    }

    public void setTaxes(List<TaxDto> taxes) {
        this.taxes = taxes;
    }

    public Integer getQuickPickCount() {
        return quickPickCount;
    }

    public void setQuickPickCount(Integer quickPickCount) {
        this.quickPickCount = quickPickCount;
    }

    public Integer getSecondaryQuickPickCount() {
        return secondaryQuickPickCount;
    }

    public void setSecondaryQuickPickCount(Integer secondaryQuickPickCount) {
        this.secondaryQuickPickCount = secondaryQuickPickCount;
    }

    public String getGroupMemberId() {
        return groupMemberId;
    }

    public void setGroupMemberId(String groupMemberId) {
        this.groupMemberId = groupMemberId;
    }

    public List<String> getTertiarySelections() {
        return tertiarySelections;
    }

    public void setTertiarySelections(List<String> tertiarySelections) {
        this.tertiarySelections = tertiarySelections;
    }

    public Boolean getTertiarySelectionsQuickpicked() {
        return tertiarySelectionsQuickpicked;
    }

    public void setTertiarySelectionsQuickpicked(Boolean tertiarySelectionsQuickpicked) {
        this.tertiarySelectionsQuickpicked = tertiarySelectionsQuickpicked;
    }

    public Integer getTertiaryQuickPickCount() {
        return tertiaryQuickPickCount;
    }

    public void setTertiaryQuickPickCount(Integer tertiaryQuickPickCount) {
        this.tertiaryQuickPickCount = tertiaryQuickPickCount;
    }

    public Long getPotentialPrizeAmount() {
        return potentialPrizeAmount;
    }

    public void setPotentialPrizeAmount(Long potentialPrizeAmount) {
        this.potentialPrizeAmount = potentialPrizeAmount;
    }
}
