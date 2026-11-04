package cz.bsl.czh.favorites.dao;

import tools.jackson.databind.ObjectMapper;
import cz.bsl.czh.favorites.api.WagerDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit 5 tests for {@link FavoritesWagerDaoImpl} against HSQLDB in-memory.
 *
 * <p>Grep anchor: favorites
 */
@SpringJUnitConfig(DaoTestConfig.class)
class FavoritesWagerDaoTest {

    @Autowired
    private FavoritesWagerDao dao;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM GIS_FAV_WAGER");
    }

    // -------------------------------------------------------------------------

    @Test
    void insert_generatesId() throws Exception {
        FavoriteWagerRecord r = wager("player-1", "1", "LOTTO", "Lucky", wagerJson("LOTTO"));
        long id = dao.insert(r);
        assertTrue(id > 0);
        assertEquals(id, r.getFavWagerId());
    }

    @Test
    void findById_returnsWager() throws Exception {
        String json = wagerJson("KENO");
        FavoriteWagerRecord r = wager("player-1", "2", "KENO", "My KENO", json);
        long id = dao.insert(r);

        FavoriteWagerRecord found = dao.findById(id);

        assertNotNull(found);
        assertEquals("player-1", found.getPlayerId());
        assertEquals("2", found.getGroupNumber());
        assertEquals("KENO", found.getGameName());
        assertEquals("My KENO", found.getWagerName());
        assertEquals(json, found.getWagerJson());
        assertNotNull(found.getCreatedAt());
        assertNotNull(found.getUpdatedAt());
    }

    @Test
    void findById_returnsNullWhenAbsent() {
        assertNull(dao.findById(999999L));
    }

    @Test
    void findByPlayer_returnsAllOrderedById() throws Exception {
        dao.insert(wager("player-1", "1", "LOTTO", "W1", wagerJson("LOTTO")));
        dao.insert(wager("player-1", "1", "KENO",  "W2", wagerJson("KENO")));

        List<FavoriteWagerRecord> results = dao.findByPlayer("player-1");

        assertEquals(2, results.size());
        assertTrue(results.get(0).getFavWagerId() < results.get(1).getFavWagerId());
    }

    @Test
    void findByPlayerAndGroup_filtersCorrectly() throws Exception {
        dao.insert(wager("player-1", "1", "LOTTO", "W1", wagerJson("LOTTO")));
        dao.insert(wager("player-1", "2", "LOTTO", "W2", wagerJson("LOTTO")));
        dao.insert(wager("player-1", "1", "KENO",  "W3", wagerJson("KENO")));

        List<FavoriteWagerRecord> group1 = dao.findByPlayerAndGroup("player-1", "1");
        List<FavoriteWagerRecord> group2 = dao.findByPlayerAndGroup("player-1", "2");

        assertEquals(2, group1.size());
        assertEquals(1, group2.size());
        assertEquals("W2", group2.get(0).getWagerName());
    }

    @Test
    void findByPlayerAndGame_filtersCorrectly() throws Exception {
        dao.insert(wager("player-1", "1", "LOTTO", "L1", wagerJson("LOTTO")));
        dao.insert(wager("player-1", "1", "LOTTO", "L2", wagerJson("LOTTO")));
        dao.insert(wager("player-1", "1", "KENO",  "K1", wagerJson("KENO")));

        List<FavoriteWagerRecord> lottos = dao.findByPlayerAndGame("player-1", "LOTTO");
        List<FavoriteWagerRecord> kenos  = dao.findByPlayerAndGame("player-1", "KENO");

        assertEquals(2, lottos.size());
        assertEquals(1, kenos.size());
    }

    @Test
    void update_changesFields() throws Exception {
        String originalJson = wagerJson("LOTTO");
        FavoriteWagerRecord r = wager("player-1", "1", "LOTTO", "Original", originalJson);
        long id = dao.insert(r);

        Instant insertedAt = dao.findById(id).getUpdatedAt();

        String updatedJson = wagerJson("LOTTO_UPDATED");
        r.setWagerName("Updated");
        r.setFlags(1);
        r.setWagerJson(updatedJson);

        int rows = dao.update(r);

        assertEquals(1, rows);
        FavoriteWagerRecord updated = dao.findById(id);
        assertNotNull(updated);
        assertEquals("Updated", updated.getWagerName());
        assertEquals(1, updated.getFlags());
        assertEquals(updatedJson, updated.getWagerJson());
        assertFalse(updated.getUpdatedAt().isBefore(insertedAt));
    }

    @Test
    void update_returnsZeroForUnknownId() throws Exception {
        FavoriteWagerRecord ghost = wager("p", "1", "LOTTO", "x", wagerJson("LOTTO"));
        ghost.setFavWagerId(999999L);
        assertEquals(0, dao.update(ghost));
    }

    @Test
    void delete_removesRow() throws Exception {
        long id = dao.insert(wager("player-1", "1", "LOTTO", "ToDelete", wagerJson("LOTTO")));

        assertEquals(1, dao.delete(id));
        assertNull(dao.findById(id));
    }

    @Test
    void delete_returnsZeroForUnknownId() {
        assertEquals(0, dao.delete(999999L));
    }

    @Test
    void countByPlayer_countsOnlyTargetPlayer() throws Exception {
        dao.insert(wager("player-A", "1", "LOTTO", "W1", wagerJson("LOTTO")));
        dao.insert(wager("player-A", "1", "KENO",  "W2", wagerJson("KENO")));
        dao.insert(wager("player-A", "2", "LOTTO", "W3", wagerJson("LOTTO")));
        dao.insert(wager("player-A", "2", "KENO",  "W4", wagerJson("KENO")));
        dao.insert(wager("player-B", "1", "LOTTO", "W5", wagerJson("LOTTO")));

        assertEquals(4, dao.countByPlayer("player-A"));
        assertEquals(1, dao.countByPlayer("player-B"));
        assertEquals(0, dao.countByPlayer("nobody"));
    }

    @Test
    void wagerJsonRoundTrip() throws Exception {
        WagerDto original = new WagerDto();
        original.setGameName("EUROJACKPOT");
        original.setStake(250L);
        original.setPrice(500L);
        original.setDuration(3);
        original.setSerialNumber("SN-42");
        original.setBoards(List.of(
                board("7", "14", "21", "28", "35"),
                board("2", "4", "6", "8", "10")
        ));

        String json = objectMapper.writeValueAsString(original);
        long id = dao.insert(wager("player-1", "1", "EUROJACKPOT", "EJ Fave", json));

        FavoriteWagerRecord record = dao.findById(id);
        assertNotNull(record);

        WagerDto restored = objectMapper.readValue(record.getWagerJson(), WagerDto.class);
        assertEquals(original.getGameName(), restored.getGameName());
        assertEquals(original.getStake(), restored.getStake());
        assertEquals(original.getPrice(), restored.getPrice());
        assertEquals(original.getDuration(), restored.getDuration());
        assertEquals(original.getSerialNumber(), restored.getSerialNumber());
        assertEquals(original.getBoards().size(), restored.getBoards().size());
        assertEquals(original.getBoards().get(0).getSelections(), restored.getBoards().get(0).getSelections());
        assertEquals(original.getBoards().get(1).getSelections(), restored.getBoards().get(1).getSelections());
    }

    private static cz.bsl.czh.favorites.api.BoardDto board(String... selections) {
        cz.bsl.czh.favorites.api.BoardDto board = new cz.bsl.czh.favorites.api.BoardDto();
        board.setSelections(List.of(selections));
        return board;
    }

    // -------------------------------------------------------------------------

    private static FavoriteWagerRecord wager(String playerId, String groupNumber,
                                              String gameName, String wagerName,
                                              String wagerJson) {
        FavoriteWagerRecord r = new FavoriteWagerRecord();
        r.setPlayerId(playerId);
        r.setGroupNumber(groupNumber);
        r.setGameName(gameName);
        r.setWagerName(wagerName);
        r.setFlags(0);
        r.setWagerJson(wagerJson);
        return r;
    }

    private static String wagerJson(String gameName) throws Exception {
        return "{\"gameName\":\"" + gameName + "\",\"stake\":100,\"price\":200," +
               "\"duration\":3,\"serialNumber\":null,\"boards\":[[1,2,3,4,5,6]]}";
    }
}
