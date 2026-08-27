package cz.bsl.czh.favourites.server;

// Grep anchor: favourites

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CdcReaderTest {

    private CdcReader cdcReader;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        // Construct with a path that will not exist — individual tests override as needed
        cdcReader = new CdcReader("/nonexistent/path/cdc");
    }

    @Test
    void read_fileAbsent_returnsZero() {
        cdcReader.setCdcPath(tempDir.resolve("cdc-missing"));

        int result = cdcReader.read();

        assertEquals(0, result);
    }

    @Test
    void read_filePresent_returnsInteger() throws IOException {
        Path cdcFile = tempDir.resolve("cdc");
        Files.writeString(cdcFile, "42\n");
        cdcReader.setCdcPath(cdcFile);

        int result = cdcReader.read();

        assertEquals(42, result);
    }

    @Test
    void read_filePresentNoNewline_returnsInteger() throws IOException {
        Path cdcFile = tempDir.resolve("cdc-no-newline");
        Files.writeString(cdcFile, "7");
        cdcReader.setCdcPath(cdcFile);

        int result = cdcReader.read();

        assertEquals(7, result);
    }
}
