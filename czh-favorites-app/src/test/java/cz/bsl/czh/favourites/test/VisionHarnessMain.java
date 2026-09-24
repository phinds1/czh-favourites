package cz.bsl.czh.favourites.test;

// Grep anchor: favourites

import cz.bsl.favourites.FavouritesApplication;

/**
 * Standalone entry point for the {@code test/vision-test.sh} harness. Boots the app-under-test
 * ({@link FavouritesApplication}) on the fixed prod ports — app 9290, management 9291 — with the
 * {@code test} profile (HSQLDB in-memory). The harness starts nginx in front and asserts the GUI
 * works through the proxy.
 *
 * <p>This exists because a bare {@code java -jar} of the exec jar cannot run: production uses DB2,
 * while the harness needs the test classpath (HSQLDB + the test schema). Launching
 * {@link FavouritesApplication} alone is not enough either — the test schema is normally loaded by
 * {@code TestDatabaseConfig} (a {@code @TestConfiguration} imported via {@code @SpringBootTest}),
 * which does not apply when booting via {@code main()}. So this main sets
 * {@code spring.sql.init.mode=always} + the schema location to create the HSQLDB tables at startup.
 *
 * <p>Run via the app module's test classpath (built by the harness with
 * {@code dependency:build-classpath} + {@code target/test-classes}):
 * <pre>
 *   java -cp &lt;test cp&gt; cz.bsl.czh.favourites.test.VisionHarnessMain \
 *     --server.port=9290 --management.server.port=9291 --spring.profiles.active=test
 * </pre>
 */
public final class VisionHarnessMain {

    private VisionHarnessMain() {}

    public static void main(String[] args) {
        // The prod application.yml defines no file appender, but select a sane console level for the
        // harness before Spring Boot initializes logging.
        if (System.getProperty("logging.config") == null) {
            // no logback override needed — the test profile sets root=WARN; rely on that.
        }
        // The test profile's application-test.yml sets management.server.port=0 (random); the harness
        // passes --management.server.port=9291 on the command line, which overrides it. Load the test
        // schema the way TestDatabaseConfig would, since @TestConfiguration does not apply via main().
        System.setProperty("spring.sql.init.mode", "always");
        System.setProperty("spring.sql.init.schema-locations",
                "classpath:dao-test-schema.sql");
        FavouritesApplication.main(args);
    }
}
