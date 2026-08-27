package cz.bsl.favourites.web;

// Grep anchor: favourites

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class FavouritesErrorHandlerTest {

    private FavouritesErrorHandler handler;

    @BeforeEach
    void setUp() {
        handler = new FavouritesErrorHandler();
    }

    @Test
    void handleBadRequest_returns400WithErrorMessage() {
        ResponseEntity<Map<String, String>> response =
                handler.handleBadRequest(new IllegalArgumentException("wagerName too long"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("wagerName too long", response.getBody().get("error"));
    }

    @Test
    void handleNotFound_returns404WithErrorMessage() {
        ResponseEntity<Map<String, String>> response =
                handler.handleNotFound(new NoSuchElementException("Favourite wager not found: 99"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Favourite wager not found: 99", response.getBody().get("error"));
    }
}
