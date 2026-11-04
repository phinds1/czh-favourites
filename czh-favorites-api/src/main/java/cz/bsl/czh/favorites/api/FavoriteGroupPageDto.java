package cz.bsl.czh.favorites.api;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

/**
 * API DTO for a page of favorite wager groups; returned by the list-groups endpoint.
 *
 * <p>Mirrors front end {@code FavoriteWagerGroupPageDTO} (gisorch): the items travel in
 * {@code favoriteWagerGroups} and the legacy paging fields are part of the shape. The
 * favorites list is unpaged, so the paging fields stay null (omitted on the wire);
 * {@code totalCount} is a favorites-only additive attribute.
 *
 * <p>Grep anchor: favorites
 */
@JsonAutoDetect(fieldVisibility = Visibility.ANY, getterVisibility = Visibility.NONE,
        isGetterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE,
        creatorVisibility = Visibility.NONE)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FavoriteGroupPageDto {

    /** List of favorite wager groups. */
    private List<FavoriteGroupDto> favoriteWagerGroups;
    /** Favorites extension: total number of groups (the list is unpaged). */
    private int totalCount;
    /** URL to get the next page of the paginated results. */
    private String nextPageUrl;
    /** URL to get the previous page of the paginated results. */
    private String previousPageUrl;
    /** Page number - page URL mapping to jump to a selected page. */
    private Map<Integer, String> pageUrls;
    /** Total amount of items contained in the next pages. */
    private Integer nextItems;
    /** Total amount of items contained in the previous pages. */
    private Integer previousItems;

    public FavoriteGroupPageDto() {
    }

    public FavoriteGroupPageDto(List<FavoriteGroupDto> favoriteWagerGroups, int totalCount) {
        this.favoriteWagerGroups = favoriteWagerGroups;
        this.totalCount = totalCount;
    }

    public List<FavoriteGroupDto> getFavoriteWagerGroups() {
        return favoriteWagerGroups;
    }

    public void setFavoriteWagerGroups(List<FavoriteGroupDto> favoriteWagerGroups) {
        this.favoriteWagerGroups = favoriteWagerGroups;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public String getNextPageUrl() {
        return nextPageUrl;
    }

    public void setNextPageUrl(String nextPageUrl) {
        this.nextPageUrl = nextPageUrl;
    }

    public String getPreviousPageUrl() {
        return previousPageUrl;
    }

    public void setPreviousPageUrl(String previousPageUrl) {
        this.previousPageUrl = previousPageUrl;
    }

    public Map<Integer, String> getPageUrls() {
        return pageUrls;
    }

    public void setPageUrls(Map<Integer, String> pageUrls) {
        this.pageUrls = pageUrls;
    }

    public Integer getNextItems() {
        return nextItems;
    }

    public void setNextItems(Integer nextItems) {
        this.nextItems = nextItems;
    }

    public Integer getPreviousItems() {
        return previousItems;
    }

    public void setPreviousItems(Integer previousItems) {
        this.previousItems = previousItems;
    }
}
