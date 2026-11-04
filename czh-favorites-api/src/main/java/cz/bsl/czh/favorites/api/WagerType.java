package cz.bsl.czh.favorites.api;

/**
 * Wager types supported
 *
 * <p>Mirrors front end {@code WagerType} (gisorch); not shared by design - keep in sync.
 *
 * <p>Grep anchor: favorites
 */
public enum WagerType {
    /** Wager placed initially by player or later on repeated by subscription process */
    SUBSCRIPTION,

    /** Wager placed in order to buy a fraction of a syndicate wager */
    SYNDICATE,

    /** Normal wager */
    NORMAL,

    /** Fraction wager */
    FRACTION
}
