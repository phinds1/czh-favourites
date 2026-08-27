package cz.bsl.favourites.web;

// Grep anchor: favourites

import cz.bsl.czh.favourites.api.FavouriteGroupDto;
import cz.bsl.czh.favourites.api.FavouriteGroupPageDto;
import cz.bsl.czh.favourites.api.FavouriteWagerDto;
import cz.bsl.czh.favourites.api.FavouriteWagerPageDto;
import cz.bsl.czh.favourites.server.PlayerContextResolver;
import cz.bsl.czh.favourites.service.FavouritesService;
import cz.bsl.czh.favourites.service.FavouritesValidator;
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

import java.net.URI;
import java.util.Arrays;
import java.util.List;

/**
 * Player-facing REST controller for favourite wagers and groups.
 *
 * <p>Base path: {@code /favourites}. All endpoints require {@code X-Player-Id} header —
 * extracted by {@link PlayerContextResolver} which throws {@link IllegalArgumentException}
 * (→ HTTP 400 via {@link FavouritesErrorHandler}) if the header is absent or blank.
 *
 * <p>Error mapping is handled by {@link FavouritesErrorHandler} ({@code @ControllerAdvice}):
 * {@link IllegalArgumentException} → 400, {@link java.util.NoSuchElementException} → 404.
 */
@RestController
@RequestMapping("/favourites")
public class FavouritesController {

    private final FavouritesService service;
    private final PlayerContextResolver playerContext;
    private final FavouritesValidator validator;

    public FavouritesController(FavouritesService service,
                                PlayerContextResolver playerContext,
                                FavouritesValidator validator) {
        this.service       = service;
        this.playerContext = playerContext;
        this.validator     = validator;
    }

    // -------------------------------------------------------------------------
    // Wager endpoints
    // -------------------------------------------------------------------------

    /** POST /favourites/wagers → 201 Created */
    @PostMapping("/wagers")
    public ResponseEntity<FavouriteWagerDto> createWager(
            @RequestBody FavouriteWagerDto dto,
            HttpServletRequest request) {
        String playerId = playerContext.resolve(request);
        FavouriteWagerDto created = service.createWager(dto, playerId);
        URI location = URI.create("/favourites/wagers/" + created.getId());
        return ResponseEntity.created(location).body(created);
    }

    /** GET /favourites/wagers/{id} → 200 */
    @GetMapping("/wagers/{id}")
    public FavouriteWagerDto getWager(
            @PathVariable String id,
            HttpServletRequest request) {
        String playerId = playerContext.resolve(request);
        return service.getWager(validator.validateId(id), playerId);
    }

    /** PUT /favourites/wagers/{id} → 200 */
    @PutMapping("/wagers/{id}")
    public FavouriteWagerDto updateWager(
            @PathVariable String id,
            @RequestBody FavouriteWagerDto dto,
            HttpServletRequest request) {
        String playerId = playerContext.resolve(request);
        return service.updateWager(validator.validateId(id), dto, playerId);
    }

    /** DELETE /favourites/wagers/{id} → 204 No Content */
    @DeleteMapping("/wagers/{id}")
    public ResponseEntity<Void> deleteWager(
            @PathVariable String id,
            HttpServletRequest request) {
        String playerId = playerContext.resolve(request);
        service.deleteWager(validator.validateId(id), playerId);
        return ResponseEntity.noContent().build();
    }

    /** GET /favourites/wagers?group={groupNumber}&game-names={csv} → 200 */
    @GetMapping("/wagers")
    public FavouriteWagerPageDto listWagers(
            @RequestParam(name = "group", required = false) String group,
            @RequestParam(name = "game-names", required = false) String gameNamesCsv,
            HttpServletRequest request) {
        String playerId = playerContext.resolve(request);
        List<String> gameNames = (gameNamesCsv == null || gameNamesCsv.isBlank())
                ? List.of()
                : Arrays.stream(gameNamesCsv.split(","))
                        .map(String::strip)
                        .filter(s -> !s.isEmpty())
                        .toList();
        if (gameNames.size() > 50) {
            throw new IllegalArgumentException("game-names query parameter must not exceed 50 entries");
        }
        return service.listWagers(playerId, group, gameNames);
    }

    // -------------------------------------------------------------------------
    // Group endpoints
    // -------------------------------------------------------------------------

    /** POST /favourites/groups → 201 Created */
    @PostMapping("/groups")
    public ResponseEntity<FavouriteGroupDto> createGroup(
            @RequestBody FavouriteGroupDto dto,
            HttpServletRequest request) {
        String playerId = playerContext.resolve(request);
        FavouriteGroupDto created = service.createGroup(dto, playerId);
        URI location = URI.create("/favourites/groups/" + created.getGroupNumber());
        return ResponseEntity.created(location).body(created);
    }

    /** GET /favourites/groups/{groupNumber} → 200 */
    @GetMapping("/groups/{groupNumber}")
    public FavouriteGroupDto getGroup(
            @PathVariable String groupNumber,
            HttpServletRequest request) {
        String playerId = playerContext.resolve(request);
        validator.validateGroupNumber(groupNumber);
        return service.getGroup(groupNumber, playerId);
    }

    /** PUT /favourites/groups/{groupNumber} → 200 */
    @PutMapping("/groups/{groupNumber}")
    public FavouriteGroupDto updateGroup(
            @PathVariable String groupNumber,
            @RequestBody FavouriteGroupDto dto,
            HttpServletRequest request) {
        String playerId = playerContext.resolve(request);
        validator.validateGroupNumber(groupNumber);
        return service.updateGroup(groupNumber, dto, playerId);
    }

    /** DELETE /favourites/groups/{groupNumber} → 204 No Content */
    @DeleteMapping("/groups/{groupNumber}")
    public ResponseEntity<Void> deleteGroup(
            @PathVariable String groupNumber,
            HttpServletRequest request) {
        String playerId = playerContext.resolve(request);
        validator.validateGroupNumber(groupNumber);
        service.deleteGroup(groupNumber, playerId);
        return ResponseEntity.noContent().build();
    }

    /** GET /favourites/groups → 200 */
    @GetMapping("/groups")
    public FavouriteGroupPageDto listGroups(HttpServletRequest request) {
        String playerId = playerContext.resolve(request);
        return service.listGroups(playerId);
    }
}
