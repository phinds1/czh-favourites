package cz.bsl.favorites;

import cz.bsl.favorites.config.FavoritesProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

// Verifies the Spring context loads cleanly against HSQLDB and FavoritesProperties binds correctly.
@SpringBootTest
@ActiveProfiles("test")
class FavoritesApplicationSmokeTest {

    @Autowired
    private FavoritesProperties properties;

    @Test
    void contextLoads() {
        assertNotNull(properties);
    }

    @Test
    void defaultPropertiesAreBound() {
        assertEquals(1,   properties.getMinGroupIndex());
        assertEquals(10,  properties.getMaxGroupIndex());
        assertEquals(50,  properties.getMaxFavorites());
        assertEquals(200, properties.getMaxFavoriteBoards());
    }
}
