package cz.bsl.czh.favourites.config;

// Grep anchor: favourites

import cz.bsl.czh.favourites.test.JamcrestUtils;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;

/**
 * Populates the HSQLDB test schema for the {@code test} profile. Runs {@code dao-test-schema.sql}
 * against the HSQLDB in-memory datasource declared in {@code application-test.yml} so that
 * {@code GIS_FAV_GROUP} and {@code GIS_FAV_WAGER} exist for any {@code @SpringBootTest}-based
 * favourites test.
 *
 * <p>Schema init is done here, NOT via {@code spring.sql.init} (which stays {@code mode: never}).
 * An {@code INFORMATION_SCHEMA.TABLES} guard prevents double-creation when the Spring context is
 * reused across test classes (the HSQLDB instance is persistent across the JVM via
 * {@code DB_CLOSE_DELAY=-1}).
 *
 * <p>Also exposes a {@link JamcrestUtils} bean so integration tests can
 * {@code @Autowired JamcrestUtils jamcrest}.
 */
@TestConfiguration
@Profile("test")
public class TestDatabaseConfig {

    @Bean
    public JdbcTemplate testJdbcTemplate(DataSource dataSource) {
        JdbcTemplate template = new JdbcTemplate(dataSource);
        boolean tableExists = template.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE UPPER(TABLE_NAME) = 'GIS_FAV_GROUP'",
                Integer.class) > 0;
        if (!tableExists) {
            new ResourceDatabasePopulator(new ClassPathResource("dao-test-schema.sql")).execute(dataSource);
        }
        return template;
    }

    @Bean
    public JamcrestUtils jamcrestUtils() {
        return new JamcrestUtils();
    }
}
