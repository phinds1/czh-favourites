# SP2: JSON API Models — Progress

| Phase | Description                                                        | Status   |
|-------|--------------------------------------------------------------------|----------|
| 1     | Write `WagerDto` + round-trip test                                 | complete |
| 2     | Write `FavouriteWagerDto` + round-trip test                        | complete |
| 3     | Write `FavouriteGroupDto` + round-trip test                        | complete |
| 4     | Write page wrappers (`FavouriteWagerPageDto`, `FavouriteGroupPageDto`) + tests | complete |
| 5     | Post-SP sign-off: test → coverage → security → refactor → routing map | complete |

## Notes

### Phase 5 — Post-SP sign-off

- Tests/coverage deferred — norun active during execution (human running tests in background)
- Security review: 0 critical, 0 high, 2 medium, 2 low — all findings are SP5 service-layer
  validation responsibilities; no changes required to DTO files
  - MEDIUM: `WagerDto.boards` unbounded depth — SP5 validator must cap at `max-favorite-boards`
  - MEDIUM: `gameName` length not guarded — SP5 validator must enforce ≤ 64 chars
  - LOW: `flags` field writable by clients — SP5 service must ignore client-supplied flags on create
  - LOW: `groupName`/`wagerName` no length guard — SP5 validator must enforce ≤ 255 chars
- Refactor pass: clean — no legacy imports, no dead code
- Routing map updated: `.requirements/design/routing.md`

## SP2 status: DONE — proceed to SP3

## Resume instructions

1. Read `.planning/plans/SP2_API_MODELS_PLAN.md`
2. Find the first phase marked **pending** above
3. Read only the **Files to read** listed for that phase
4. Implement, run `./mvn.sh -pl czh-favorites-api test`, confirm green
5. Update the phase status to **complete** in this file before moving to the next phase
