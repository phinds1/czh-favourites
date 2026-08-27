package cz.bsl.czh.favourites.server;

// Grep anchor: favourites

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlayerContextResolverTest {

    private PlayerContextResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new PlayerContextResolver();
    }

    @Test
    void resolve_returnsPlayerId() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Player-Id", "player-123");

        String playerId = resolver.resolve(request);

        assertEquals("player-123", playerId);
    }

    @Test
    void resolve_stripsWhitespace() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Player-Id", "  player-456  ");

        String playerId = resolver.resolve(request);

        assertEquals("player-456", playerId);
    }

    @Test
    void resolve_missingHeader_throws() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        assertThrows(IllegalArgumentException.class, () -> resolver.resolve(request));
    }

    @Test
    void resolve_blankHeader_throws() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Player-Id", "   ");

        assertThrows(IllegalArgumentException.class, () -> resolver.resolve(request));
    }

    // Security finding 2: playerId over 64 chars must be rejected
    @Test
    void resolve_oversizedHeader_throws() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Player-Id", "x".repeat(65));

        assertThrows(IllegalArgumentException.class, () -> resolver.resolve(request));
    }

    @Test
    void resolve_exactlyMaxLength_succeeds() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Player-Id", "x".repeat(64));

        String playerId = resolver.resolve(request);

        assertEquals("x".repeat(64), playerId);
    }
}
