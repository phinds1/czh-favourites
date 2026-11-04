package cz.bsl.favorites.web;

// Grep anchor: favorites

import cz.bsl.czh.favorites.api.FavoriteGroupDto;
import cz.bsl.czh.favorites.api.FavoriteGroupPageDto;
import cz.bsl.czh.favorites.api.FavoriteWagerDto;
import cz.bsl.czh.favorites.api.FavoriteWagerPageDto;
import cz.bsl.czh.favorites.server.PlayerContextResolver;
import cz.bsl.czh.favorites.service.FavoritesService;
import cz.bsl.czh.favorites.service.FavoritesValidator;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Player-facing REST controller for favorite wagers and groups.
 *
 * <p>Base path: {@code /{version}/draw-games/favorite-wagers} — the legacy GIS contract
 * (FavoriteWagerResource; see {@code .requirements/integrations/gateway-paths.md}). The
 * {@code version} path segment is accepted for gateway compatibility and currently ignored.
 * All endpoints require {@code X-Player-Id} header — extracted by {@link PlayerContextResolver}
 * which throws {@link IllegalArgumentException} (→ HTTP 400 via {@link FavoritesErrorHandler})
 * if the header is absent or blank.
 *
 * <p>Error mapping is handled by {@link FavoritesErrorHandler} ({@code @ControllerAdvice}):
 * {@link IllegalArgumentException} → 400, {@link java.util.NoSuchElementException} → 404.
 */
@RestController
@RequestMapping("/{version}/draw-games/favorite-wagers")
public class FavoritesController {

    private final FavoritesService service;
    private final PlayerContextResolver playerContext;
    private final FavoritesValidator validator;

    public FavoritesController(FavoritesService service, PlayerContextResolver playerContext, FavoritesValidator validator) {
        this.service       = service;
        this.playerContext = playerContext;
        this.validator     = validator;
    }

    // -------------------------------------------------------------------------
    // Wager endpoints
    // -------------------------------------------------------------------------

    /** POST /{version}/draw-games/favorite-wagers/wagers → 200 (legacy contract: 200 + body with string id) */
    @PostMapping("/wagers")
    public FavoriteWagerDto createWager(
            @PathVariable("version") String version,
            @RequestBody FavoriteWagerDto dto,
            HttpServletRequest request) {
        String playerId = playerContext.resolve(request);
        return service.createWager(dto, playerId);
    }

    /** GET /{version}/draw-games/favorite-wagers/wagers/{id} → 200 */
    @GetMapping("/wagers/{id}")
    public FavoriteWagerDto getWager(
            @PathVariable("version") String version,
            @PathVariable String id,
            HttpServletRequest request) {
        String playerId = playerContext.resolve(request);
        return service.getWager(validator.validateId(id), playerId);
    }

    /** PUT /{version}/draw-games/favorite-wagers/wagers/{id} → 200 */
    @PutMapping("/wagers/{id}")
    public FavoriteWagerDto updateWager(
            @PathVariable("version") String version,
            @PathVariable String id,
            @RequestBody FavoriteWagerDto dto,
            HttpServletRequest request) {
        String playerId = playerContext.resolve(request);
        return service.updateWager(validator.validateId(id), dto, playerId);
    }

    /** DELETE /{version}/draw-games/favorite-wagers/wagers/{id} → 204 No Content */
    @DeleteMapping("/wagers/{id}")
    public ResponseEntity<Void> deleteWager(
            @PathVariable("version") String version,
            @PathVariable String id,
            HttpServletRequest request) {
        String playerId = playerContext.resolve(request);
        service.deleteWager(validator.validateId(id), playerId);
        return ResponseEntity.noContent().build();
    }

    /** GET /{version}/draw-games/favorite-wagers/wagers?group={groupNumber}&game-names={csv} → 200 */
    @GetMapping("/wagers")
    public FavoriteWagerPageDto listWagers(
            @PathVariable("version") String version,
            @RequestParam(name = "group", required = false) String group,
            @RequestParam(name = "game-names", required = false) String gameNamesCsv,
            HttpServletRequest request) {
        String playerId = playerContext.resolve(request);
        List<String> gameNames = FavoritesValidator.parseGameNamesCsv(gameNamesCsv);
        return service.listWagers(playerId, group, gameNames);
    }

    // -------------------------------------------------------------------------
    // Group endpoints
    // -------------------------------------------------------------------------

    /** POST /{version}/draw-games/favorite-wagers/groups → 200 (legacy contract: 200 + body with string id) */
    @PostMapping("/groups")
    public FavoriteGroupDto createGroup(
            @PathVariable("version") String version,
            @RequestBody FavoriteGroupDto dto,
            HttpServletRequest request) {
        String playerId = playerContext.resolve(request);
        return service.createGroup(dto, playerId);
    }

    /** GET /{version}/draw-games/favorite-wagers/groups/{id} → 200 ({id} is the group number) */
    @GetMapping("/groups/{id}")
    public FavoriteGroupDto getGroup(
            @PathVariable("version") String version,
            @PathVariable("id") String groupNumber,
            HttpServletRequest request) {
        String playerId = playerContext.resolve(request);
        validator.validateGroupNumber(groupNumber);
        return service.getGroup(groupNumber, playerId);
    }

    /** PUT /{version}/draw-games/favorite-wagers/groups/{id} → 200 */
    @PutMapping("/groups/{id}")
    public FavoriteGroupDto updateGroup(
            @PathVariable("version") String version,
            @PathVariable("id") String groupNumber,
            @RequestBody FavoriteGroupDto dto,
            HttpServletRequest request) {
        String playerId = playerContext.resolve(request);
        validator.validateGroupNumber(groupNumber);
        return service.updateGroup(groupNumber, dto, playerId);
    }

    /** DELETE /{version}/draw-games/favorite-wagers/groups/{id} → 204 No Content */
    @DeleteMapping("/groups/{id}")
    public ResponseEntity<Void> deleteGroup(
            @PathVariable("version") String version,
            @PathVariable("id") String groupNumber,
            HttpServletRequest request) {
        String playerId = playerContext.resolve(request);
        validator.validateGroupNumber(groupNumber);
        service.deleteGroup(groupNumber, playerId);
        return ResponseEntity.noContent().build();
    }

    /** GET /{version}/draw-games/favorite-wagers/groups → 200 */
    @GetMapping("/groups")
    public FavoriteGroupPageDto listGroups(
            @PathVariable("version") String version,
            HttpServletRequest request) {
        String playerId = playerContext.resolve(request);
        return service.listGroups(playerId);
    }
}
