package cz.bsl.czh.favourites.dao;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * {@link JdbcTemplate} implementation of {@link FavouritesWagerDao}.
 * SQL as named constants; no string concatenation; parameterised queries only.
 *
 * <p>{@code WAGER_JSON} is stored and retrieved as a raw {@code String}; the service
 * layer (SP5) owns {@code ObjectMapper} serialisation of {@code WagerDto}.
 *
 * <p>{@code CREATED_AT} and {@code UPDATED_AT} are {@code NOT NULL DEFAULT CURRENT_TIMESTAMP}
 * so {@code rs.getTimestamp(...).toInstant()} is safe — no null check required.
 *
 * <p>Grep anchor: favourites
 */
@Repository
public class FavouritesWagerDaoImpl implements FavouritesWagerDao {

    private static final String INSERT =
            "INSERT INTO GIS_FAV_WAGER " +
            "(PLAYER_ID, GROUP_NUMBER, GAME_NAME, WAGER_NAME, FLAGS, WAGER_JSON) " +
            "VALUES (?,?,?,?,?,?)";

    private static final String FIND_BY_ID =
            "SELECT * FROM GIS_FAV_WAGER WHERE FAV_WAGER_ID=?";

    private static final String FIND_BY_PLAYER =
            "SELECT * FROM GIS_FAV_WAGER WHERE PLAYER_ID=? ORDER BY FAV_WAGER_ID";

    private static final String FIND_BY_PLAYER_AND_GROUP =
            "SELECT * FROM GIS_FAV_WAGER WHERE PLAYER_ID=? AND GROUP_NUMBER=? ORDER BY FAV_WAGER_ID";

    private static final String FIND_BY_PLAYER_AND_GAME =
            "SELECT * FROM GIS_FAV_WAGER WHERE PLAYER_ID=? AND GAME_NAME=? ORDER BY FAV_WAGER_ID";

    private static final String UPDATE =
            "UPDATE GIS_FAV_WAGER SET WAGER_NAME=?, FLAGS=?, WAGER_JSON=?, " +
            "UPDATED_AT=CURRENT_TIMESTAMP WHERE FAV_WAGER_ID=?";

    private static final String DELETE =
            "DELETE FROM GIS_FAV_WAGER WHERE FAV_WAGER_ID=?";

    private static final String COUNT_BY_PLAYER =
            "SELECT COUNT(*) FROM GIS_FAV_WAGER WHERE PLAYER_ID=?";

    private final JdbcTemplate jdbcTemplate;
    private final WagerRowMapper rowMapper = new WagerRowMapper();

    public FavouritesWagerDaoImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public long insert(FavouriteWagerRecord wager) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(conn -> {
            PreparedStatement ps = conn.prepareStatement(INSERT, new String[]{"FAV_WAGER_ID"});
            ps.setString(1, wager.getPlayerId());
            ps.setString(2, wager.getGroupNumber());
            ps.setString(3, wager.getGameName());
            ps.setString(4, wager.getWagerName());
            ps.setInt(5, wager.getFlags());
            ps.setString(6, wager.getWagerJson());
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Generated FAV_WAGER_ID not returned");
        }
        long id = key.longValue();
        wager.setFavWagerId(id);
        return id;
    }

    @Override
    public FavouriteWagerRecord findById(long favWagerId) {
        try {
            return jdbcTemplate.queryForObject(FIND_BY_ID, rowMapper, favWagerId);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    @Override
    public List<FavouriteWagerRecord> findByPlayer(String playerId) {
        return jdbcTemplate.query(FIND_BY_PLAYER, rowMapper, playerId);
    }

    @Override
    public List<FavouriteWagerRecord> findByPlayerAndGroup(String playerId, String groupNumber) {
        return jdbcTemplate.query(FIND_BY_PLAYER_AND_GROUP, rowMapper, playerId, groupNumber);
    }

    @Override
    public List<FavouriteWagerRecord> findByPlayerAndGame(String playerId, String gameName) {
        return jdbcTemplate.query(FIND_BY_PLAYER_AND_GAME, rowMapper, playerId, gameName);
    }

    @Override
    public int update(FavouriteWagerRecord wager) {
        return jdbcTemplate.update(UPDATE,
                wager.getWagerName(),
                wager.getFlags(),
                wager.getWagerJson(),
                wager.getFavWagerId());
    }

    @Override
    public int delete(long favWagerId) {
        return jdbcTemplate.update(DELETE, favWagerId);
    }

    @Override
    public int countByPlayer(String playerId) {
        Integer count = jdbcTemplate.queryForObject(COUNT_BY_PLAYER, Integer.class, playerId);
        return count == null ? 0 : count;
    }

    // -------------------------------------------------------------------------

    static class WagerRowMapper implements RowMapper<FavouriteWagerRecord> {
        @Override
        public FavouriteWagerRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
            FavouriteWagerRecord r = new FavouriteWagerRecord();
            r.setFavWagerId(rs.getLong("FAV_WAGER_ID"));
            r.setPlayerId(rs.getString("PLAYER_ID"));
            r.setGroupNumber(rs.getString("GROUP_NUMBER"));
            r.setGameName(rs.getString("GAME_NAME"));
            r.setWagerName(rs.getString("WAGER_NAME"));
            r.setFlags(rs.getInt("FLAGS"));
            r.setCreatedAt(rs.getTimestamp("CREATED_AT").toInstant());
            r.setUpdatedAt(rs.getTimestamp("UPDATED_AT").toInstant());
            r.setWagerJson(rs.getString("WAGER_JSON"));
            return r;
        }
    }
}
