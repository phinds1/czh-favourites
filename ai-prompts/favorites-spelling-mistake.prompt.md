Lots of code has been built with UK spelling of favorites, we use the US spelling.

The API paths are totally incorrect, seem to ge hallucinated based on UK spelling.

references/gis/components/czh-gateway-draw-games-rest-api/src/main/java/com/gtech/esa/gateway/rest/drawgames/FavoriteWagerResource.java  has the correct paths for czh-favorites-app/src/main/java/cz/bsl/favourites/web/FavouritesController.java

components/gis/gis-site/src/main/java/com/gtech/gis/site/resources/AdminFavoriteWagerResource.java has the correct paths for czh-favorites-app/src/main/java/cz/bsl/favourites/web/AdminFavouritesController.java

