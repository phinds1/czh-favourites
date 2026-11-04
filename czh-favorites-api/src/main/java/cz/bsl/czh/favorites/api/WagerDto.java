package cz.bsl.czh.favorites.api;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * Represents a wager for a draw
 *
 * <p>Mirrors front end {@code WagerDTO} (gisorch); not shared by design - keep in sync.
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
public class WagerDto {
    /**
     * Status of the wager
     */
    private String status;
    /**
     * Reason wager was rejected.
     */
    private String rejectReason;

    /**
     * Reason for wager to be rejected
     */
    private ErrorDto error;

    /**
     * Wager type
     */
    private WagerType type;

    /**
     * Wager purchase channel
     */
    private String channel;

    /**
     * List of drawIds for non-consecutive multi-draw games. If drawIds are supplied, drawId and duration properties must not be sent
     */
    private List<String> drawIds;

    /**
     * Starting drawId for a consecutive multi-draw wager. If supplied, drawIds cannot be set. If supplied, duration has to be set
     */
    private String drawId;

    /**
     * Number of draws from drawNumber in which the wager will be participating. If supplied, drawNumber must be set and drawNumbers cannot be given
     */
    private Integer duration;

    /**
     * Start wagering by skipping certain number of draws starting from current draw
     */
    private Integer delay;

    /**
     * Draw names
     */
    private List<String> drawNames;

    /**
     * Name of the game for which the wager will be placed
     */
    private String gameName;

    /**
     * List of draw add-on wagers
     */
    private List<WagerDto> addonWagers;

    /**
     * Wager price in cents
     */
    private Long price;

    /**
     * Internal wager id. Reference implementation uses this field for ESTE's serialNumber.
     */
    private String id;

    /**
     * Externally provided wager id.
     */
    private String requestId;

    /**
     * Wager serial number. Reference implementation uses this field for GMS's serialNumber, ESTE's exttxid.
     */
    private String serialNumber;

    /**
     * Board data for the wager
     */
    private List<BoardDto> boards;

    /**
     * Creation time
     */
    private Long transactionTime;

    private Boolean additionalPrizeTier;

    /**
     * Selected stake
     */
    private Long stake;

    /**
     * The wager multiplier
     */
    private Long multiplier;

    /**
     * Predefined game rule set id e.g. in Keno
     */
    private String predefinedGameRuleSetId;

    /**
     * System level used in games with variable selection
     */
    private Integer systemLevel;

    /**
     * Subscription id
     */
    private String subscriptionId;

    /**
     * Syndicate id
     */
    private String syndicateId;

    /**
     * Syndicate instance id
     */
    private String syndicateInstanceId;

    /**
     * Total syndicate parts
     */
    private Integer syndicateTotalParts;

    /**
     * Number of syndicate parts per wager
     */
    private Integer syndicatePartsPerWager;

    /**
     * Group play member id
     */
    private String groupMemberId;

    /**
     * Group play id
     */
    private String groupId;

    /**
     * Indicates whether a wager is favorite wager or not
     */
    private Boolean favorite;

    /**
     * Specifying favorite wager id
     */
    private String favoriteId;

    /**
     * A hash or checksum of selected fields in the wager request
     */
    private String digest;

    /**
     * Payment source to use to pay for wager
     */
    private String paymentSourceId;

    /**
     * Amount to transfer from payment source to gaming account
     * to pay for wager
     */
    private Long paymentAmount;

    /**
     * Id of already completed funds transfer from payment source
     * to gaming account that should be reversed if wager fails
     */
    private String paymentTransactionId;

    /**
     * Signature of the wager returned by the back-end system
     */
    private String signature;

    /**
     * Staring draw date in which the wager will be participating in milliseconds since epoch.
     */
    private Long drawStartDate;

    /**
     * Ending draw date in which the wager will be participating in milliseconds since epoch.
     */
    private Long drawEndDate;
    /**
     * The current revision of the game rules.
     */
    private String revision;
    /**
     * If present in a response, the free ticket field contains the
     * request to send to get the free ticket.
     */
    private WagerDto freeTicket;
    /**
     * If present in a response, the list contains requests to send
     * to get the free tickets.
     */
    private List<WagerDto> freeTickets;
    /**
     * The ticket security information.
     */
    private TicketSecurityDto ticketSecurity;
    /**
     * Indicates the wager was done offline and the ticket has already
     * been printed and the requestId field contains the foreign serial
     * number printed on the ticket.
     */
    private Boolean alreadyPrinted;
    /**
     * Indicates the included addon wagers are played when set.
     */
    private Boolean addonPlayed;
    /**
     * Indicates wager request is for a free ticket.
     */
    private Boolean freeTicketRequest;
    /**
     * If wager request is a promotional free ticket, the promotion
     * number for the free ticket.
     */
    private String promotionId;
    /**
     * Payment type (e.g: real money, virtual money, points)
     */
    private String paymentType;

    /**
     * List of prizes won instantly
     */
    private List<PrizeDto> prizes;

    /**
     * List of bonus entries (e.g. Millionaire Club Numbers)
     */
    private List<String> bonusEntries;

    /**
     * Unique entry web code for seconds change games
     */
    private String webCode;
    /**
     * List of taxes charged for the wager.
     */
    private List<TaxDto> taxes;
    /**
     * The amount discounted from the purchase price in cents of this wager
     */
    private Long discountAmount;
    /**
     * The promotion this wager qualifies for
     */
    private PromotionDto promotion;
    /**
     * Id to match this wager with other transactions
     */
    private String correlationId;
    /**
     * List of text regions to print
     */
    private List<TextDto> ticketMessage;
    /**
     * Location wager was placed
     */
    private AddressDto wageredAt;
    /**
     * Preferred method of paying winnings such as PLAYER_ACCOUNT, BANK_ACCOUNT, or CASH.
     */
    private String redemptionMethod;
    /**
     * An extra game, such as Double Take, is included with this wager
     */
    private Boolean extraPlayed;
    /**
     * Grid wagered
     */
    private String gridId;
    /**
     * Number of fractions wagered or for a fractional wager the total number of fractions
     * the wager is to be broken into.
     */
    private Long fractions;
    /**
     * Entry date for the results of the last draw the wager is eligible to participate in.
     */
    private Long resultDate;
    /**
     * Ticket entry code.  Used to identify the online wager portion of a hybrid game.
     */
    private String ticketEntryCode;
    /**
     * List of wager serial numbers for each fraction.
     */
    private List<String> fractionSerialNumbers;
    /**
     * Trigger type which caused the wager to be generated. A player can create a trigger based on some event,
     * like a high jackpot amount, that will cause a wager to automatically be placed by a player system.
     */
    private String triggerType;
    /**
     * Information about player scanned in from an id document such as a passport or driver's license.
     */
    private IdDocumentDto idDocument;
    /**
     * If true, indicates wager is a prolongation of a previous wager.
     */
    private Boolean prolongation;
    /**
     * The id of bet slip used to placed the wager.
     */
    private String betSlipId;

    /**
     * Neo Id
     */
    private Integer bonusPolicyId;

    /**
     * Main discount amount in cents.
     */
    private Long mainDiscountAmount;

    /**
     * Addon discount amount in cents.
     */
    private Long addonDiscountAmount;

    /**
     * Bonus discount amount in cents.
     */
    private Long bonusDiscountAmount;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public ErrorDto getError() {
        return error;
    }

    public void setError(ErrorDto error) {
        this.error = error;
    }

    public WagerType getType() {
        return type;
    }

    public void setType(WagerType type) {
        this.type = type;
    }

    public List<String> getDrawIds() {
        return drawIds;
    }

    public void setDrawIds(List<String> drawIds) {
        this.drawIds = drawIds;
    }

    public String getDrawId() {
        return drawId;
    }

    public void setDrawId(String drawId) {
        this.drawId = drawId;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public Integer getDelay() {
        return delay;
    }

    public void setDelay(Integer delay) {
        this.delay = delay;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getGameName() {
        return gameName;
    }

    public void setGameName(String gameName) {
        this.gameName = gameName;
    }

    public List<WagerDto> getAddonWagers() {
        return addonWagers;
    }

    public void setAddonWagers(List<WagerDto> addonWagers) {
        this.addonWagers = addonWagers;
    }

    public Long getPrice() {
        return price;
    }

    public void setPrice(Long price) {
        this.price = price;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public List<BoardDto> getBoards() {
        return boards;
    }

    public void setBoards(List<BoardDto> boards) {
        this.boards = boards;
    }

    public Integer getSystemLevel() {
        return systemLevel;
    }

    public void setSystemLevel(Integer systemLevel) {
        this.systemLevel = systemLevel;
    }

    public List<String> getDrawNames() {
        return drawNames;
    }

    public void setDrawNames(List<String> drawNames) {
        this.drawNames = drawNames;
    }

    public String getPredefinedGameRuleSetId() {
        return predefinedGameRuleSetId;
    }

    public void setPredefinedGameRuleSetId(String predefinedGameRuleSetId) {
        this.predefinedGameRuleSetId = predefinedGameRuleSetId;
    }

    public Long getTransactionTime() {
        return transactionTime;
    }

    public void setTransactionTime(Long transactionTime) {
        this.transactionTime = transactionTime;
    }

    public Boolean getAdditionalPrizeTier() {
        return additionalPrizeTier;
    }

    public void setAdditionalPrizeTier(Boolean additionalPrizeTier) {
        this.additionalPrizeTier = additionalPrizeTier;
    }

    public Long getStake() {
        return stake;
    }

    public void setStake(Long stake) {
        this.stake = stake;
    }

    public Long getMultiplier() {
        return multiplier;
    }

    public void setMultiplier(Long multiplier) {
        this.multiplier = multiplier;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public String getSyndicateId() {
        return syndicateId;
    }

    public void setSyndicateId(String syndicateId) {
        this.syndicateId = syndicateId;
    }

    public String getGroupMemberId() {
        return groupMemberId;
    }

    public void setGroupMemberId(String groupMemberId) {
        this.groupMemberId = groupMemberId;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getSyndicateInstanceId() {
        return syndicateInstanceId;
    }

    public void setSyndicateInstanceId(String syndicateInstanceId) {
        this.syndicateInstanceId = syndicateInstanceId;
    }

    public Integer getSyndicateTotalParts() {
        return syndicateTotalParts;
    }

    public void setSyndicateTotalParts(Integer syndicateTotalParts) {
        this.syndicateTotalParts = syndicateTotalParts;
    }

    public Integer getSyndicatePartsPerWager() {
        return syndicatePartsPerWager;
    }

    public void setSyndicatePartsPerWager(Integer syndicatePartsPerWager) {
        this.syndicatePartsPerWager = syndicatePartsPerWager;
    }

    public Boolean isFavorite() {
        return favorite;
    }

    public void setFavorite(Boolean isFavorite) {
        this.favorite = isFavorite;
    }

    public String getFavoriteId() {
        return favoriteId;
    }

    public void setFavoriteId(String favoriteId) {
        this.favoriteId = favoriteId;
    }

    public String getDigest() {
        return digest;
    }

    public void setDigest(String digest) {
        this.digest = digest;
    }

    public String getPaymentSourceId() {
        return paymentSourceId;
    }

    public void setPaymentSourceId(String paymentSourceId) {
        this.paymentSourceId = paymentSourceId;
    }

    public Long getPaymentAmount() {
        return paymentAmount;
    }

    public void setPaymentAmount(Long paymentAmount) {
        this.paymentAmount = paymentAmount;
    }

    public String getPaymentTransactionId() {
        return paymentTransactionId;
    }

    public void setPaymentTransactionId(String paymentTransactionId) {
        this.paymentTransactionId = paymentTransactionId;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public Long getDrawStartDate() {
        return drawStartDate;
    }

    public void setDrawStartDate(Long drawStartDate) {
        this.drawStartDate = drawStartDate;
    }

    public Long getDrawEndDate() {
        return drawEndDate;
    }

    public void setDrawEndDate(Long drawEndDate) {
        this.drawEndDate = drawEndDate;
    }

    public String getRevision() {
        return revision;
    }

    public void setRevision(String revision) {
        this.revision = revision;
    }

    public WagerDto getFreeTicket() {
        return freeTicket;
    }

    public void setFreeTicket(WagerDto freeTicket) {
        this.freeTicket = freeTicket;
    }

    public List<WagerDto> getFreeTickets() {
        return freeTickets;
    }

    public void setFreeTickets(List<WagerDto> freeTickets) {
        this.freeTickets = freeTickets;
    }

    public TicketSecurityDto getTicketSecurity() {
        return ticketSecurity;
    }

    public void setTicketSecurity(TicketSecurityDto ticketSecurity) {
        this.ticketSecurity = ticketSecurity;
    }

    public Boolean getAlreadyPrinted() {
        return alreadyPrinted;
    }

    public void setAlreadyPrinted(Boolean alreadyPrinted) {
        this.alreadyPrinted = alreadyPrinted;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }

    public Boolean getAddonPlayed() {
        return addonPlayed;
    }

    public void setAddonPlayed(Boolean addonPlayed) {
        this.addonPlayed = addonPlayed;
    }

    public Boolean getFreeTicketRequest() {
        return freeTicketRequest;
    }

    public void setFreeTicketRequest(Boolean freeTicketRequest) {
        this.freeTicketRequest = freeTicketRequest;
    }

    public String getPromotionId() {
        return promotionId;
    }

    public void setPromotionId(String promotionId) {
        this.promotionId = promotionId;
    }

    public String getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(String paymentType) {
        this.paymentType = paymentType;
    }

    public List<PrizeDto> getPrizes() {
        return prizes;
    }

    public void setPrizes(List<PrizeDto> prizes) {
        this.prizes = prizes;
    }

    public List<String> getBonusEntries() {
        return bonusEntries;
    }

    public void setBonusEntries(List<String> bonusEntries) {
        this.bonusEntries = bonusEntries;
    }

    public String getWebCode() {
        return webCode;
    }

    public void setWebCode(String webCode) {
        this.webCode = webCode;
    }

    public List<TaxDto> getTaxes() {
        return taxes;
    }

    public void setTaxes(List<TaxDto> taxes) {
        this.taxes = taxes;
    }

    public Long getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(Long discountAmount) {
        this.discountAmount = discountAmount;
    }

    public PromotionDto getPromotion() {
        return promotion;
    }

    public void setPromotion(PromotionDto promotion) {
        this.promotion = promotion;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public List<TextDto> getTicketMessage() {
        return ticketMessage;
    }

    public void setTicketMessage(List<TextDto> ticketMessage) {
        this.ticketMessage = ticketMessage;
    }

    public AddressDto getWageredAt() {
        return wageredAt;
    }

    public void setWageredAt(AddressDto wageredAt) {
        this.wageredAt = wageredAt;
    }

    public String getRedemptionMethod() {
        return redemptionMethod;
    }

    public void setRedemptionMethod(String redemptionMethod) {
        this.redemptionMethod = redemptionMethod;
    }

    public Boolean getExtraPlayed() {
        return extraPlayed;
    }

    public void setExtraPlayed(Boolean extraPlayed) {
        this.extraPlayed = extraPlayed;
    }

    public String getGridId() {
        return gridId;
    }

    public void setGridId(String gridId) {
        this.gridId = gridId;
    }

    public Long getFractions() {
        return fractions;
    }

    public void setFractions(Long fractions) {
        this.fractions = fractions;
    }

    public Long getResultDate() {
        return resultDate;
    }

    public void setResultDate(Long resultDate) {
        this.resultDate = resultDate;
    }

    public String getTicketEntryCode() {
        return ticketEntryCode;
    }

    public void setTicketEntryCode(String ticketEntryCode) {
        this.ticketEntryCode = ticketEntryCode;
    }

    public List<String> getFractionSerialNumbers() {
        return fractionSerialNumbers;
    }

    public void setFractionSerialNumbers(List<String> fractionSerialNumbers) {
        this.fractionSerialNumbers = fractionSerialNumbers;
    }

    public String getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(String triggerType) {
        this.triggerType = triggerType;
    }

    public IdDocumentDto getIdDocument() {
        return idDocument;
    }

    public void setIdDocument(IdDocumentDto idDocument) {
        this.idDocument = idDocument;
    }

    public Boolean getProlongation() {
        return prolongation;
    }

    public void setProlongation(Boolean prolongation) {
        this.prolongation = prolongation;
    }

    public String getBetSlipId() {
        return betSlipId;
    }

    public void setBetSlipId(String betSlipId) {
        this.betSlipId = betSlipId;
    }

    public Integer getBonusPolicyId() {
        return bonusPolicyId;
    }

    public void setBonusPolicyId(Integer bonusPolicyId) {
        this.bonusPolicyId = bonusPolicyId;
    }

    public Long getMainDiscountAmount() {
        return mainDiscountAmount;
    }

    public void setMainDiscountAmount(Long mainDiscountAmount) {
        this.mainDiscountAmount = mainDiscountAmount;
    }

    public Long getAddonDiscountAmount() {
        return addonDiscountAmount;
    }

    public void setAddonDiscountAmount(Long addonDiscountAmount) {
        this.addonDiscountAmount = addonDiscountAmount;
    }

    public Long getBonusDiscountAmount() {
        return bonusDiscountAmount;
    }

    public void setBonusDiscountAmount(Long bonusDiscountAmount) {
        this.bonusDiscountAmount = bonusDiscountAmount;
    }
}

