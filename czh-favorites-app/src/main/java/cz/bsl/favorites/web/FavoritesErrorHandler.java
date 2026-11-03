package cz.bsl.favorites.web;

// Grep anchor: favorites

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Translates service exceptions to HTTP error responses for all controllers in
 * {@code cz.bsl.favorites.web}. Returns a consistent JSON body {@code {"error": "<message>"}}.
 *
 * <ul>
 *   <li>{@link IllegalArgumentException} → 400 Bad Request (validation failures, quota exceeded)</li>
 *   <li>{@link NoSuchElementException} → 404 Not Found (wager/group not found or not owned by player)</li>
 * </ul>
 */
@ControllerAdvice
public class FavoritesErrorHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NoSuchElementException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }
}
