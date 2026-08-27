package cz.bsl.czh.favourites.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Paginated list of {@link FavouriteWagerDto} items.
 *
 * <p>Grep anchor: favourites
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FavouriteWagerPageDto {

    private List<FavouriteWagerDto> items;
    private int totalCount;

    public FavouriteWagerPageDto() {
    }

    public FavouriteWagerPageDto(List<FavouriteWagerDto> items, int totalCount) {
        this.items = items;
        this.totalCount = totalCount;
    }

    public List<FavouriteWagerDto> getItems() {
        return items;
    }

    public void setItems(List<FavouriteWagerDto> items) {
        this.items = items;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }
}
