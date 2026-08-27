package cz.bsl.czh.favourites.server;

// Grep anchor: favourites

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Reads the current draw counter from the CDC file (default {@code /run/mx/cdc}).
 *
 * <p>The file contains a single ASCII integer optionally followed by a newline. Returns 0 when
 * the file is absent (test environments, developer machines).
 *
 * <p>The path is configurable via {@code favourites.cdc-path} so integration tests can inject a
 * temporary path without touching {@code /run/mx/cdc}.
 */
@Component
public class CdcReader {

    private Path cdcPath;

    public CdcReader(@Value("${favourites.cdc-path:/run/mx/cdc}") String cdcPath) {
        this.cdcPath = Path.of(cdcPath);
    }

    /** Read the CDC counter. Returns 0 when the file is absent or unreadable. */
    public int read() {
        if (!Files.exists(cdcPath)) {
            return 0;
        }
        try {
            return Integer.parseInt(Files.readString(cdcPath).strip());
        } catch (IOException | NumberFormatException e) {
            return 0;
        }
    }

    /** Package-private — allows tests to override the path without Spring context. */
    void setCdcPath(Path cdcPath) {
        this.cdcPath = cdcPath;
    }
}
