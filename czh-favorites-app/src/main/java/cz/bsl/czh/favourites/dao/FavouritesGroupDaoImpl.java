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
 * {@link JdbcTemplate} implementation of {@link FavouritesGroupDao}.
 * SQL as named constants; no string concatenation; parameterised queries only.
 *
 * <p>{@code CREATED_AT} and {@code UPDATED_AT} are {@code NOT NULL DEFAULT CURRENT_TIMESTAMP}
 * so {@code rs.getTimestamp(...).toInstant()} is safe — no null check required.
 *
 * <p>Grep anchor: favourites
 */
@Repository
public class FavouritesGroupDaoImpl implements FavouritesGroupDao {

    private static final String INSERT =
            "INSERT INTO GIS_FAV_GROUP (PLAYER_ID, GROUP_NUMBER, GROUP_NAME, FLAGS) VALUES (?,?,?,?)";

    private static final String FIND_BY_PLAYER =
            "SELECT * FROM GIS_FAV_GROUP WHERE PLAYER_ID = ? ORDER BY GROUP_NUMBER";

    private static final String FIND_BY_PLAYER_AND_GROUP =
            "SELECT * FROM GIS_FAV_GROUP WHERE PLAYER_ID = ? AND GROUP_NUMBER = ?";

    private static final String UPDATE_NAME =
            "UPDATE GIS_FAV_GROUP SET GROUP_NAME=?, UPDATED_AT=CURRENT_TIMESTAMP WHERE FAV_GROUP_ID=?";

    private static final String DELETE =
            "DELETE FROM GIS_FAV_GROUP WHERE FAV_GROUP_ID=?";

    private static final String COUNT_BY_PLAYER =
            "SELECT COUNT(*) FROM GIS_FAV_GROUP WHERE PLAYER_ID=?";

    private final JdbcTemplate jdbcTemplate;
    private final GroupRowMapper rowMapper = new GroupRowMapper();

    public FavouritesGroupDaoImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public long insert(FavouriteGroupRecord group) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(conn -> {
            PreparedStatement ps = conn.prepareStatement(INSERT, new String[]{"FAV_GROUP_ID"});
            ps.setString(1, group.getPlayerId());
            ps.setString(2, group.getGroupNumber());
            ps.setString(3, group.getGroupName());
            ps.setInt(4, group.getFlags());
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Generated FAV_GROUP_ID not returned");
        }
        long id = key.longValue();
        group.setFavGroupId(id);
        return id;
    }

    @Override
    public List<FavouriteGroupRecord> findByPlayer(String playerId) {
        return jdbcTemplate.query(FIND_BY_PLAYER, rowMapper, playerId);
    }

    @Override
    public FavouriteGroupRecord findByPlayerAndGroup(String playerId, String groupNumber) {
        try {
            return jdbcTemplate.queryForObject(FIND_BY_PLAYER_AND_GROUP, rowMapper, playerId, groupNumber);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    @Override
    public int updateName(long favGroupId, String groupName) {
        return jdbcTemplate.update(UPDATE_NAME, groupName, favGroupId);
    }

    @Override
    public int delete(long favGroupId) {
        return jdbcTemplate.update(DELETE, favGroupId);
    }

    @Override
    public int countByPlayer(String playerId) {
        Integer count = jdbcTemplate.queryForObject(COUNT_BY_PLAYER, Integer.class, playerId);
        return count == null ? 0 : count;
    }

    // -------------------------------------------------------------------------

    static class GroupRowMapper implements RowMapper<FavouriteGroupRecord> {
        @Override
        public FavouriteGroupRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
            FavouriteGroupRecord r = new FavouriteGroupRecord();
            r.setFavGroupId(rs.getLong("FAV_GROUP_ID"));
            r.setPlayerId(rs.getString("PLAYER_ID"));
            r.setGroupNumber(rs.getString("GROUP_NUMBER"));
            r.setGroupName(rs.getString("GROUP_NAME"));
            r.setFlags(rs.getInt("FLAGS"));
            r.setCreatedAt(rs.getTimestamp("CREATED_AT").toInstant());
            r.setUpdatedAt(rs.getTimestamp("UPDATED_AT").toInstant());
            return r;
        }
    }
}
