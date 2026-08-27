# Java 6 Migration

This project is to be a Spring Boot 4 micro-service containing the favourite wagers service.  This service supports fetching and saving a players favourite wagers in the database.

It is ported from the Java6 GIS implementation available for reading and copying in `./references/gis/`.

`./references` includes 
- `gis` app that contains the features we are porting
- `czh-dgsubs` which is a different part of GIS that was recently ported to Java15 and sprint boot.


## Core classes to port..

Core code is to be in the `czh-favorites-app` sub module.
FavoriteWagerService & DefaultFavoriteWagerService - port and upgrade to Java25 and Spring annotations
ARTEGameHostServiceImpl should be rewritten and fetch the current CDC from a file `/run/mx/cdc` which contains an ascii integer in the current CDC, and optionally `\n`.
Time of day should be presumed to be the same time as the current system clock

## JSON API

The contens of gateway-draw-games-rest-api-2.6.43.0.jar that are needed for `czh-favorites` should be copied to a new module. `./czh-favorites-api`
The package name should be changed to start with `cz.bsl.favorites`

## FavoriteWagerDAO

The database is to remain IBM DB2, in production, h2 in test.
The DAO layer is to be rewritten in JdbcTemplates and the storeage of favorite wagers is to change to a JSON document store design.
The JSON store design is a single flat table `GIS_FAVORITES` with columns for each value that must be in an index and a VARCHAR(32) column for the wager JSON.
This will be a new table and we will migrate the data using a separate project.
Format of the JSON in the database is to be almost identical to the front end JSON format.


## HTTP endpoints

There are two endpoints to be ported to Spring web...

- AdminFavoriteWagerResource
- PlayerFavoriteWagerResource

Every method in these resources needs Jamcrest test.

The authentication will be different, pre-authenticated requests will be sent with whatever player and tracking identifiers needed passed as HTTP headers.
The ported methods should have comments indicating the authentication required.

Validation is to be ported from Hibernate validator to plain Java code.
Any use of infinispan cache is to be dropped.

## Jamcrest testing

HTTP resource testing will be changed to use Spring native test rest client and Jamcrest for asserting JSON. /jamcrest
Jamcrest testing framework should be setup as per /jamcrest-testing. In the AbstractRestTest class we should detect when a resp.js file does not exist and print the expected JSON.
AbstractRestTest should support mocking admin or player auth by setting the expected HTTP headers.

WagerResourceFavouriteTest & WagerResourcePowerspinFavoriteTest need to be ported, all JUnit tests should still exist when the porting is complete.
`*.req.js` files can be created from the existing tests in  references/gis/components/gis/gis-site/src/main/resources/json/example/favorites
Current tests make no assertions we should assert the full JSON response with Jamcrest and `*.resp.js` files.  /jamcrest-testing.

AbstractRestTest should initialises the spring-boot container and mock db2 for testing.
We should create a mock H2 database that contains just the table needed by the new DAO layer.  This DB should be initialised by a Spring bean that only loads when the profile is "test".
The table creation delta scripts should be in czh-favorites-db/src/main/delta, These should be loaded to the database via calling the `db2delta` application that is installed locally and has a man page
The delta file format is...

```
terminator=;
continue-on-error=false
author=Alice
logging=debug

[changeset:create_favorites_table]

CREATE TABLE GIS_FAVORITES ....

[validation:favorites_table_exists]

SELECT COUNT(*) FROM syscat.tables WHERE tabname='GIS_FAVORITES'
```




