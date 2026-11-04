package cz.bsl.czh.favorites.api;

/**
 * prize types supported
 *
 * <p>Mirrors front end {@code PrizeType} (gisorch); not shared by design - keep in sync.
 *
 * <p>Grep anchor: favorites
 */
public enum PrizeType {
    PARAMUTUEL,
    FIXED,
    FIXED_AND_PARAMUTUEL,
    FREE_TICKET,
    MERCHANDISE
}
