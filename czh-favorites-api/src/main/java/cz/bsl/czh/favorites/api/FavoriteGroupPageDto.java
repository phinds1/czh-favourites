package cz.bsl.czh.favorites.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Paginated list of {@link FavoriteGroupDto} items.
 *
 * <p>Grep anchor: favorites
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FavoriteGroupPageDto {

    private List<FavoriteGroupDto> items;
    private int totalCount;

    public FavoriteGroupPageDto() {
    }

    public FavoriteGroupPageDto(List<FavoriteGroupDto> items, int totalCount) {
        this.items = items;
        this.totalCount = totalCount;
    }

    public List<FavoriteGroupDto> getItems() {
        return items;
    }

    public void setItems(List<FavoriteGroupDto> items) {
        this.items = items;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }
}
