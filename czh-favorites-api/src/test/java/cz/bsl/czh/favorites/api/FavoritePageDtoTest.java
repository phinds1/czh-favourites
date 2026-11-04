package cz.bsl.czh.favorites.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Jackson round-trip tests for {@link FavoriteWagerPageDto} and {@link FavoriteGroupPageDto}.
 *
 * <p>The list wrappers use the legacy front-end collection field names
 * ({@code favoriteWagers} / {@code favoriteWagerGroups}) plus the legacy paging
 * fields; {@code totalCount} is the favorites-only additive extension.
 *
 * <p>Grep anchor: favorites
 */
class FavoritePageDtoTest {

    private static ObjectMapper mapper;

    @BeforeAll
    static void setUp() {
        mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    // --- FavoriteWagerPageDto ---

    @Test
    void wagerPage_roundTrip() throws Exception {
        FavoriteWagerDto w1 = new FavoriteWagerDto();
        w1.setId("1");
        w1.setPlayerId("player-1");
        w1.setName("My Sportka");
        FavoriteWagerDto w2 = new FavoriteWagerDto();
        w2.setId("2");
        w2.setPlayerId("player-1");
        w2.setName("Keno Favorite");

        FavoriteWagerPageDto original = new FavoriteWagerPageDto(Arrays.asList(w1, w2), 2);

        String json = mapper.writeValueAsString(original);
        FavoriteWagerPageDto result = mapper.readValue(json, FavoriteWagerPageDto.class);

        assertEquals(2, result.getTotalCount());
        assertEquals(2, result.getFavoriteWagers().size());
        assertEquals("1", result.getFavoriteWagers().get(0).getId());
        assertEquals("My Sportka", result.getFavoriteWagers().get(0).getName());
        assertEquals("Keno Favorite", result.getFavoriteWagers().get(1).getName());
    }

    @Test
    void wagerPage_usesLegacyWireNames() throws Exception {
        FavoriteWagerPageDto original = new FavoriteWagerPageDto(
                Collections.singletonList(new FavoriteWagerDto()), 1);

        JsonNode tree = mapper.readTree(mapper.writeValueAsString(original));

        assertTrue(tree.has("favoriteWagers"), "legacy front end reads the items from 'favoriteWagers'");
        assertFalse(tree.has("items"), "'items' is not a legacy wire field");
        assertTrue(tree.has("totalCount"));
        assertFalse(tree.has("nextPageUrl"), "unset paging fields must be omitted");
    }

    @Test
    void wagerPage_pagingFieldsRoundTrip() throws Exception {
        Map<Integer, String> pageUrls = new HashMap<>();
        pageUrls.put(1, "/wagers?page=1");
        pageUrls.put(2, "/wagers?page=2");

        FavoriteWagerPageDto original = new FavoriteWagerPageDto(Collections.emptyList(), 0);
        original.setNextPageUrl("/wagers?page=2");
        original.setPreviousPageUrl("/wagers?page=1");
        original.setPageUrls(pageUrls);
        original.setNextItems(5);
        original.setPreviousItems(3);

        String json = mapper.writeValueAsString(original);
        FavoriteWagerPageDto result = mapper.readValue(json, FavoriteWagerPageDto.class);

        assertEquals("/wagers?page=2", result.getNextPageUrl());
        assertEquals("/wagers?page=1", result.getPreviousPageUrl());
        assertEquals(pageUrls, result.getPageUrls());
        assertEquals(Integer.valueOf(5), result.getNextItems());
        assertEquals(Integer.valueOf(3), result.getPreviousItems());
    }

    @Test
    void wagerPage_emptyRoundTrip() throws Exception {
        FavoriteWagerPageDto original = new FavoriteWagerPageDto(Collections.emptyList(), 0);

        String json = mapper.writeValueAsString(original);
        FavoriteWagerPageDto result = mapper.readValue(json, FavoriteWagerPageDto.class);

        assertEquals(0, result.getTotalCount());
        assertNotNull(result.getFavoriteWagers());
        assertTrue(result.getFavoriteWagers().isEmpty());
    }

    // --- FavoriteGroupPageDto ---

    @Test
    void groupPage_roundTrip() throws Exception {
        FavoriteGroupDto g1 = new FavoriteGroupDto("1", "Alpha");
        FavoriteGroupDto g2 = new FavoriteGroupDto("2", "Beta");

        FavoriteGroupPageDto original = new FavoriteGroupPageDto(Arrays.asList(g1, g2), 2);

        String json = mapper.writeValueAsString(original);
        FavoriteGroupPageDto result = mapper.readValue(json, FavoriteGroupPageDto.class);

        assertEquals(2, result.getTotalCount());
        assertEquals(2, result.getFavoriteWagerGroups().size());
        assertEquals("Alpha", result.getFavoriteWagerGroups().get(0).getName());
        assertEquals("Beta", result.getFavoriteWagerGroups().get(1).getName());
    }

    @Test
    void groupPage_usesLegacyWireNames() throws Exception {
        FavoriteGroupPageDto original = new FavoriteGroupPageDto(
                Collections.singletonList(new FavoriteGroupDto()), 1);

        JsonNode tree = mapper.readTree(mapper.writeValueAsString(original));

        assertTrue(tree.has("favoriteWagerGroups"),
                "legacy front end reads the items from 'favoriteWagerGroups'");
        assertFalse(tree.has("items"), "'items' is not a legacy wire field");
        assertTrue(tree.has("totalCount"));
        assertFalse(tree.has("nextPageUrl"), "unset paging fields must be omitted");
    }

    @Test
    void groupPage_emptyRoundTrip() throws Exception {
        FavoriteGroupPageDto original = new FavoriteGroupPageDto(Collections.emptyList(), 0);

        String json = mapper.writeValueAsString(original);
        FavoriteGroupPageDto result = mapper.readValue(json, FavoriteGroupPageDto.class);

        assertEquals(0, result.getTotalCount());
        assertNotNull(result.getFavoriteWagerGroups());
        assertTrue(result.getFavoriteWagerGroups().isEmpty());
    }
}
