package cz.bsl.czh.favorites.service;

// Grep anchor: favorites

import cz.bsl.czh.favorites.api.FavoriteGroupDto;
import cz.bsl.czh.favorites.api.FavoriteGroupPageDto;
import cz.bsl.czh.favorites.api.FavoriteWagerDto;
import cz.bsl.czh.favorites.api.FavoriteWagerGroupDto;
import cz.bsl.czh.favorites.api.FavoriteWagerPageDto;
import cz.bsl.czh.favorites.dao.FavoriteGroupRecord;
import cz.bsl.czh.favorites.dao.FavoriteWagerRecord;
import cz.bsl.czh.favorites.dao.FavoritesGroupDao;
import cz.bsl.czh.favorites.dao.FavoritesWagerDao;
import cz.bsl.czh.favorites.metrics.FavoritesMetrics;
import cz.bsl.favorites.config.FavoritesProperties;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Default implementation of {@link FavoritesService}. Validates inputs, enforces quotas, and
 * delegates all persistence to the DAO layer. Ownership checks use the same "not found" exception
 * for absent and wrong-player cases to avoid leaking data existence.
 *
 * <p><b>Group resolution</b> (legacy front-end contract — the wire group id is the group slot
 * number, {@code 1..maxGroupIndex}):
 * <ul>
 *   <li>wager create: {@code group.id} present and existing → use it; present but unknown →
 *       create the group with that number and the body's name (legacy gis behaviour); absent →
 *       resolve by {@code group.name} against the player's groups, else auto-assign the next
 *       slot ({@code minGroupIndex} when the player has no groups, else max+1) and create it;
 *       no group at all → same as absent, with the configured default group name.</li>
 *   <li>wager update: {@code group.id} present → the group must exist and, when a name is
 *       supplied, match it (else 404, legacy behaviour); absent → resolve/create like on
 *       create; no group at all → keep the wager's current group.</li>
 *   <li>group create: id optional; absent → auto-assign the next slot (legacy bodies are
 *       {@code {"name": "..."}} only).</li>
 * </ul>
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
        this.validator = validator;
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
            String groupNumber = resolveGroupForWager(dto, playerId);
            FavoriteWagerRecord record = converter.toRecord(dto, playerId, groupNumber);
            long id = wagerDao.insert(record);
            FavoriteWagerRecord saved = wagerDao.findById(id);
            if (saved == null) {
                throw new IllegalStateException("Insert succeeded but wager record not found: " + id);
            }
            FavoriteWagerDto result = converter.toDto(saved);
            fillGroupNames(List.of(result), playerId);
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
            fillGroupNames(List.of(result), playerId);
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
            String groupNumber = resolveGroupForUpdate(dto, existing, playerId);
            FavoriteWagerRecord updated = converter.toRecord(dto, playerId, groupNumber);
            updated.setFavWagerId(existing.getFavWagerId());
            updated.setFlags(existing.getFlags());
            wagerDao.update(updated);
            FavoriteWagerRecord saved = wagerDao.findById(wagerId);
            if (saved == null) {
                throw new IllegalStateException("Update succeeded but wager record not found: " + wagerId);
            }
            FavoriteWagerDto result = converter.toDto(saved);
            fillGroupNames(List.of(result), playerId);
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
            fillGroupNames(result.getFavoriteWagers(), playerId);
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
            String groupNumber = dto.getId();
            if (groupNumber == null || groupNumber.isBlank()) {
                // legacy create body has name only -> auto-assign the next slot
                groupNumber = nextGroupNumber(playerId);
            } else if (groupDao.findByPlayerAndGroup(playerId, groupNumber) != null) {
                throw new IllegalArgumentException("Group " + groupNumber + " already exists for this player");
            }
            FavoriteGroupRecord record = converter.toRecord(dto, playerId, groupNumber);
            groupDao.insert(record);
            FavoriteGroupRecord saved = groupDao.findByPlayerAndGroup(playerId, groupNumber);
            if (saved == null) {
                throw new IllegalStateException("Insert succeeded but group record not found: " + groupNumber);
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
            groupDao.updateName(existing.getFavGroupId(), dto.getName());
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
    // Group resolution (legacy front-end contract)
    // -------------------------------------------------------------------------

    /**
     * Resolve the group for a wager create. May create the group (explicit number, or
     * auto-assigned when the body carries a name only, or no group at all). Mutates
     * {@code dto.group} so the created group reflects the resolved state.
     */
    private String resolveGroupForWager(FavoriteWagerDto dto, String playerId) {
        FavoriteWagerGroupDto group = dto.getGroup();
        if (group == null) {
            group = new FavoriteWagerGroupDto(null, properties.getDefaultGroupName());
            dto.setGroup(group);
        }
        String number = group.getId();
        if (number != null && !number.isBlank()) {
            if (groupDao.findByPlayerAndGroup(playerId, number) != null) {
                return number;
            }
            insertGroup(playerId, number, group.getName());
            return number;
        }
        FavoriteGroupRecord byName = findGroupByName(playerId, group.getName());
        if (byName != null) {
            return byName.getGroupNumber();
        }
        String assigned = nextGroupNumber(playerId);
        insertGroup(playerId, assigned, group.getName());
        return assigned;
    }

    /**
     * Resolve the group for a wager update: id present → must exist and (name given) match
     * (legacy 404 behaviour); id absent → resolve/create like on create; no group → keep
     * the wager's current group.
     */
    private String resolveGroupForUpdate(FavoriteWagerDto dto, FavoriteWagerRecord existing, String playerId) {
        FavoriteWagerGroupDto group = dto.getGroup();
        if (group == null) {
            return existing.getGroupNumber();
        }
        String number = group.getId();
        if (number == null || number.isBlank()) {
            return resolveGroupForWager(dto, playerId);
        }
        FavoriteGroupRecord record = groupDao.findByPlayerAndGroup(playerId, number);
        if (record == null) {
            throw new NoSuchElementException("Favorite group not found: " + number);
        }
        if (group.getName() != null && !group.getName().equals(record.getGroupName())) {
            throw new NoSuchElementException("Favorite group name mismatch for group " + number);
        }
        return number;
    }

    /** Create the group record for a wager-referenced group, enforcing the group quota. */
    private void insertGroup(String playerId, String groupNumber, String name) {
        if (groupDao.countByPlayer(playerId) >= properties.getMaxGroupIndex()) {
            throw new IllegalArgumentException("Favorite group limit of " + properties.getMaxGroupIndex() + " exceeded");
        }
        FavoriteGroupRecord record = new FavoriteGroupRecord();
        record.setPlayerId(playerId);
        record.setGroupNumber(groupNumber);
        record.setGroupName(name == null ? "" : name);
        record.setFlags(0);
        groupDao.insert(record);
    }

    /** Next free group slot: {@code minGroupIndex} when the player has no groups, else max+1. */
    private String nextGroupNumber(String playerId) {
        List<FavoriteGroupRecord> groups = groupDao.findByPlayer(playerId);
        if (groups.isEmpty()) {
            return String.valueOf(properties.getMinGroupIndex());
        }
        int max = 0;
        for (FavoriteGroupRecord group : groups) {
            max = Math.max(max, Integer.parseInt(group.getGroupNumber()));
        }
        return String.valueOf(max + 1);
    }

    /** The player's group with the given display name, or null. */
    private FavoriteGroupRecord findGroupByName(String playerId, String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        for (FavoriteGroupRecord group : groupDao.findByPlayer(playerId)) {
            if (name.equals(group.getGroupName())) {
                return group;
            }
        }
        return null;
    }

    /** Fill {@code group.name} on the DTOs from the player's groups (one lookup). */
    private void fillGroupNames(List<FavoriteWagerDto> dtos, String playerId) {
        if (dtos == null || dtos.isEmpty()) {
            return;
        }
        Map<String, String> names = new HashMap<>();
        for (FavoriteGroupRecord group : groupDao.findByPlayer(playerId)) {
            names.put(group.getGroupNumber(), group.getGroupName());
        }
        for (FavoriteWagerDto dto : dtos) {
            FavoriteWagerGroupDto group = dto.getGroup();
            if (group != null && group.getId() != null) {
                group.setName(names.get(group.getId()));
            }
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
