package cz.bsl.czh.favourites.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Paginated list of {@link FavouriteGroupDto} items.
 *
 * <p>Grep anchor: favourites
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FavouriteGroupPageDto {

    private List<FavouriteGroupDto> items;
    private int totalCount;

    public FavouriteGroupPageDto() {
    }

    public FavouriteGroupPageDto(List<FavouriteGroupDto> items, int totalCount) {
        this.items = items;
        this.totalCount = totalCount;
    }

    public List<FavouriteGroupDto> getItems() {
        return items;
    }

    public void setItems(List<FavouriteGroupDto> items) {
        this.items = items;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }
}
