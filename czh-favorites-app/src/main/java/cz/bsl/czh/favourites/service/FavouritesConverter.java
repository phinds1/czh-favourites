package cz.bsl.czh.favourites.service;

// Grep anchor: favourites

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import cz.bsl.czh.favourites.api.FavouriteGroupDto;
import cz.bsl.czh.favourites.api.FavouriteGroupPageDto;
import cz.bsl.czh.favourites.api.FavouriteWagerDto;
import cz.bsl.czh.favourites.api.FavouriteWagerPageDto;
import cz.bsl.czh.favourites.api.WagerDto;
import cz.bsl.czh.favourites.dao.FavouriteGroupRecord;
import cz.bsl.czh.favourites.dao.FavouriteWagerRecord;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Converts between API DTOs ({@link FavouriteWagerDto}, {@link FavouriteGroupDto}) and DAO records
 * ({@link FavouriteWagerRecord}, {@link FavouriteGroupRecord}). Owns all {@link ObjectMapper}
 * round-trips for {@code WAGER_JSON}; the DAO is agnostic of JSON.
 *
 * <p>Flags are always forced to 0 on create — the service layer manages flag changes explicitly.
 * PlayerId is never read from the DTO; it comes from the {@code X-Player-Id} header.
 */
@Component
public class FavouritesConverter {

    private static final int MAX_WAGER_JSON_BYTES = 30_000;

    private final ObjectMapper objectMapper;

    public FavouritesConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** Convert an incoming API DTO to a new DAO record ready for insert. */
    public FavouriteWagerRecord toRecord(FavouriteWagerDto dto, String playerId) {
        String wagerJson = serializeWager(dto.getWager());
        if (wagerJson.length() > MAX_WAGER_JSON_BYTES) {
            throw new IllegalArgumentException("Wager JSON exceeds maximum allowed size of " + MAX_WAGER_JSON_BYTES + " characters");
        }
        FavouriteWagerRecord record = new FavouriteWagerRecord();
        record.setPlayerId(playerId);
        record.setGroupNumber(dto.getGroupNumber());
        record.setGameName(dto.getWager() != null ? dto.getWager().getGameName() : "");
        record.setWagerName(dto.getWagerName() != null ? dto.getWagerName() : "");
        record.setFlags(0);
        record.setWagerJson(wagerJson);
        return record;
    }

    /** Convert a DAO record back to an API DTO for HTTP responses. */
    public FavouriteWagerDto toDto(FavouriteWagerRecord record) {
        FavouriteWagerDto dto = new FavouriteWagerDto();
        dto.setId(record.getFavWagerId());
        dto.setPlayerId(record.getPlayerId());
        dto.setGroupNumber(record.getGroupNumber());
        dto.setGameName(record.getGameName());
        dto.setWagerName(record.getWagerName());
        dto.setFlags(record.getFlags());
        dto.setCreatedAt(record.getCreatedAt());
        dto.setWager(deserializeWager(record.getWagerJson()));
        return dto;
    }

    /** Convert an incoming group API DTO to a DAO record. */
    public FavouriteGroupRecord toRecord(FavouriteGroupDto dto, String playerId) {
        FavouriteGroupRecord record = new FavouriteGroupRecord();
        record.setPlayerId(playerId);
        record.setGroupNumber(dto.getGroupNumber());
        record.setGroupName(dto.getGroupName() != null ? dto.getGroupName() : "");
        record.setFlags(0);
        return record;
    }

    /** Convert a DAO record to a group API DTO. */
    public FavouriteGroupDto toDto(FavouriteGroupRecord record) {
        FavouriteGroupDto dto = new FavouriteGroupDto();
        dto.setId(record.getFavGroupId());
        dto.setPlayerId(record.getPlayerId());
        dto.setGroupNumber(record.getGroupNumber());
        dto.setGroupName(record.getGroupName());
        dto.setFlags(record.getFlags());
        dto.setCreatedAt(record.getCreatedAt());
        dto.setUpdatedAt(record.getUpdatedAt());
        return dto;
    }

    /** Convert a list of wager records to a page DTO. */
    public FavouriteWagerPageDto toWagerPage(List<FavouriteWagerRecord> records) {
        return new FavouriteWagerPageDto(records.stream().map(this::toDto).toList(), records.size());
    }

    /** Convert a list of group records to a page DTO. */
    public FavouriteGroupPageDto toGroupPage(List<FavouriteGroupRecord> records) {
        return new FavouriteGroupPageDto(records.stream().map(this::toDto).toList(), records.size());
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
