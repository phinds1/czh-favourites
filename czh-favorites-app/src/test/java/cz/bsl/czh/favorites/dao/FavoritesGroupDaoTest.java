package cz.bsl.czh.favorites.dao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit 5 tests for {@link FavoritesGroupDaoImpl} against HSQLDB in-memory.
 *
 * <p>Grep anchor: favorites
 */
@SpringJUnitConfig(DaoTestConfig.class)
class FavoritesGroupDaoTest {

    @Autowired
    private FavoritesGroupDao dao;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM GIS_FAV_GROUP");
    }

    // -------------------------------------------------------------------------

    @Test
    void insert_generatesId() {
        FavoriteGroupRecord r = group("player-1", "1", "My Group");
        long id = dao.insert(r);
        assertTrue(id > 0);
        assertEquals(id, r.getFavGroupId());
    }

    @Test
    void findByPlayer_returnsAllOrderedByGroupNumber() {
        dao.insert(group("player-1", "3", "C"));
        dao.insert(group("player-1", "1", "A"));
        dao.insert(group("player-1", "2", "B"));

        List<FavoriteGroupRecord> results = dao.findByPlayer("player-1");

        assertEquals(3, results.size());
        assertEquals("1", results.get(0).getGroupNumber());
        assertEquals("2", results.get(1).getGroupNumber());
        assertEquals("3", results.get(2).getGroupNumber());
    }

    @Test
    void findByPlayerAndGroup_returnsCorrectGroup() {
        dao.insert(group("player-1", "1", "Alpha"));
        dao.insert(group("player-1", "2", "Beta"));

        FavoriteGroupRecord result = dao.findByPlayerAndGroup("player-1", "1");

        assertNotNull(result);
        assertEquals("player-1", result.getPlayerId());
        assertEquals("1", result.getGroupNumber());
        assertEquals("Alpha", result.getGroupName());
        assertNotNull(result.getCreatedAt());
        assertNotNull(result.getUpdatedAt());
    }

    @Test
    void findByPlayerAndGroup_returnsNullWhenAbsent() {
        assertNull(dao.findByPlayerAndGroup("nobody", "99"));
    }

    @Test
    void insert_throwsDuplicateKeyOnDuplicateGroupNumber() {
        dao.insert(group("player-1", "1", "First"));
        assertThrows(DuplicateKeyException.class,
                () -> dao.insert(group("player-1", "1", "Second")));
    }

    @Test
    void updateName_changesGroupName() {
        long id = dao.insert(group("player-1", "1", "Old Name"));

        int rows = dao.updateName(id, "New Name");

        assertEquals(1, rows);
        FavoriteGroupRecord updated = dao.findByPlayerAndGroup("player-1", "1");
        assertNotNull(updated);
        assertEquals("New Name", updated.getGroupName());
    }

    @Test
    void updateName_returnsZeroForUnknownId() {
        assertEquals(0, dao.updateName(999999L, "irrelevant"));
    }

    @Test
    void delete_removesRow() {
        long id = dao.insert(group("player-1", "1", "To Delete"));

        int rows = dao.delete(id);

        assertEquals(1, rows);
        assertTrue(dao.findByPlayer("player-1").isEmpty());
    }

    @Test
    void delete_returnsZeroForUnknownId() {
        assertEquals(0, dao.delete(999999L));
    }

    @Test
    void countByPlayer_countsOnlyTargetPlayer() {
        dao.insert(group("player-A", "1", "G1"));
        dao.insert(group("player-A", "2", "G2"));
        dao.insert(group("player-A", "3", "G3"));
        dao.insert(group("player-B", "1", "G1"));

        assertEquals(3, dao.countByPlayer("player-A"));
        assertEquals(1, dao.countByPlayer("player-B"));
        assertEquals(0, dao.countByPlayer("nobody"));
    }

    // -------------------------------------------------------------------------

    private static FavoriteGroupRecord group(String playerId, String groupNumber, String groupName) {
        FavoriteGroupRecord r = new FavoriteGroupRecord();
        r.setPlayerId(playerId);
        r.setGroupNumber(groupNumber);
        r.setGroupName(groupName);
        r.setFlags(0);
        return r;
    }
}
