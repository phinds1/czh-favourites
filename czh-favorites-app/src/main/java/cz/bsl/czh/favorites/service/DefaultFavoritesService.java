package cz.bsl.czh.favorites.service;

// Grep anchor: favorites

import cz.bsl.czh.favorites.api.FavoriteGroupDto;
import cz.bsl.czh.favorites.api.FavoriteGroupPageDto;
import cz.bsl.czh.favorites.api.FavoriteWagerDto;
import cz.bsl.czh.favorites.api.FavoriteWagerPageDto;
import cz.bsl.czh.favorites.dao.FavoriteGroupRecord;
import cz.bsl.czh.favorites.dao.FavoriteWagerRecord;
import cz.bsl.czh.favorites.dao.FavoritesGroupDao;
import cz.bsl.czh.favorites.dao.FavoritesWagerDao;
import cz.bsl.czh.favorites.metrics.FavoritesMetrics;
import cz.bsl.favorites.config.FavoritesProperties;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Default implementation of {@link FavoritesService}. Validates inputs, enforces quotas, and
 * delegates all persistence to the DAO layer. Ownership checks use the same "not found" exception
 * for absent and wrong-player cases to avoid leaking data existence.
 */
@Service
public class DefaultFavoritesService implements FavoritesService {

    private final FavoritesGroupDao groupDao;
    private final FavoritesWagerDao wagerDao;
    private final FavoritesConverter converter;
    private final FavoritesValidator validator;
    private final FavoritesProperties properties;
    private final FavoritesMetrics metrics;

    public DefaultFavoritesService(FavoritesGroupDao groupDao,
                                    FavoritesWagerDao wagerDao,
                                    FavoritesConverter converter,
                                    FavoritesValidator validator,
                                    FavoritesProperties properties,
                                    FavoritesMetrics metrics) {
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
    public FavoriteWagerDto createWager(FavoriteWagerDto dto, String playerId) {
        try {
            validator.validateWager(dto);
            if (wagerDao.countByPlayer(playerId) >= properties.getMaxFavorites()) {
                throw new IllegalArgumentException("Favorite wager limit of " + properties.getMaxFavorites() + " exceeded");
            }
            requireGroup(dto.getGroupNumber(), playerId);
            FavoriteWagerRecord record = converter.toRecord(dto, playerId);
            long id = wagerDao.insert(record);
            FavoriteWagerRecord saved = wagerDao.findById(id);
            if (saved == null) {
                throw new IllegalStateException("Insert succeeded but wager record not found: " + id);
            }
            FavoriteWagerDto result = converter.toDto(saved);
            metrics.wagerOk(FavoritesMetrics.OP_CREATE);
            return result;
        } catch (RuntimeException e) {
            metrics.wagerFailure(FavoritesMetrics.OP_CREATE);
            throw e;
        }
    }

    @Override
    public FavoriteWagerDto getWager(long wagerId, String playerId) {
        try {
            FavoriteWagerDto result = converter.toDto(requireWager(wagerId, playerId));
            metrics.wagerOk(FavoritesMetrics.OP_GET);
            return result;
        } catch (RuntimeException e) {
            metrics.wagerFailure(FavoritesMetrics.OP_GET);
            throw e;
        }
    }

    @Override
    public FavoriteWagerDto updateWager(long wagerId, FavoriteWagerDto dto, String playerId) {
        try {
            FavoriteWagerRecord existing = requireWager(wagerId, playerId);
            validator.validateWager(dto);
            requireGroup(dto.getGroupNumber(), playerId);
            FavoriteWagerRecord updated = converter.toRecord(dto, playerId);
            updated.setFavWagerId(existing.getFavWagerId());
            updated.setFlags(existing.getFlags());
            wagerDao.update(updated);
            FavoriteWagerDto result = converter.toDto(wagerDao.findById(wagerId));
            metrics.wagerOk(FavoritesMetrics.OP_UPDATE);
            return result;
        } catch (RuntimeException e) {
            metrics.wagerFailure(FavoritesMetrics.OP_UPDATE);
            throw e;
        }
    }

    @Override
    public void deleteWager(long wagerId, String playerId) {
        try {
            requireWager(wagerId, playerId);
            wagerDao.delete(wagerId);
            metrics.wagerOk(FavoritesMetrics.OP_DELETE);
        } catch (RuntimeException e) {
            metrics.wagerFailure(FavoritesMetrics.OP_DELETE);
            throw e;
        }
    }

    @Override
    public FavoriteWagerPageDto listWagers(String playerId, String groupNumber, List<String> gameNames) {
        try {
            List<FavoriteWagerRecord> results;
            if (groupNumber != null && !groupNumber.isBlank() && gameNames != null && !gameNames.isEmpty()) {
                List<FavoriteWagerRecord> byGroup = wagerDao.findByPlayerAndGroup(playerId, groupNumber);
                results = byGroup.stream()
                        .filter(r -> gameNames.contains(r.getGameName()))
                        .toList();
            } else if (groupNumber != null && !groupNumber.isBlank()) {
                results = wagerDao.findByPlayerAndGroup(playerId, groupNumber);
            } else if (gameNames != null && !gameNames.isEmpty()) {
                List<FavoriteWagerRecord> all = new ArrayList<>();
                for (String gameName : gameNames) {
                    all.addAll(wagerDao.findByPlayerAndGame(playerId, gameName));
                }
                results = all;
            } else {
                results = wagerDao.findByPlayer(playerId);
            }
            FavoriteWagerPageDto result = converter.toWagerPage(results);
            metrics.wagerOk(FavoritesMetrics.OP_LIST);
            return result;
        } catch (RuntimeException e) {
            metrics.wagerFailure(FavoritesMetrics.OP_LIST);
            throw e;
        }
    }

    // -------------------------------------------------------------------------
    // Group operations
    // -------------------------------------------------------------------------

    @Override
    public FavoriteGroupDto createGroup(FavoriteGroupDto dto, String playerId) {
        try {
            validator.validateGroup(dto);
            if (groupDao.countByPlayer(playerId) >= properties.getMaxGroupIndex()) {
                throw new IllegalArgumentException("Favorite group limit of " + properties.getMaxGroupIndex() + " exceeded");
            }
            if (groupDao.findByPlayerAndGroup(playerId, dto.getGroupNumber()) != null) {
                throw new IllegalArgumentException("Group " + dto.getGroupNumber() + " already exists for this player");
            }
            FavoriteGroupRecord record = converter.toRecord(dto, playerId);
            groupDao.insert(record);
            FavoriteGroupRecord saved = groupDao.findByPlayerAndGroup(playerId, dto.getGroupNumber());
            if (saved == null) {
                throw new IllegalStateException("Insert succeeded but group record not found: " + dto.getGroupNumber());
            }
            FavoriteGroupDto result = converter.toDto(saved);
            metrics.groupOk(FavoritesMetrics.OP_CREATE);
            return result;
        } catch (RuntimeException e) {
            metrics.groupFailure(FavoritesMetrics.OP_CREATE);
            throw e;
        }
    }

    @Override
    public FavoriteGroupDto getGroup(String groupNumber, String playerId) {
        try {
            FavoriteGroupDto result = converter.toDto(requireGroup(groupNumber, playerId));
            metrics.groupOk(FavoritesMetrics.OP_GET);
            return result;
        } catch (RuntimeException e) {
            metrics.groupFailure(FavoritesMetrics.OP_GET);
            throw e;
        }
    }

    @Override
    public FavoriteGroupDto updateGroup(String groupNumber, FavoriteGroupDto dto, String playerId) {
        try {
            FavoriteGroupRecord existing = requireGroup(groupNumber, playerId);
            validator.validateGroup(dto);
            groupDao.updateName(existing.getFavGroupId(), dto.getGroupName());
            FavoriteGroupDto result = converter.toDto(groupDao.findByPlayerAndGroup(playerId, groupNumber));
            metrics.groupOk(FavoritesMetrics.OP_UPDATE);
            return result;
        } catch (RuntimeException e) {
            metrics.groupFailure(FavoritesMetrics.OP_UPDATE);
            throw e;
        }
    }

    @Override
    public void deleteGroup(String groupNumber, String playerId) {
        try {
            FavoriteGroupRecord existing = requireGroup(groupNumber, playerId);
            groupDao.delete(existing.getFavGroupId());
            metrics.groupOk(FavoritesMetrics.OP_DELETE);
        } catch (RuntimeException e) {
            metrics.groupFailure(FavoritesMetrics.OP_DELETE);
            throw e;
        }
    }

    @Override
    public FavoriteGroupPageDto listGroups(String playerId) {
        try {
            FavoriteGroupPageDto result = converter.toGroupPage(groupDao.findByPlayer(playerId));
            metrics.groupOk(FavoritesMetrics.OP_LIST);
            return result;
        } catch (RuntimeException e) {
            metrics.groupFailure(FavoritesMetrics.OP_LIST);
            throw e;
        }
    }

    // -------------------------------------------------------------------------
    // Ownership helpers
    // -------------------------------------------------------------------------

    private FavoriteWagerRecord requireWager(long wagerId, String playerId) {
        FavoriteWagerRecord record = wagerDao.findById(wagerId);
        if (record == null || !record.getPlayerId().equals(playerId)) {
            throw new NoSuchElementException("Favorite wager not found: " + wagerId);
        }
        return record;
    }

    private FavoriteGroupRecord requireGroup(String groupNumber, String playerId) {
        FavoriteGroupRecord record = groupDao.findByPlayerAndGroup(playerId, groupNumber);
        if (record == null) {
            throw new NoSuchElementException("Favorite group not found: " + groupNumber);
        }
        return record;
    }
}
