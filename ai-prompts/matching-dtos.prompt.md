WagerDto for this project as its own API (which is correct to have our own classes) but they must match the front end DTOs.

Particularly WagerDto and its dependent sub Dtos.

The original `gis` reference classes are here: `references/gis/components/czh-gateway-draw-games-rest-api/src/main/java/com/gtech/esa/gateway/rest/drawgames/dto`

The `gisorch` is the new front end and its classes are here: `references/czh-gisorch/czh-gis-api/src/main/java/cz/bsl/gis/api/drawgames`

They should match.

Favorites has some additions such as favorite name but the structure of the wager should be the same in this microservice as the front end.

There is an analysis of differences here `/java/czh/czh-gis-integrated/.requirements/integrations/favorites-integration.md`

We do not want shared Dtos, so each microservice can dictate its extensions and attributes that are not supported.