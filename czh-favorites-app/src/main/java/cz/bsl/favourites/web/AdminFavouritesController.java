package cz.bsl.favourites.web;

// Grep anchor: favourites

import cz.bsl.czh.favourites.api.FavouriteGroupDto;
import cz.bsl.czh.favourites.api.FavouriteGroupPageDto;
import cz.bsl.czh.favourites.api.FavouriteWagerDto;
import cz.bsl.czh.favourites.api.FavouriteWagerPageDto;
import cz.bsl.czh.favourites.service.FavouritesService;
import cz.bsl.czh.favourites.service.FavouritesValidator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Read-only admin view of a player's favourite wagers and groups.
 *
 * <p><b>Security boundary:</b> {@code playerId} is a trusted path parameter — this endpoint is
 * reachable only via the admin gateway tier which enforces operator-level credentials. No
 * {@code X-Player-Id} header is validated here. Mutation operations (POST/PUT/DELETE) are
 * intentionally absent; use {@link FavouritesController} for player-facing CRUD.
 *
 * <p>Admin callers may query any player id and any group number (including out-of-range ones)
 * for diagnostic purposes; group-number range validation is therefore intentionally skipped here.
 * The service returns a {@link NoSuchElementException} (→ 404) if the resource does not exist.
 *
 * <p>Error handling is provided by {@link FavouritesErrorHandler} ({@code @ControllerAdvice}):
 * {@link IllegalArgumentException} → 400, {@link NoSuchElementException} → 404.
 */
@RestController
@RequestMapping("/admin/favourites")
public class AdminFavouritesController {

    private final FavouritesService service;
    private final FavouritesValidator validator;

    public AdminFavouritesController(FavouritesService service, FavouritesValidator validator) {
        this.service   = service;
        this.validator = validator;
    }

    /** GET /admin/favourites/players/{playerId}/wagers → 200 */
    @GetMapping("/players/{playerId}/wagers")
    public FavouriteWagerPageDto listWagers(@PathVariable String playerId) {
        validatePlayerId(playerId);
        return service.listWagers(playerId, null, List.of());
    }

    /** GET /admin/favourites/players/{playerId}/wagers/{id} → 200 */
    @GetMapping("/players/{playerId}/wagers/{id}")
    public FavouriteWagerDto getWager(
            @PathVariable String playerId,
            @PathVariable String id) {
        validatePlayerId(playerId);
        return service.getWager(validator.validateId(id), playerId);
    }

    /** GET /admin/favourites/players/{playerId}/groups → 200 */
    @GetMapping("/players/{playerId}/groups")
    public FavouriteGroupPageDto listGroups(@PathVariable String playerId) {
        validatePlayerId(playerId);
        return service.listGroups(playerId);
    }

    /** GET /admin/favourites/players/{playerId}/groups/{groupNumber} → 200 */
    @GetMapping("/players/{playerId}/groups/{groupNumber}")
    public FavouriteGroupDto getGroup(
            @PathVariable String playerId,
            @PathVariable String groupNumber) {
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
