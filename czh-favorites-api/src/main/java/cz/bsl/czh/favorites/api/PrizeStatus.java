package cz.bsl.czh.favorites.api;

/**
 * Prize statuses supported
 *
 * <p>Mirrors front end {@code PrizeStatus} (gisorch); not shared by design - keep in sync.
 *
 * <p>Grep anchor: favorites
 */
public enum PrizeStatus {
    NOT_PAID, PAID, EXPIRED
}
