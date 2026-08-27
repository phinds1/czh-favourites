package cz.bsl.czh.favourites.metrics;

// Grep anchor: favourites

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Prometheus counters for favourite wager and group operations.
 *
 * <p>Two counter families:
 * <ul>
 *   <li>{@code favourites.wager.operation{op,outcome}} — wager CRUD ops</li>
 *   <li>{@code favourites.group.operation{op,outcome}} — group CRUD ops</li>
 * </ul>
 *
 * <p>{@code outcome} is {@code ok} on success, {@code failure} on exception.
 * Prometheus renders these as {@code favourites_wager_operation_total} and
 * {@code favourites_group_operation_total}.
 */
@Component
public class FavouritesMetrics {

    public static final String WAGER_COUNTER = "favourites.wager.operation";
    public static final String GROUP_COUNTER = "favourites.group.operation";
    public static final String TAG_OP        = "op";
    public static final String TAG_OUTCOME   = "outcome";
    public static final String OK            = "ok";
    public static final String FAILURE       = "failure";

    public static final String OP_CREATE = "create";
    public static final String OP_GET    = "get";
    public static final String OP_UPDATE = "update";
    public static final String OP_DELETE = "delete";
    public static final String OP_LIST   = "list";

    private final MeterRegistry registry;

    public FavouritesMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void wagerOk(String op) {
        registry.counter(WAGER_COUNTER, TAG_OP, op, TAG_OUTCOME, OK).increment();
    }

    public void wagerFailure(String op) {
        registry.counter(WAGER_COUNTER, TAG_OP, op, TAG_OUTCOME, FAILURE).increment();
    }

    public void groupOk(String op) {
        registry.counter(GROUP_COUNTER, TAG_OP, op, TAG_OUTCOME, OK).increment();
    }

    public void groupFailure(String op) {
        registry.counter(GROUP_COUNTER, TAG_OP, op, TAG_OUTCOME, FAILURE).increment();
    }
}
