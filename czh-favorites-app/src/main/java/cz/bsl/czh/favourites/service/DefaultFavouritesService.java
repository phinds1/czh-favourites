package cz.bsl.czh.favourites.service;

// Grep anchor: favourites

import cz.bsl.czh.favourites.api.FavouriteGroupDto;
import cz.bsl.czh.favourites.api.FavouriteGroupPageDto;
import cz.bsl.czh.favourites.api.FavouriteWagerDto;
import cz.bsl.czh.favourites.api.FavouriteWagerPageDto;
import cz.bsl.czh.favourites.dao.FavouriteGroupRecord;
import cz.bsl.czh.favourites.dao.FavouriteWagerRecord;
import cz.bsl.czh.favourites.dao.FavouritesGroupDao;
import cz.bsl.czh.favourites.dao.FavouritesWagerDao;
import cz.bsl.czh.favourites.metrics.FavouritesMetrics;
import cz.bsl.favourites.config.FavouritesProperties;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Default implementation of {@link FavouritesService}. Validates inputs, enforces quotas, and
 * delegates all persistence to the DAO layer. Ownership checks use the same "not found" exception
 * for absent and wrong-player cases to avoid leaking data existence.
 */
@Service
public class DefaultFavouritesService implements FavouritesService {

    private final FavouritesGroupDao groupDao;
    private final FavouritesWagerDao wagerDao;
    private final FavouritesConverter converter;
    private final FavouritesValidator validator;
    private final FavouritesProperties properties;
    private final FavouritesMetrics metrics;

    public DefaultFavouritesService(FavouritesGroupDao groupDao,
                                    FavouritesWagerDao wagerDao,
                                    FavouritesConverter converter,
                                    FavouritesValidator validator,
                                    FavouritesProperties properties,
                                    FavouritesMetrics metrics) {
        this.groupDao   = groupDao;
        this.wagerDao   = wagerDao;
        this.converter  = converter;
        this.validator  = validator;
        this.properties = properties;
        this.metrics    = metrics;
    }

    // -------------------------------------------------------------------------
    // Wager operations
    // -------------------------------------------------------------------------

    @Override
    public FavouriteWagerDto createWager(FavouriteWagerDto dto, String playerId) {
        try {
            validator.validateWager(dto);
            if (wagerDao.countByPlayer(playerId) >= properties.getMaxFavorites()) {
                throw new IllegalArgumentException("Favourite wager limit of " + properties.getMaxFavorites() + " exceeded");
            }
            requireGroup(dto.getGroupNumber(), playerId);
            FavouriteWagerRecord record = converter.toRecord(dto, playerId);
            long id = wagerDao.insert(record);
            FavouriteWagerRecord saved = wagerDao.findById(id);
            if (saved == null) {
                throw new IllegalStateException("Insert succeeded but wager record not found: " + id);
            }
            FavouriteWagerDto result = converter.toDto(saved);
            metrics.wagerOk(FavouritesMetrics.OP_CREATE);
            return result;
        } catch (RuntimeException e) {
            metrics.wagerFailure(FavouritesMetrics.OP_CREATE);
            throw e;
        }
    }

    @Override
    public FavouriteWagerDto getWager(long wagerId, String playerId) {
        try {
            FavouriteWagerDto result = converter.toDto(requireWager(wagerId, playerId));
            metrics.wagerOk(FavouritesMetrics.OP_GET);
            return result;
        } catch (RuntimeException e) {
            metrics.wagerFailure(FavouritesMetrics.OP_GET);
            throw e;
        }
    }

    @Override
    public FavouriteWagerDto updateWager(long wagerId, FavouriteWagerDto dto, String playerId) {
        try {
            FavouriteWagerRecord existing = requireWager(wagerId, playerId);
            validator.validateWager(dto);
            requireGroup(dto.getGroupNumber(), playerId);
            FavouriteWagerRecord updated = converter.toRecord(dto, playerId);
            updated.setFavWagerId(existing.getFavWagerId());
            updated.setFlags(existing.getFlags());
            wagerDao.update(updated);
            FavouriteWagerDto result = converter.toDto(wagerDao.findById(wagerId));
            metrics.wagerOk(FavouritesMetrics.OP_UPDATE);
            return result;
        } catch (RuntimeException e) {
            metrics.wagerFailure(FavouritesMetrics.OP_UPDATE);
            throw e;
        }
    }

    @Override
    public void deleteWager(long wagerId, String playerId) {
        try {
            requireWager(wagerId, playerId);
            wagerDao.delete(wagerId);
            metrics.wagerOk(FavouritesMetrics.OP_DELETE);
        } catch (RuntimeException e) {
            metrics.wagerFailure(FavouritesMetrics.OP_DELETE);
            throw e;
        }
    }

    @Override
    public FavouriteWagerPageDto listWagers(String playerId, String groupNumber, List<String> gameNames) {
        try {
            List<FavouriteWagerRecord> results;
            if (groupNumber != null && !groupNumber.isBlank() && gameNames != null && !gameNames.isEmpty()) {
                List<FavouriteWagerRecord> byGroup = wagerDao.findByPlayerAndGroup(playerId, groupNumber);
                results = byGroup.stream()
                        .filter(r -> gameNames.contains(r.getGameName()))
                        .toList();
            } else if (groupNumber != null && !groupNumber.isBlank()) {
                results = wagerDao.findByPlayerAndGroup(playerId, groupNumber);
            } else if (gameNames != null && !gameNames.isEmpty()) {
                List<FavouriteWagerRecord> all = new ArrayList<>();
                for (String gameName : gameNames) {
                    all.addAll(wagerDao.findByPlayerAndGame(playerId, gameName));
                }
                results = all;
            } else {
                results = wagerDao.findByPlayer(playerId);
            }
            FavouriteWagerPageDto result = converter.toWagerPage(results);
            metrics.wagerOk(FavouritesMetrics.OP_LIST);
            return result;
        } catch (RuntimeException e) {
            metrics.wagerFailure(FavouritesMetrics.OP_LIST);
            throw e;
        }
    }

    // -------------------------------------------------------------------------
    // Group operations
    // -------------------------------------------------------------------------

    @Override
    public FavouriteGroupDto createGroup(FavouriteGroupDto dto, String playerId) {
        try {
            validator.validateGroup(dto);
            if (groupDao.countByPlayer(playerId) >= properties.getMaxGroupIndex()) {
                throw new IllegalArgumentException("Favourite group limit of " + properties.getMaxGroupIndex() + " exceeded");
            }
            if (groupDao.findByPlayerAndGroup(playerId, dto.getGroupNumber()) != null) {
                throw new IllegalArgumentException("Group " + dto.getGroupNumber() + " already exists for this player");
            }
            FavouriteGroupRecord record = converter.toRecord(dto, playerId);
            groupDao.insert(record);
            FavouriteGroupRecord saved = groupDao.findByPlayerAndGroup(playerId, dto.getGroupNumber());
            if (saved == null) {
                throw new IllegalStateException("Insert succeeded but group record not found: " + dto.getGroupNumber());
            }
            FavouriteGroupDto result = converter.toDto(saved);
            metrics.groupOk(FavouritesMetrics.OP_CREATE);
            return result;
        } catch (RuntimeException e) {
            metrics.groupFailure(FavouritesMetrics.OP_CREATE);
            throw e;
        }
    }

    @Override
    public FavouriteGroupDto getGroup(String groupNumber, String playerId) {
        try {
            FavouriteGroupDto result = converter.toDto(requireGroup(groupNumber, playerId));
            metrics.groupOk(FavouritesMetrics.OP_GET);
            return result;
        } catch (RuntimeException e) {
            metrics.groupFailure(FavouritesMetrics.OP_GET);
            throw e;
        }
    }

    @Override
    public FavouriteGroupDto updateGroup(String groupNumber, FavouriteGroupDto dto, String playerId) {
        try {
            FavouriteGroupRecord existing = requireGroup(groupNumber, playerId);
            validator.validateGroup(dto);
            groupDao.updateName(existing.getFavGroupId(), dto.getGroupName());
            FavouriteGroupDto result = converter.toDto(groupDao.findByPlayerAndGroup(playerId, groupNumber));
            metrics.groupOk(FavouritesMetrics.OP_UPDATE);
            return result;
        } catch (RuntimeException e) {
            metrics.groupFailure(FavouritesMetrics.OP_UPDATE);
            throw e;
        }
    }

    @Override
    public void deleteGroup(String groupNumber, String playerId) {
        try {
            FavouriteGroupRecord existing = requireGroup(groupNumber, playerId);
            groupDao.delete(existing.getFavGroupId());
            metrics.groupOk(FavouritesMetrics.OP_DELETE);
        } catch (RuntimeException e) {
            metrics.groupFailure(FavouritesMetrics.OP_DELETE);
            throw e;
        }
    }

    @Override
    public FavouriteGroupPageDto listGroups(String playerId) {
        try {
            FavouriteGroupPageDto result = converter.toGroupPage(groupDao.findByPlayer(playerId));
            metrics.groupOk(FavouritesMetrics.OP_LIST);
            return result;
        } catch (RuntimeException e) {
            metrics.groupFailure(FavouritesMetrics.OP_LIST);
            throw e;
        }
    }

    // -------------------------------------------------------------------------
    // Ownership helpers
    // -------------------------------------------------------------------------

    private FavouriteWagerRecord requireWager(long wagerId, String playerId) {
        FavouriteWagerRecord record = wagerDao.findById(wagerId);
        if (record == null || !record.getPlayerId().equals(playerId)) {
            throw new NoSuchElementException("Favourite wager not found: " + wagerId);
        }
        return record;
    }

    private FavouriteGroupRecord requireGroup(String groupNumber, String playerId) {
        FavouriteGroupRecord record = groupDao.findByPlayerAndGroup(playerId, groupNumber);
        if (record == null) {
            throw new NoSuchElementException("Favourite group not found: " + groupNumber);
        }
        return record;
    }
}
