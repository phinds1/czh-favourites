package cz.bsl.czh.favourites.service;

// Grep anchor: favourites

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import cz.bsl.czh.favourites.api.FavouriteGroupDto;
import cz.bsl.czh.favourites.api.FavouriteWagerDto;
import cz.bsl.czh.favourites.api.FavouriteWagerPageDto;
import cz.bsl.czh.favourites.api.WagerDto;
import cz.bsl.czh.favourites.dao.FavouriteGroupRecord;
import cz.bsl.czh.favourites.dao.FavouriteWagerRecord;
import cz.bsl.czh.favourites.dao.FavouritesGroupDao;
import cz.bsl.czh.favourites.dao.FavouritesWagerDao;
import cz.bsl.czh.favourites.metrics.FavouritesMetrics;
import cz.bsl.favourites.config.FavouritesProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;

import static cz.bsl.czh.favourites.metrics.FavouritesMetrics.FAILURE;
import static cz.bsl.czh.favourites.metrics.FavouritesMetrics.GROUP_COUNTER;
import static cz.bsl.czh.favourites.metrics.FavouritesMetrics.OK;
import static cz.bsl.czh.favourites.metrics.FavouritesMetrics.OP_CREATE;
import static cz.bsl.czh.favourites.metrics.FavouritesMetrics.TAG_OP;
import static cz.bsl.czh.favourites.metrics.FavouritesMetrics.TAG_OUTCOME;
import static cz.bsl.czh.favourites.metrics.FavouritesMetrics.WAGER_COUNTER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultFavouritesServiceTest {

    @Mock
    private FavouritesGroupDao groupDao;

    @Mock
    private FavouritesWagerDao wagerDao;

    private DefaultFavouritesService service;
    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        ObjectMapper mapper = JsonMapper.builder().build();
        FavouritesProperties props = new FavouritesProperties();
        FavouritesConverter converter = new FavouritesConverter(mapper);
        FavouritesValidator validator = new FavouritesValidator(props);
        meterRegistry = new SimpleMeterRegistry();
        FavouritesMetrics metrics = new FavouritesMetrics(meterRegistry);
        service = new DefaultFavouritesService(groupDao, wagerDao, converter, validator, props, metrics);
    }

    private double counterValue(String name, String op, String outcome) {
        Counter c = meterRegistry.find(name).tag(TAG_OP, op).tag(TAG_OUTCOME, outcome).counter();
        return c == null ? 0.0 : c.count();
    }

    // --- createWager ---

    @Test
    void createWager_persistsAndReturnsDto() {
        FavouriteWagerDto dto = wagerDto("LOTTO", "1");
        when(wagerDao.countByPlayer("p1")).thenReturn(0);
        when(groupDao.findByPlayerAndGroup("p1", "1")).thenReturn(groupRecord("1"));
        when(wagerDao.insert(any())).thenReturn(10L);
        when(wagerDao.findById(10L)).thenReturn(wagerRecord(10L, "p1", "1", "LOTTO"));

        FavouriteWagerDto result = service.createWager(dto, "p1");

        assertNotNull(result);
        assertEquals(10L, result.getId());
        verify(wagerDao).insert(any());
        assertEquals(1.0, counterValue(WAGER_COUNTER, OP_CREATE, OK), 0.001);
    }

    @Test
    void createWager_limitExceeded_throws() {
        FavouriteWagerDto dto = wagerDto("LOTTO", "1");
        when(wagerDao.countByPlayer("p1")).thenReturn(50);

        assertThrows(IllegalArgumentException.class, () -> service.createWager(dto, "p1"));
    }

    @Test
    void createWager_groupNotFound_throws() {
        FavouriteWagerDto dto = wagerDto("LOTTO", "1");
        when(wagerDao.countByPlayer("p1")).thenReturn(0);
        when(groupDao.findByPlayerAndGroup("p1", "1")).thenReturn(null);

        assertThrows(NoSuchElementException.class, () -> service.createWager(dto, "p1"));
    }

    // --- getWager ---

    @Test
    void getWager_found_returnsDto() {
        when(wagerDao.findById(5L)).thenReturn(wagerRecord(5L, "p1", "1", "KENO"));

        FavouriteWagerDto result = service.getWager(5L, "p1");

        assertEquals(5L, result.getId());
        assertEquals("KENO", result.getWager().getGameName());
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
        FavouriteWagerRecord existing = wagerRecord(5L, "p1", "1", "LOTTO");
        when(wagerDao.findById(5L)).thenReturn(existing);
        when(groupDao.findByPlayerAndGroup("p1", "1")).thenReturn(groupRecord("1"));
        FavouriteWagerRecord updated = wagerRecord(5L, "p1", "1", "LOTTO");
        updated.setWagerName("Updated Name");
        when(wagerDao.findById(5L)).thenReturn(existing).thenReturn(updated);

        FavouriteWagerDto dto = wagerDto("LOTTO", "1");
        dto.setWagerName("Updated Name");
        FavouriteWagerDto result = service.updateWager(5L, dto, "p1");

        verify(wagerDao).update(any());
        assertNotNull(result);
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

        FavouriteWagerPageDto page = service.listWagers("p1", null, null);

        assertEquals(2, page.getTotalCount());
    }

    @Test
    void listWagers_byGroup_filters() {
        when(wagerDao.findByPlayerAndGroup("p1", "1")).thenReturn(List.of(
                wagerRecord(1L, "p1", "1", "LOTTO")
        ));

        FavouriteWagerPageDto page = service.listWagers("p1", "1", null);

        assertEquals(1, page.getTotalCount());
    }

    // --- createGroup ---

    @Test
    void createGroup_persistsAndReturns() {
        when(groupDao.countByPlayer("p1")).thenReturn(0);
        when(groupDao.findByPlayerAndGroup("p1", "1")).thenReturn(null).thenReturn(groupRecord("1"));

        FavouriteGroupDto dto = groupDto("1", "My Group");
        FavouriteGroupDto result = service.createGroup(dto, "p1");

        verify(groupDao).insert(any());
        assertNotNull(result);
        assertEquals("1", result.getGroupNumber());
    }

    @Test
    void createGroup_limitExceeded_throws() {
        when(groupDao.countByPlayer("p1")).thenReturn(10);

        FavouriteGroupDto dto = groupDto("1", "Group");
        assertThrows(IllegalArgumentException.class, () -> service.createGroup(dto, "p1"));
    }

    @Test
    void createGroup_duplicate_throws() {
        when(groupDao.countByPlayer("p1")).thenReturn(2);
        when(groupDao.findByPlayerAndGroup("p1", "1")).thenReturn(groupRecord("1"));

        FavouriteGroupDto dto = groupDto("1", "Duplicate Group");
        assertThrows(IllegalArgumentException.class, () -> service.createGroup(dto, "p1"));
    }

    // --- getGroup ---

    @Test
    void getGroup_found_returnsDto() {
        when(groupDao.findByPlayerAndGroup("p1", "2")).thenReturn(groupRecord("2"));

        FavouriteGroupDto result = service.getGroup("2", "p1");

        assertEquals("2", result.getGroupNumber());
    }

    @Test
    void getGroup_notFound_throws() {
        when(groupDao.findByPlayerAndGroup("p1", "5")).thenReturn(null);

        assertThrows(NoSuchElementException.class, () -> service.getGroup("5", "p1"));
    }

    // --- updateGroup ---

    @Test
    void updateGroup_updatesName() {
        FavouriteGroupRecord existing = groupRecord("1");
        when(groupDao.findByPlayerAndGroup("p1", "1")).thenReturn(existing).thenReturn(existing);

        FavouriteGroupDto dto = groupDto("1", "New Name");
        service.updateGroup("1", dto, "p1");

        verify(groupDao).updateName(existing.getFavGroupId(), "New Name");
    }

    // --- deleteGroup ---

    @Test
    void deleteGroup_deletesRow() {
        FavouriteGroupRecord record = groupRecord("3");
        when(groupDao.findByPlayerAndGroup("p1", "3")).thenReturn(record);

        service.deleteGroup("3", "p1");

        verify(groupDao).delete(record.getFavGroupId());
    }

    // --- listGroups ---

    @Test
    void listGroups_returnsPage() {
        when(groupDao.findByPlayer("p1")).thenReturn(List.of(groupRecord("1"), groupRecord("2")));

        var page = service.listGroups("p1");

        assertEquals(2, page.getTotalCount());
    }

    // --- Security finding 3: null-check after insert ---

    @Test
    void createWager_findByIdReturnsNullAfterInsert_throwsIllegalState() {
        FavouriteWagerDto dto = wagerDto("LOTTO", "1");
        when(wagerDao.countByPlayer("p1")).thenReturn(0);
        when(groupDao.findByPlayerAndGroup("p1", "1")).thenReturn(groupRecord("1"));
        when(wagerDao.insert(any())).thenReturn(99L);
        when(wagerDao.findById(99L)).thenReturn(null); // simulate DAO returning null

        assertThrows(IllegalStateException.class, () -> service.createWager(dto, "p1"));
    }

    @Test
    void createGroup_findAfterInsertReturnsNull_throwsIllegalState() {
        when(groupDao.countByPlayer("p1")).thenReturn(0);
        // first call (duplicate check) null, second call (post-insert read) null
        when(groupDao.findByPlayerAndGroup("p1", "1")).thenReturn(null).thenReturn(null);

        FavouriteGroupDto dto = groupDto("1", "My Group");
        assertThrows(IllegalStateException.class, () -> service.createGroup(dto, "p1"));
    }

    // --- Metrics failure counters ---

    @Test
    void createWager_onFailure_incrementsWagerFailureCounter() {
        FavouriteWagerDto dto = wagerDto("LOTTO", "1");
        when(wagerDao.countByPlayer("p1")).thenReturn(50); // triggers failure

        assertThrows(IllegalArgumentException.class, () -> service.createWager(dto, "p1"));
        assertEquals(1.0, counterValue(WAGER_COUNTER, OP_CREATE, FAILURE), 0.001);
        assertEquals(0.0, counterValue(WAGER_COUNTER, OP_CREATE, OK), 0.001);
    }

    @Test
    void createGroup_onFailure_incrementsGroupFailureCounter() {
        when(groupDao.countByPlayer("p1")).thenReturn(10); // triggers limit failure

        FavouriteGroupDto dto = groupDto("1", "Group");
        assertThrows(IllegalArgumentException.class, () -> service.createGroup(dto, "p1"));
        assertEquals(1.0, counterValue(GROUP_COUNTER, OP_CREATE, FAILURE), 0.001);
        assertEquals(0.0, counterValue(GROUP_COUNTER, OP_CREATE, OK), 0.001);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static FavouriteWagerDto wagerDto(String gameName, String groupNumber) {
        FavouriteWagerDto dto = new FavouriteWagerDto();
        dto.setGroupNumber(groupNumber);
        dto.setWager(new WagerDto(gameName, 100L, 100L, 1, null, List.of()));
        return dto;
    }

    private static FavouriteWagerRecord wagerRecord(long id, String playerId, String group, String gameName) {
        FavouriteWagerRecord r = new FavouriteWagerRecord();
        r.setFavWagerId(id);
        r.setPlayerId(playerId);
        r.setGroupNumber(group);
        r.setGameName(gameName);
        r.setWagerName("");
        r.setFlags(0);
        r.setCreatedAt(Instant.now());
        r.setWagerJson("{\"gameName\":\"" + gameName + "\",\"stake\":100,\"price\":100,\"duration\":1,\"boards\":[]}");
        return r;
    }

    private static FavouriteGroupRecord groupRecord(String groupNumber) {
        FavouriteGroupRecord r = new FavouriteGroupRecord();
        r.setFavGroupId(Long.parseLong(groupNumber));
        r.setPlayerId("p1");
        r.setGroupNumber(groupNumber);
        r.setGroupName("Group " + groupNumber);
        r.setFlags(0);
        r.setCreatedAt(Instant.now());
        r.setUpdatedAt(Instant.now());
        return r;
    }

    private static FavouriteGroupDto groupDto(String groupNumber, String groupName) {
        FavouriteGroupDto dto = new FavouriteGroupDto();
        dto.setGroupNumber(groupNumber);
        dto.setGroupName(groupName);
        return dto;
    }
}
