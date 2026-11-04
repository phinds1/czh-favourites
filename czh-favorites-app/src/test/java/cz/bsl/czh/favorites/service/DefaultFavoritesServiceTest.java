package cz.bsl.czh.favorites.service;

// Grep anchor: favorites

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import cz.bsl.czh.favorites.api.FavoriteGroupDto;
import cz.bsl.czh.favorites.api.FavoriteWagerDto;
import cz.bsl.czh.favorites.api.FavoriteWagerGroupDto;
import cz.bsl.czh.favorites.api.FavoriteWagerPageDto;
import cz.bsl.czh.favorites.api.WagerDto;
import cz.bsl.czh.favorites.dao.FavoriteGroupRecord;
import cz.bsl.czh.favorites.dao.FavoriteWagerRecord;
import cz.bsl.czh.favorites.dao.FavoritesGroupDao;
import cz.bsl.czh.favorites.dao.FavoritesWagerDao;
import cz.bsl.czh.favorites.metrics.FavoritesMetrics;
import cz.bsl.favorites.config.FavoritesProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;

import static cz.bsl.czh.favorites.metrics.FavoritesMetrics.FAILURE;
import static cz.bsl.czh.favorites.metrics.FavoritesMetrics.GROUP_COUNTER;
import static cz.bsl.czh.favorites.metrics.FavoritesMetrics.OK;
import static cz.bsl.czh.favorites.metrics.FavoritesMetrics.OP_CREATE;
import static cz.bsl.czh.favorites.metrics.FavoritesMetrics.TAG_OP;
import static cz.bsl.czh.favorites.metrics.FavoritesMetrics.TAG_OUTCOME;
import static cz.bsl.czh.favorites.metrics.FavoritesMetrics.WAGER_COUNTER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultFavoritesServiceTest {

    @Mock
    private FavoritesGroupDao groupDao;

    @Mock
    private FavoritesWagerDao wagerDao;

    private DefaultFavoritesService service;
    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        ObjectMapper mapper = JsonMapper.builder().build();
        FavoritesProperties props = new FavoritesProperties();
        FavoritesConverter converter = new FavoritesConverter(mapper);
        FavoritesValidator validator = new FavoritesValidator(props);
        meterRegistry = new SimpleMeterRegistry();
        FavoritesMetrics metrics = new FavoritesMetrics(meterRegistry);
        service = new DefaultFavoritesService(groupDao, wagerDao, converter, validator, props, metrics);
    }

    private double counterValue(String name, String op, String outcome) {
        Counter c = meterRegistry.find(name).tag(TAG_OP, op).tag(TAG_OUTCOME, outcome).counter();
        return c == null ? 0.0 : c.count();
    }

    // --- createWager: group resolution ---

    @Test
    void createWager_existingGroupId_usesItWithoutCreating() {
        FavoriteWagerDto dto = wagerDto("LOTTO", "1", "My Lotto");
        when(wagerDao.countByPlayer("p1")).thenReturn(0);
        when(groupDao.findByPlayerAndGroup("p1", "1")).thenReturn(groupRecord("1"));
        when(wagerDao.insert(any())).thenReturn(10L);
        when(wagerDao.findById(10L)).thenReturn(wagerRecord(10L, "p1", "1", "LOTTO"));
        when(groupDao.findByPlayer("p1")).thenReturn(List.of(groupRecord("1")));

        FavoriteWagerDto result = service.createWager(dto, "p1");

        assertEquals("10", result.getId(), "wire id is the surrogate key as a string");
        assertEquals("1", result.getGroup().getId());
        assertEquals("Group 1", result.getGroup().getName(), "group name is enriched from the player's groups");
        verify(groupDao, never()).insert(any());
        assertEquals(1.0, counterValue(WAGER_COUNTER, OP_CREATE, OK), 0.001);
    }

    @Test
    void createWager_unknownGroupId_createsGroupWithThatNumber() {
        FavoriteWagerDto dto = wagerDto("LOTTO", "3", "My Lotto");
        dto.getGroup().setName("G");
        when(wagerDao.countByPlayer("p1")).thenReturn(0);
        when(groupDao.findByPlayerAndGroup("p1", "3")).thenReturn(null);
        when(groupDao.countByPlayer("p1")).thenReturn(0);
        when(wagerDao.insert(any())).thenReturn(10L);
        when(wagerDao.findById(10L)).thenReturn(wagerRecord(10L, "p1", "3", "LOTTO"));
        when(groupDao.findByPlayer("p1")).thenReturn(List.of());

        service.createWager(dto, "p1");

        ArgumentCaptor<FavoriteGroupRecord> captor = ArgumentCaptor.forClass(FavoriteGroupRecord.class);
        verify(groupDao).insert(captor.capture());
        assertEquals("3", captor.getValue().getGroupNumber());
        assertEquals("G", captor.getValue().getGroupName(), "group name comes from the wager body");
        assertEquals("p1", captor.getValue().getPlayerId());
    }

    @Test
    void createWager_groupNameOnly_resolvesByExistingName() {
        FavoriteWagerDto dto = wagerDto("LOTTO", null, "My Lotto");
        dto.getGroup().setName("Sportka Group");
        when(wagerDao.countByPlayer("p1")).thenReturn(0);
        FavoriteGroupRecord existing = groupRecord("2");
        existing.setGroupName("Sportka Group");
        when(groupDao.findByPlayer("p1")).thenReturn(List.of(existing));
        when(wagerDao.insert(any())).thenReturn(11L);
        when(wagerDao.findById(11L)).thenReturn(wagerRecord(11L, "p1", "2", "LOTTO"));

        FavoriteWagerDto result = service.createWager(dto, "p1");

        assertEquals("2", result.getGroup().getId(), "group resolved by name");
        verify(groupDao, never()).insert(any());
    }

    @Test
    void createWager_groupNameOnly_unknownName_autoAssignsFirstSlot() {
        FavoriteWagerDto dto = wagerDto("LOTTO", null, "My Lotto");
        dto.getGroup().setName("Brand New Group");
        when(wagerDao.countByPlayer("p1")).thenReturn(0);
        when(groupDao.findByPlayer("p1")).thenReturn(List.of());
        when(groupDao.countByPlayer("p1")).thenReturn(0);
        when(wagerDao.insert(any())).thenReturn(12L);
        when(wagerDao.findById(12L)).thenReturn(wagerRecord(12L, "p1", "1", "LOTTO"));

        service.createWager(dto, "p1");

        ArgumentCaptor<FavoriteGroupRecord> captor = ArgumentCaptor.forClass(FavoriteGroupRecord.class);
        verify(groupDao).insert(captor.capture());
        assertEquals("1", captor.getValue().getGroupNumber(), "first slot is minGroupIndex");
        assertEquals("Brand New Group", captor.getValue().getGroupName());
    }

    @Test
    void createWager_groupNameOnly_unknownName_assignsMaxPlusOne() {
        FavoriteWagerDto dto = wagerDto("LOTTO", null, "My Lotto");
        dto.getGroup().setName("Third Group");
        when(wagerDao.countByPlayer("p1")).thenReturn(0);
        when(groupDao.findByPlayer("p1")).thenReturn(List.of(groupRecord("2"), groupRecord("5")));
        when(groupDao.countByPlayer("p1")).thenReturn(2);
        when(wagerDao.insert(any())).thenReturn(13L);
        when(wagerDao.findById(13L)).thenReturn(wagerRecord(13L, "p1", "6", "LOTTO"));

        service.createWager(dto, "p1");

        ArgumentCaptor<FavoriteGroupRecord> captor = ArgumentCaptor.forClass(FavoriteGroupRecord.class);
        verify(groupDao).insert(captor.capture());
        assertEquals("6", captor.getValue().getGroupNumber(), "next slot is max+1 (legacy behaviour)");
    }

    @Test
    void createWager_noGroup_createsDefaultGroup() {
        FavoriteWagerDto dto = new FavoriteWagerDto();
        dto.setName("My Lotto");
        dto.setWager(wager("LOTTO"));
        when(wagerDao.countByPlayer("p1")).thenReturn(0);
        when(groupDao.findByPlayer("p1")).thenReturn(List.of());
        when(groupDao.countByPlayer("p1")).thenReturn(0);
        when(wagerDao.insert(any())).thenReturn(14L);
        when(wagerDao.findById(14L)).thenReturn(wagerRecord(14L, "p1", "1", "LOTTO"));

        service.createWager(dto, "p1");

        ArgumentCaptor<FavoriteGroupRecord> captor = ArgumentCaptor.forClass(FavoriteGroupRecord.class);
        verify(groupDao).insert(captor.capture());
        assertEquals("1", captor.getValue().getGroupNumber());
        assertEquals("", captor.getValue().getGroupName(), "no group in body -> default group name");
    }

    @Test
    void createWager_groupQuotaExceededOnAutoCreate_throws() {
        FavoriteWagerDto dto = wagerDto("LOTTO", null, "My Lotto");
        dto.getGroup().setName("New Group");
        when(wagerDao.countByPlayer("p1")).thenReturn(0);
        when(groupDao.findByPlayer("p1")).thenReturn(List.of());
        when(groupDao.countByPlayer("p1")).thenReturn(10); // quota already reached

        assertThrows(IllegalArgumentException.class, () -> service.createWager(dto, "p1"));
        verify(wagerDao, never()).insert(any());
    }

    // --- createWager: limits and persistence ---

    @Test
    void createWager_limitExceeded_throws() {
        FavoriteWagerDto dto = wagerDto("LOTTO", "1", "My Lotto");
        when(wagerDao.countByPlayer("p1")).thenReturn(50);

        assertThrows(IllegalArgumentException.class, () -> service.createWager(dto, "p1"));
    }

    @Test
    void createWager_findByIdReturnsNullAfterInsert_throwsIllegalState() {
        FavoriteWagerDto dto = wagerDto("LOTTO", "1", "My Lotto");
        when(wagerDao.countByPlayer("p1")).thenReturn(0);
        when(groupDao.findByPlayerAndGroup("p1", "1")).thenReturn(groupRecord("1"));
        when(wagerDao.insert(any())).thenReturn(99L);
        when(wagerDao.findById(99L)).thenReturn(null); // simulate DAO returning null

        assertThrows(IllegalStateException.class, () -> service.createWager(dto, "p1"));
    }

    @Test
    void createWager_storedJsonIsStrippedReplayTemplate() {
        FavoriteWagerDto dto = wagerDto("LOTTO", "1", "My Lotto");
        dto.getWager().setStatus("WON"); // server-assigned junk in the request body
        when(wagerDao.countByPlayer("p1")).thenReturn(0);
        when(groupDao.findByPlayerAndGroup("p1", "1")).thenReturn(groupRecord("1"));
        when(wagerDao.insert(any())).thenReturn(10L);
        when(wagerDao.findById(10L)).thenReturn(wagerRecord(10L, "p1", "1", "LOTTO"));
        when(groupDao.findByPlayer("p1")).thenReturn(List.of(groupRecord("1")));

        service.createWager(dto, "p1");

        ArgumentCaptor<FavoriteWagerRecord> captor = ArgumentCaptor.forClass(FavoriteWagerRecord.class);
        verify(wagerDao).insert(captor.capture());
        String stored = captor.getValue().getWagerJson();
        assertFalse(stored.contains("WON"), "server-assigned fields must not be stored");
        assertTrue(stored.contains("LOTTO"));
    }

    // --- getWager ---

    @Test
    void getWager_found_returnsDtoWithGroupEnriched() {
        when(wagerDao.findById(5L)).thenReturn(wagerRecord(5L, "p1", "1", "KENO"));
        when(groupDao.findByPlayer("p1")).thenReturn(List.of(groupRecord("1")));

        FavoriteWagerDto result = service.getWager(5L, "p1");

        assertEquals("5", result.getId());
        assertEquals("KENO", result.getWager().getGameName());
        assertEquals("Group 1", result.getGroup().getName());
    }

    @Test
    void getWager_notFound_throwsNoSuchElement() {
        when(wagerDao.findById(99L)).thenReturn(null);

        assertThrows(NoSuchElementException.class, () -> service.getWager(99L, "p1"));
    }

    @Test
    void getWager_wrongPlayer_throwsNoSuchElement() {
        when(wagerDao.findById(5L)).thenReturn(wagerRecord(5L, "other-player", "1", "LOTTO"));

        assertThrows(NoSuchElementException.class, () -> service.getWager(5L, "p1"));
    }

    // --- updateWager ---

    @Test
    void updateWager_updatesAndReturns() {
        FavoriteWagerRecord existing = wagerRecord(5L, "p1", "1", "LOTTO");
        FavoriteWagerRecord updated = wagerRecord(5L, "p1", "1", "LOTTO");
        updated.setWagerName("Updated Name");
        FavoriteWagerDto dto = wagerDto("LOTTO", "1", "Updated Name");
        when(wagerDao.findById(5L)).thenReturn(existing).thenReturn(updated);
        when(groupDao.findByPlayerAndGroup("p1", "1")).thenReturn(groupRecord("1"));
        when(groupDao.findByPlayer("p1")).thenReturn(List.of(groupRecord("1")));

        FavoriteWagerDto result = service.updateWager(5L, dto, "p1");

        verify(wagerDao).update(any());
        assertNotNull(result);
        assertEquals("Updated Name", result.getName());
    }

    @Test
    void updateWager_noGroupInBody_keepsCurrentGroup() {
        FavoriteWagerRecord existing = wagerRecord(5L, "p1", "2", "LOTTO");
        FavoriteWagerDto dto = new FavoriteWagerDto();
        dto.setName("Renamed");
        dto.setWager(wager("LOTTO"));
        when(wagerDao.findById(5L)).thenReturn(existing).thenReturn(existing);
        when(groupDao.findByPlayer("p1")).thenReturn(List.of(groupRecord("2")));

        FavoriteWagerDto result = service.updateWager(5L, dto, "p1");

        ArgumentCaptor<FavoriteWagerRecord> captor = ArgumentCaptor.forClass(FavoriteWagerRecord.class);
        verify(wagerDao).update(captor.capture());
        assertEquals("2", captor.getValue().getGroupNumber(), "current group kept");
        assertEquals("2", result.getGroup().getId());
    }

    @Test
    void updateWager_unknownGroupId_throwsNotFound() {
        FavoriteWagerRecord existing = wagerRecord(5L, "p1", "1", "LOTTO");
        FavoriteWagerDto dto = wagerDto("LOTTO", "9", "N");
        when(wagerDao.findById(5L)).thenReturn(existing);
        when(groupDao.findByPlayerAndGroup("p1", "9")).thenReturn(null);

        assertThrows(NoSuchElementException.class, () -> service.updateWager(5L, dto, "p1"));
    }

    @Test
    void updateWager_groupNameMismatch_throwsNotFound() {
        FavoriteWagerRecord existing = wagerRecord(5L, "p1", "1", "LOTTO");
        FavoriteWagerDto dto = wagerDto("LOTTO", "1", "N");
        dto.getGroup().setName("Wrong Name");
        when(wagerDao.findById(5L)).thenReturn(existing);
        when(groupDao.findByPlayerAndGroup("p1", "1")).thenReturn(groupRecord("1")); // named "Group 1"

        assertThrows(NoSuchElementException.class, () -> service.updateWager(5L, dto, "p1"));
    }

    // --- deleteWager ---

    @Test
    void deleteWager_deletesRow() {
        when(wagerDao.findById(3L)).thenReturn(wagerRecord(3L, "p1", "1", "LOTTO"));

        service.deleteWager(3L, "p1");

        verify(wagerDao).delete(3L);
    }

    @Test
    void deleteWager_notFound_throws() {
        when(wagerDao.findById(3L)).thenReturn(null);

        assertThrows(NoSuchElementException.class, () -> service.deleteWager(3L, "p1"));
    }

    // --- listWagers ---

    @Test
    void listWagers_noFilter_returnsAll() {
        when(wagerDao.findByPlayer("p1")).thenReturn(List.of(
                wagerRecord(1L, "p1", "1", "LOTTO"),
                wagerRecord(2L, "p1", "2", "KENO")
        ));
        when(groupDao.findByPlayer("p1")).thenReturn(List.of(groupRecord("1"), groupRecord("2")));

        FavoriteWagerPageDto page = service.listWagers("p1", null, null);

        assertEquals(2, page.getTotalCount());
        assertEquals(2, page.getFavoriteWagers().size());
        assertEquals("Group 1", page.getFavoriteWagers().get(0).getGroup().getName());
    }

    @Test
    void listWagers_byGroup_filters() {
        when(wagerDao.findByPlayerAndGroup("p1", "1")).thenReturn(List.of(
                wagerRecord(1L, "p1", "1", "LOTTO")
        ));
        when(groupDao.findByPlayer("p1")).thenReturn(List.of(groupRecord("1")));

        FavoriteWagerPageDto page = service.listWagers("p1", "1", null);

        assertEquals(1, page.getTotalCount());
    }

    // --- createGroup ---

    @Test
    void createGroup_withId_persistsAndReturns() {
        when(groupDao.countByPlayer("p1")).thenReturn(0);
        FavoriteGroupRecord saved = groupRecord("1");
        saved.setGroupName("My Group");
        when(groupDao.findByPlayerAndGroup("p1", "1")).thenReturn(null).thenReturn(saved);

        FavoriteGroupDto dto = groupDto("1", "My Group");
        FavoriteGroupDto result = service.createGroup(dto, "p1");

        verify(groupDao).insert(any());
        assertNotNull(result);
        assertEquals("1", result.getId(), "wire id is the slot number");
        assertEquals("My Group", result.getName());
    }

    @Test
    void createGroup_legacyBodyNameOnly_autoAssignsSlot() {
        when(groupDao.countByPlayer("p1")).thenReturn(0);
        when(groupDao.findByPlayer("p1")).thenReturn(List.of());   // no groups -> minGroupIndex
        // id absent: no duplicate check call, only the post-insert read
        when(groupDao.findByPlayerAndGroup("p1", "1")).thenReturn(groupRecord("1"));

        FavoriteGroupDto dto = groupDto(null, "V Group");
        FavoriteGroupDto result = service.createGroup(dto, "p1");

        ArgumentCaptor<FavoriteGroupRecord> captor = ArgumentCaptor.forClass(FavoriteGroupRecord.class);
        verify(groupDao).insert(captor.capture());
        assertEquals("1", captor.getValue().getGroupNumber(), "auto-assigned from minGroupIndex");
        assertEquals("V Group", captor.getValue().getGroupName());
        assertEquals("1", result.getId());
    }

    @Test
    void createGroup_limitExceeded_throws() {
        when(groupDao.countByPlayer("p1")).thenReturn(10);

        FavoriteGroupDto dto = groupDto("1", "Group");
        assertThrows(IllegalArgumentException.class, () -> service.createGroup(dto, "p1"));
    }

    @Test
    void createGroup_duplicate_throws() {
        when(groupDao.countByPlayer("p1")).thenReturn(2);
        when(groupDao.findByPlayerAndGroup("p1", "1")).thenReturn(groupRecord("1"));

        FavoriteGroupDto dto = groupDto("1", "Duplicate Group");
        assertThrows(IllegalArgumentException.class, () -> service.createGroup(dto, "p1"));
    }

    @Test
    void createGroup_findAfterInsertReturnsNull_throwsIllegalState() {
        when(groupDao.countByPlayer("p1")).thenReturn(0);
        // first call (duplicate check) null, second call (post-insert read) null
        when(groupDao.findByPlayerAndGroup("p1", "1")).thenReturn(null).thenReturn(null);

        FavoriteGroupDto dto = groupDto("1", "My Group");
        assertThrows(IllegalStateException.class, () -> service.createGroup(dto, "p1"));
    }

    // --- getGroup / updateGroup / deleteGroup / listGroups ---

    @Test
    void getGroup_found_returnsDto() {
        when(groupDao.findByPlayerAndGroup("p1", "2")).thenReturn(groupRecord("2"));

        FavoriteGroupDto result = service.getGroup("2", "p1");

        assertEquals("2", result.getId());
        assertEquals("Group 2", result.getName());
    }

    @Test
    void getGroup_notFound_throws() {
        when(groupDao.findByPlayerAndGroup("p1", "5")).thenReturn(null);

        assertThrows(NoSuchElementException.class, () -> service.getGroup("5", "p1"));
    }

    @Test
    void updateGroup_updatesName() {
        FavoriteGroupRecord existing = groupRecord("1");
        when(groupDao.findByPlayerAndGroup("p1", "1")).thenReturn(existing).thenReturn(existing);

        FavoriteGroupDto dto = groupDto("1", "New Name");
        service.updateGroup("1", dto, "p1");

        verify(groupDao).updateName(existing.getFavGroupId(), "New Name");
    }

    @Test
    void deleteGroup_deletesRow() {
        FavoriteGroupRecord record = groupRecord("3");
        when(groupDao.findByPlayerAndGroup("p1", "3")).thenReturn(record);

        service.deleteGroup("3", "p1");

        verify(groupDao).delete(record.getFavGroupId());
    }

    @Test
    void listGroups_returnsPage() {
        when(groupDao.findByPlayer("p1")).thenReturn(List.of(groupRecord("1"), groupRecord("2")));

        var page = service.listGroups("p1");

        assertEquals(2, page.getTotalCount());
        assertEquals(2, page.getFavoriteWagerGroups().size());
    }

    // --- Metrics failure counters ---

    @Test
    void createWager_onFailure_incrementsWagerFailureCounter() {
        FavoriteWagerDto dto = wagerDto("LOTTO", "1", "My Lotto");
        when(wagerDao.countByPlayer("p1")).thenReturn(50); // triggers failure

        assertThrows(IllegalArgumentException.class, () -> service.createWager(dto, "p1"));
        assertEquals(1.0, counterValue(WAGER_COUNTER, OP_CREATE, FAILURE), 0.001);
        assertEquals(0.0, counterValue(WAGER_COUNTER, OP_CREATE, OK), 0.001);
    }

    @Test
    void createGroup_onFailure_incrementsGroupFailureCounter() {
        when(groupDao.countByPlayer("p1")).thenReturn(10); // triggers limit failure

        FavoriteGroupDto dto = groupDto("1", "Group");
        assertThrows(IllegalArgumentException.class, () -> service.createGroup(dto, "p1"));
        assertEquals(1.0, counterValue(GROUP_COUNTER, OP_CREATE, FAILURE), 0.001);
        assertEquals(0.0, counterValue(GROUP_COUNTER, OP_CREATE, OK), 0.001);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static FavoriteWagerDto wagerDto(String gameName, String groupId, String name) {
        FavoriteWagerDto dto = new FavoriteWagerDto();
        dto.setName(name);
        // group name stays null: an update must not fail the legacy name-mismatch check
        dto.setGroup(new FavoriteWagerGroupDto(groupId, null));
        dto.setWager(wager(gameName));
        return dto;
    }

    private static WagerDto wager(String gameName) {
        WagerDto wager = new WagerDto();
        wager.setGameName(gameName);
        wager.setStake(100L);
        wager.setPrice(100L);
        wager.setDuration(1);
        wager.setBoards(List.of());
        cz.bsl.czh.favorites.api.BoardDto board = new cz.bsl.czh.favorites.api.BoardDto();
        board.setSelections(List.of("1", "2", "3"));
        wager.setBoards(List.of(board));
        return wager;
    }

    private static FavoriteWagerRecord wagerRecord(long id, String playerId, String group, String gameName) {
        FavoriteWagerRecord r = new FavoriteWagerRecord();
        r.setFavWagerId(id);
        r.setPlayerId(playerId);
        r.setGroupNumber(group);
        r.setGameName(gameName);
        r.setWagerName("");
        r.setFlags(0);
        r.setCreatedAt(Instant.now());
        r.setWagerJson("{\"gameName\":\"" + gameName + "\",\"stake\":100,\"price\":100,\"duration\":1," +
                "\"boards\":[{\"selections\":[\"1\",\"2\",\"3\"]}]}");
        return r;
    }

    private static FavoriteGroupRecord groupRecord(String groupNumber) {
        FavoriteGroupRecord r = new FavoriteGroupRecord();
        r.setFavGroupId(Long.parseLong(groupNumber));
        r.setPlayerId("p1");
        r.setGroupNumber(groupNumber);
        r.setGroupName("Group " + groupNumber);
        r.setFlags(0);
        r.setCreatedAt(Instant.now());
        r.setUpdatedAt(Instant.now());
        return r;
    }

    private static FavoriteGroupDto groupDto(String id, String name) {
        return new FavoriteGroupDto(id, name);
    }
}
