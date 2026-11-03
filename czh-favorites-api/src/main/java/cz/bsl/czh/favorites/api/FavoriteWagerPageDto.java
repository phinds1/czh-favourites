package cz.bsl.czh.favorites.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Paginated list of {@link FavoriteWagerDto} items.
 *
 * <p>Grep anchor: favorites
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FavoriteWagerPageDto {

    private List<FavoriteWagerDto> items;
    private int totalCount;

    public FavoriteWagerPageDto() {
    }

    public FavoriteWagerPageDto(List<FavoriteWagerDto> items, int totalCount) {
        this.items = items;
        this.totalCount = totalCount;
    }

    public List<FavoriteWagerDto> getItems() {
        return items;
    }

    public void setItems(List<FavoriteWagerDto> items) {
        this.items = items;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }
}
