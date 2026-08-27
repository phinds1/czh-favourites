package cz.bsl.favourites.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "favourites")
public class FavouritesProperties {

    private int minGroupIndex = 1;
    private int maxGroupIndex = 10;
    private String defaultGroupName = "";
    private int maxFavorites = 50;
    private int maxFavoriteBoards = 200;
    private int kenoReservedGroupIndex = 1;
    private boolean returnEmptyGroups = false;

    public int getMinGroupIndex() {
        return minGroupIndex;
    }

    public void setMinGroupIndex(int minGroupIndex) {
        this.minGroupIndex = minGroupIndex;
    }

    public int getMaxGroupIndex() {
        return maxGroupIndex;
    }

    public void setMaxGroupIndex(int maxGroupIndex) {
        this.maxGroupIndex = maxGroupIndex;
    }

    public String getDefaultGroupName() {
        return defaultGroupName;
    }

    public void setDefaultGroupName(String defaultGroupName) {
        this.defaultGroupName = defaultGroupName;
    }

    public int getMaxFavorites() {
        return maxFavorites;
    }

    public void setMaxFavorites(int maxFavorites) {
        this.maxFavorites = maxFavorites;
    }

    public int getMaxFavoriteBoards() {
        return maxFavoriteBoards;
    }

    public void setMaxFavoriteBoards(int maxFavoriteBoards) {
        this.maxFavoriteBoards = maxFavoriteBoards;
    }

    public int getKenoReservedGroupIndex() {
        return kenoReservedGroupIndex;
    }

    public void setKenoReservedGroupIndex(int kenoReservedGroupIndex) {
        this.kenoReservedGroupIndex = kenoReservedGroupIndex;
    }

    public boolean isReturnEmptyGroups() {
        return returnEmptyGroups;
    }

    public void setReturnEmptyGroups(boolean returnEmptyGroups) {
        this.returnEmptyGroups = returnEmptyGroups;
    }
}
