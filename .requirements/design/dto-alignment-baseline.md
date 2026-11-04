# DTO alignment baseline — favorites ↔ gisorch (Phase 0)

Source of truth: **gisorch** (`references/czh-gisorch/czh-gis-api/src/main/java/cz/bsl/gis/api/drawgames`)
— it is the actual front end. Legacy gis
(`references/gis/components/czh-gateway-draw-games-rest-api/src/main/java/com/gtech/esa/gateway/rest/drawgames/dto`)
is the cross-check. Plan: `.planning/plans/DTO_ALIGNMENT.md`.
Wire-level differences: `/java/czh/czh-gis-integrated/.requirements/integrations/favorites-integration.md`.

## 1. Per-file diff gis vs gisorch (transitive closure of WagerDTO)

Field/type comparison (comments, imports, packages stripped; accessors compared as pairs).

| File                 | Fields identical | Annotation delta (gis → gisorch)                              | Field deltas (gisorch adds)   | gis-only fields |
|----------------------|------------------|----------------------------------------------------------------|--------------------------------|-----------------|
| WagerDTO             | yes              | `@XmlRootElement @XmlAccessorType(FIELD)` → `@JsonAutoDetect(field=ANY, getter/isGetter/setter/creator=NONE)` | –                              | –               |
| BoardDTO             | yes              | same JAXB → Jackson pattern                                    | –                              | –               |
| StakeDTO             | yes              | none → `@JsonAutoDetect(...)`                                 | –                              | –               |
| WagerType (enum)     | yes              | none                                                           | – (constants identical)        | –               |
| ErrorDTO             | yes              | JAXB → Jackson                                                 | `uid` (String)                 | –               |
| FieldErrorDTO        | yes              | JAXB → Jackson                                                 | `fieldPath`, `message` (String)| –               |
| AddressDTO           | yes              | none → Jackson                                                 | –                              | –               |
| IdDocumentDTO        | yes              | none → Jackson                                                 | –                              | –               |
| PrizeDTO             | yes              | none → Jackson                                                 | –                              | –               |
| PrizeStatus (enum)   | yes              | none                                                           | – (constants identical)        | –               |
| PrizeType (enum)     | yes              | none                                                           | – (constants identical)        | –               |
| PromotionDTO         | yes              | JAXB → Jackson                                                 | –                              | –               |
| TaxDTO               | yes              | none → Jackson                                                 | –                              | –               |
| TextDTO              | yes              | none → Jackson                                                 | –                              | –               |
| TicketSecurityDTO    | yes              | none → Jackson                                                 | –                              | –               |
| FavoriteWagerDTO     | yes              | JAXB → Jackson                                                 | –                              | –               |
| FavoriteWagerGroupDTO | yes            | JAXB → Jackson                                                 | –                              | –               |
| FavoriteWagerPageDTO | yes              | JAXB → Jackson                                                 | –                              | –               |
| FavoriteWagerGroupPageDTO | yes         | JAXB → Jackson                                                 | –                              | –               |

**Conclusion:** gisorch is a strict superset of gis for the closure. There are no gis-only
fields to carry over; the only content deltas are additive gisorch fields
(`ErrorDTO.uid`, `FieldErrorDTO.fieldPath`, `FieldErrorDTO.message`). The JSON property
name set (Jackson field-visibility) is identical on both sides, so wire compatibility of
the legacy JSON body is preserved by copying gisorch verbatim.

Type reference closure (verified, no further deps):
`WagerDTO → BoardDTO, StakeDTO, WagerType, ErrorDTO→FieldErrorDTO, AddressDTO, IdDocumentDTO,
PrizeDTO→PrizeStatus/PrizeType, PromotionDTO→TextDTO, TaxDTO, TextDTO, TicketSecurityDTO`.

## 2. Copy list (Phase 2)

Copy into `czh-favorites-api`, package `cz.bsl.czh.favorites.api`, keeping the JSON property
names and `@JsonAutoDetect(fieldVisibility = ANY, getter/isGetter/setter/creator = NONE)`
annotation identical to gisorch. Class naming keeps the existing `*Dto` convention:

| gisorch source          | favorites-api copy | Kind |
|-------------------------|--------------------|------|
| WagerDTO.java           | WagerDto.java      | class (recursed by `addonWagers`, `freeTicket`, `freeTickets`) |
| BoardDTO.java           | BoardDto.java      | class |
| StakeDTO.java           | StakeDto.java      | class |
| TaxDTO.java             | TaxDto.java        | class |
| TextDTO.java            | TextDto.java       | class |
| AddressDTO.java         | AddressDto.java    | class |
| IdDocumentDTO.java      | IdDocumentDto.java | class |
| PrizeDTO.java           | PrizeDto.java      | class |
| PromotionDTO.java       | PromotionDto.java  | class |
| TicketSecurityDTO.java  | TicketSecurityDto.java | class |
| ErrorDTO.java           | ErrorDto.java      | class (favorites wire error body re-uses this shape) |
| FieldErrorDTO.java      | FieldErrorDto.java | class |
| WagerType.java          | WagerType.java     | enum |
| PrizeStatus.java        | PrizeStatus.java   | enum |
| PrizeType.java          | PrizeType.java     | enum |

## 3. Golden fixtures (captured)

Byte-identical copies of the legacy front-end bodies, used as golden JSON in tests:

| Legacy source (gis rest-tests)                                    | Favorites copy |
|-------------------------------------------------------------------|---------------|
| `.../wagering/favorites/sportka.json`                            | `czh-favorites-api/src/test/resources/legacy/sportka.json` |
| `.../wagering/favorites/group/sportkagroup.json`                 | `czh-favorites-api/src/test/resources/legacy/sportkagroup.json` |

`sportka.json` exercises: `name`, `group{name}`, `wager.type`, `duration`, `drawNames`,
`gameName`, `price`, `addonWagers[].boards[].primarySelections`, `boards[].systemBetType`,
`betType`, `quickpick`, `primarySelections` — every construct the compact `WagerDto`
currently drops.

## 4. Open decisions from the plan — resolution

- **Legacy group `id` == `groupNumber`?** Yes: the wager-level `group.id` is the group slot
  number (1–10) per the integration doc §1.1/§2; the surrogate DB key stays internal.
- **Create without a number auto-assigns?** Yes: legacy group creates send `{"name": ...}`
  only (see `sportkagroup.json`), so the service assigns the next free slot (cap
  `maxGroupIndex`), per the integration doc §2.

## 5. Breaking change — release-note text (Phase 5)

> **BREAKING — stored `WAGER_JSON` format change.** The wager sub-object of a favorite is
> now stored as the full front-end wager structure (boards are objects with selections;
> `addonWagers`, `drawNames`, `type`, per-board stakes etc. are preserved) instead of the
> former compact `{gameName, stake, price, duration, serialNumber, boards: int[][]}` shape.
> Server-assigned wager fields (id, status, requestId, digest, transactionTime, signature,
> prizes, ...) are stripped before storing. Existing `GIS_FAV_WAGER.WAGER_JSON` rows written
> by the previous version are NOT readable by this version (by design — no migration, no
> backward-compatible reading; see the DTO Alignment plan decision). Back up and re-create
> favorites, or accept that pre-upgrade favorites return HTTP 500 on read (WAGER_JSON is
> treated as opaque by the DAO; deserialization failures surface as 500).
> The REST wire contract now matches the legacy front end (gisorch): string `id`,
> `name`, `group {id, name}`, `favoriteWagers`/`favoriteWagerGroups` list wrappers, and
> POST returns 200 (not 201).

Schema check: `WAGER_JSON VARCHAR(30000)` (delta `002-create-gis-fav-wager.delta`) equals
the enforced `MAX_WAGER_JSON_BYTES = 30_000` — no schema change needed. The full structure
is larger per board than the compact format (~100 chars vs ~3), so the request-time 400
check remains the guard. Note for review: DB2 `VARCHAR(n)` counts bytes, the Java check
counts chars — non-ASCII content in stored wager JSON could hit the DB limit before the
400; realistic favorites (ASCII game/draw names, numeric selections) stay well under it.
