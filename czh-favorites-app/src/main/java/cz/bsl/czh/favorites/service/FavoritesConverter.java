package cz.bsl.czh.favorites.service;

// Grep anchor: favorites

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import cz.bsl.czh.favorites.api.FavoriteGroupDto;
import cz.bsl.czh.favorites.api.FavoriteGroupPageDto;
import cz.bsl.czh.favorites.api.FavoriteWagerDto;
import cz.bsl.czh.favorites.api.FavoriteWagerGroupDto;
import cz.bsl.czh.favorites.api.FavoriteWagerPageDto;
import cz.bsl.czh.favorites.api.WagerDto;
import cz.bsl.czh.favorites.dao.FavoriteGroupRecord;
import cz.bsl.czh.favorites.dao.FavoriteWagerRecord;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Converts between API DTOs ({@link FavoriteWagerDto}, {@link FavoriteGroupDto}) and DAO records
 * ({@link FavoriteWagerRecord}, {@link FavoriteGroupRecord}). Owns all {@link ObjectMapper}
 * round-trips for {@code WAGER_JSON}; the DAO is agnostic of JSON.
 *
 * <p>Wire contract (legacy front end, gisorch): the favorite {@code id} is the DB surrogate key
 * as a string; the group {@code id} is the group slot number ({@code GROUP_NUMBER}, the same
 * value as {@code group.id} on the wire); the display name travels as {@code name}.
 *
 * <p><b>Server-assigned wager fields are stripped before storing</b> (see
 * {@link #stripServerAssignedFields(WagerDto)}): a favorite is a replay template, so fields
 * that only make sense on a placed wager (outcome, security, response-only grants) are cleared
 * on the incoming wager — including every addon wager. Player-chosen fields are kept verbatim.
 *
 * <p>Flags are always forced to 0 on create — the service layer manages flag changes explicitly.
 * PlayerId is never read from the DTO; it comes from the {@code X-Player-Id} header.
 * The group number is resolved by the service (existing, created, or auto-assigned) and passed
 * in explicitly.
 */
@Component
public class FavoritesConverter {

    /** Matches {@code WAGER_JSON VARCHAR(30000)} in the DB schema. */
    private static final int MAX_WAGER_JSON_BYTES = 30_000;

    private final ObjectMapper objectMapper;

    public FavoritesConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** Convert an incoming API DTO to a new DAO record ready for insert. */
    public FavoriteWagerRecord toRecord(FavoriteWagerDto dto, String playerId, String groupNumber) {
        if (dto.getWager() == null) {
            throw new IllegalArgumentException("Wager sub-object must not be null");
        }
        String wagerJson = serializeWager(stripServerAssignedFields(dto.getWager()));
        if (wagerJson.length() > MAX_WAGER_JSON_BYTES) {
            throw new IllegalArgumentException(
                    "Wager JSON exceeds maximum allowed size of " + MAX_WAGER_JSON_BYTES + " characters");
        }
        FavoriteWagerRecord record = new FavoriteWagerRecord();
        record.setPlayerId(playerId);
        record.setGroupNumber(groupNumber);
        record.setGameName(dto.getWager().getGameName() != null ? dto.getWager().getGameName() : "");
        record.setWagerName(dto.getName() != null ? dto.getName() : "");
        record.setFlags(0);
        record.setWagerJson(wagerJson);
        return record;
    }

    /** Convert a DAO record back to an API DTO for HTTP responses. */
    public FavoriteWagerDto toDto(FavoriteWagerRecord record) {
        FavoriteWagerDto dto = new FavoriteWagerDto();
        dto.setId(record.getFavWagerId() == null ? null : String.valueOf(record.getFavWagerId()));
        dto.setPlayerId(record.getPlayerId());
        if (record.getGroupNumber() != null) {
            // group.name is filled by the service (it needs a group lookup)
            dto.setGroup(new FavoriteWagerGroupDto(record.getGroupNumber(), null));
        }
        dto.setGameName(record.getGameName());
        dto.setName(record.getWagerName() != null ? record.getWagerName() : "");
        dto.setFlags(record.getFlags());
        dto.setCreatedAt(record.getCreatedAt());
        dto.setWager(deserializeWager(record.getWagerJson()));
        return dto;
    }

    /** Convert an incoming group API DTO to a DAO record (groupNumber resolved by the service). */
    public FavoriteGroupRecord toRecord(FavoriteGroupDto dto, String playerId, String groupNumber) {
        FavoriteGroupRecord record = new FavoriteGroupRecord();
        record.setPlayerId(playerId);
        record.setGroupNumber(groupNumber);
        record.setGroupName(dto.getName() != null ? dto.getName() : "");
        record.setFlags(0);
        return record;
    }

    /** Convert a DAO record to a group API DTO (wire id = group slot number). */
    public FavoriteGroupDto toDto(FavoriteGroupRecord record) {
        FavoriteGroupDto dto = new FavoriteGroupDto();
        dto.setId(record.getGroupNumber());
        dto.setPlayerId(record.getPlayerId());
        dto.setName(record.getGroupName());
        dto.setFlags(record.getFlags());
        dto.setCreatedAt(record.getCreatedAt());
        dto.setUpdatedAt(record.getUpdatedAt());
        return dto;
    }

    /** Convert a list of wager records to a page DTO. */
    public FavoriteWagerPageDto toWagerPage(List<FavoriteWagerRecord> records) {
        return new FavoriteWagerPageDto(records.stream().map(this::toDto).toList(), records.size());
    }

    /** Convert a list of group records to a page DTO. */
    public FavoriteGroupPageDto toGroupPage(List<FavoriteGroupRecord> records) {
        return new FavoriteGroupPageDto(records.stream().map(this::toDto).toList(), records.size());
    }

    /**
     * Clears the server-assigned (placement-outcome and response-only) fields of a wager
     * in place, including every addon wager, so the stored {@code WAGER_JSON} is a pure replay
     * template. Kept verbatim: everything the player chose (type, draws, boards, selections,
     * stakes, addon wagers, serialNumber, payment instructions).
     *
     * <p>Stripped: {@code id, requestId, status, rejectReason, error, digest, signature,
     * transactionTime, revision, drawStartDate, drawEndDate, additionalPrizeTier, prizes,
     * freeTicket, freeTickets, ticketSecurity, alreadyPrinted, prolongation, triggerType,
     * correlationId, resultDate, webCode, bonusEntries, fractionSerialNumbers,
     * paymentTransactionId, discountAmount, mainDiscountAmount, addonDiscountAmount,
     * bonusDiscountAmount, taxes, promotion, ticketMessage}.
     */
    static WagerDto stripServerAssignedFields(WagerDto wager) {
        if (wager == null) {
            return null;
        }
        wager.setId(null);
        wager.setRequestId(null);
        wager.setStatus(null);
        wager.setRejectReason(null);
        wager.setError(null);
        wager.setDigest(null);
        wager.setSignature(null);
        wager.setTransactionTime(null);
        wager.setRevision(null);
        wager.setDrawStartDate(null);
        wager.setDrawEndDate(null);
        wager.setAdditionalPrizeTier(null);
        wager.setPrizes(null);
        wager.setFreeTicket(null);
        wager.setFreeTickets(null);
        wager.setTicketSecurity(null);
        wager.setAlreadyPrinted(null);
        wager.setProlongation(null);
        wager.setTriggerType(null);
        wager.setCorrelationId(null);
        wager.setResultDate(null);
        wager.setWebCode(null);
        wager.setBonusEntries(null);
        wager.setFractionSerialNumbers(null);
        wager.setPaymentTransactionId(null);
        wager.setDiscountAmount(null);
        wager.setMainDiscountAmount(null);
        wager.setAddonDiscountAmount(null);
        wager.setBonusDiscountAmount(null);
        wager.setTaxes(null);
        wager.setPromotion(null);
        wager.setTicketMessage(null);
        if (wager.getAddonWagers() != null) {
            wager.getAddonWagers().forEach(FavoritesConverter::stripServerAssignedFields);
        }
        return wager;
    }

    private String serializeWager(WagerDto wager) {
        try {
            return objectMapper.writeValueAsString(wager);
        } catch (JacksonException e) {
            throw new IllegalStateException("Failed to serialize WagerDto to JSON", e);
        }
    }

    private WagerDto deserializeWager(String json) {
        try {
            return objectMapper.readValue(json, WagerDto.class);
        } catch (JacksonException e) {
            throw new IllegalStateException("Failed to deserialize WAGER_JSON to WagerDto", e);
        }
    }
}
