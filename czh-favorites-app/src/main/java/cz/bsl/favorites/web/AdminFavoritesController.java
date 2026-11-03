package cz.bsl.favorites.web;

// Grep anchor: favorites

import cz.bsl.czh.favorites.api.FavoriteGroupDto;
import cz.bsl.czh.favorites.api.FavoriteGroupPageDto;
import cz.bsl.czh.favorites.api.FavoriteWagerDto;
import cz.bsl.czh.favorites.api.FavoriteWagerPageDto;
import cz.bsl.czh.favorites.service.FavoritesService;
import cz.bsl.czh.favorites.service.FavoritesValidator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.NoSuchElementException;

/**
 * Read-only admin view of a player's favorite wagers and groups.
 *
 * <p><b>Security boundary:</b> {@code playerId} is a trusted path parameter — this endpoint is
 * reachable only via the admin gateway tier which enforces operator-level credentials. No
 * {@code X-Player-Id} header is validated here. Mutation operations (POST/PUT/DELETE) are
 * intentionally absent; use {@link FavoritesController} for player-facing CRUD.
 *
 * <p>Admin callers may query any player id and any group number (including out-of-range ones)
 * for diagnostic purposes; group-number range validation is therefore intentionally skipped here.
 * The service returns a {@link NoSuchElementException} (→ 404) if the resource does not exist.
 *
 * <p>Base path: {@code /admin/{version}/player/{playerId}/draw-games/favorite-wagers} — the legacy
 * GIS contract (AdminFavoriteWagerResource). The {@code version} segment is accepted for gateway
 * compatibility and currently ignored. The list-groups endpoint is an addition beyond the legacy
 * reference, required by the Vision GUI Player Lookup tab.
 *
 * <p>Error handling is provided by {@link FavoritesErrorHandler} ({@code @ControllerAdvice}):
 * {@link IllegalArgumentException} → 400, {@link NoSuchElementException} → 404.
 */
@RestController
@RequestMapping("/admin")
public class AdminFavoritesController {

    private final FavoritesService service;
    private final FavoritesValidator validator;

    public AdminFavoritesController(FavoritesService service, FavoritesValidator validator) {
        this.service   = service;
        this.validator = validator;
    }

    /**
     * GET /admin/{version}/player/{playerId}/draw-games/favorite-wagers/wagers?groupNumber={g}&game-names={csv} → 200.
     * Both filters are optional; with none, all of the player's wagers are listed.
     */
    @GetMapping("/{version}/player/{playerId}/draw-games/favorite-wagers/wagers")
    public FavoriteWagerPageDto listWagers(
            @PathVariable("version") String version,
            @PathVariable("playerId") String playerId,
            @RequestParam(name = "groupNumber", required = false) String groupNumber,
            @RequestParam(name = "game-names", required = false) String gameNamesCsv) {
        validatePlayerId(playerId);
        String group = null;
        if (groupNumber != null) {
            // Format-only validation — admin may query any group number for diagnostics
            group = String.valueOf(validator.validateId(groupNumber));
        }
        return service.listWagers(playerId, group, FavoritesValidator.parseGameNamesCsv(gameNamesCsv));
    }

    /** GET /admin/{version}/player/{playerId}/draw-games/favorite-wagers/wagers/{wagerId} → 200 */
    @GetMapping("/{version}/player/{playerId}/draw-games/favorite-wagers/wagers/{wagerId}")
    public FavoriteWagerDto getWager(
            @PathVariable("version") String version,
            @PathVariable("playerId") String playerId,
            @PathVariable("wagerId") String wagerId) {
        validatePlayerId(playerId);
        return service.getWager(validator.validateId(wagerId), playerId);
    }

    /** GET /admin/{version}/player/{playerId}/draw-games/favorite-wagers/groups → 200 (retained extra, not in the legacy reference) */
    @GetMapping("/{version}/player/{playerId}/draw-games/favorite-wagers/groups")
    public FavoriteGroupPageDto listGroups(
            @PathVariable("version") String version,
            @PathVariable("playerId") String playerId) {
        validatePlayerId(playerId);
        return service.listGroups(playerId);
    }

    /** GET /admin/{version}/player/{playerId}/draw-games/favorite-wagers/groups/{groupNumber} → 200 */
    @GetMapping("/{version}/player/{playerId}/draw-games/favorite-wagers/groups/{groupNumber}")
    public FavoriteGroupDto getGroup(
            @PathVariable("version") String version,
            @PathVariable("playerId") String playerId,
            @PathVariable("groupNumber") String groupNumber) {
        validatePlayerId(playerId);
        // No range validation — admin may query any group number for diagnostics
        return service.getGroup(groupNumber, playerId);
    }

    private static void validatePlayerId(String playerId) {
        if (playerId == null || playerId.isBlank()) {
            throw new IllegalArgumentException("playerId must not be blank");
        }
        if (playerId.length() > 64) {
            throw new IllegalArgumentException("playerId path variable exceeds maximum length of 64 characters");
        }
    }
}
