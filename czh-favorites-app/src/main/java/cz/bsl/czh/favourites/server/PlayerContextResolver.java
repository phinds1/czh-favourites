package cz.bsl.czh.favourites.server;

// Grep anchor: favourites

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/**
 * Extracts the pre-authenticated player identity from the {@code X-Player-Id} HTTP header.
 * The API gateway authenticates the player before forwarding requests to this service; this
 * component trusts the header.
 *
 * <p>Throws {@link IllegalArgumentException} if the header is absent or blank; the controller
 * maps this to HTTP 400 / 401.
 */
@Component
public class PlayerContextResolver {

    /**
     * Resolve the player id from the {@code X-Player-Id} header.
     *
     * @param request the current HTTP request
     * @return the stripped player id string (never null or blank)
     * @throws IllegalArgumentException if the header is missing or blank
     */
    public String resolve(HttpServletRequest request) {
        String playerId = request.getHeader("X-Player-Id");
        if (playerId == null || playerId.isBlank()) {
            throw new IllegalArgumentException("Missing or blank X-Player-Id header");
        }
        playerId = playerId.strip();
        if (playerId.length() > 64) {
            throw new IllegalArgumentException("X-Player-Id header exceeds maximum length of 64 characters");
        }
        return playerId;
    }
}
