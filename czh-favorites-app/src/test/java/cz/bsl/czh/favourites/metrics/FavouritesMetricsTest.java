package cz.bsl.czh.favourites.metrics;

// Grep anchor: favourites

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static cz.bsl.czh.favourites.metrics.FavouritesMetrics.FAILURE;
import static cz.bsl.czh.favourites.metrics.FavouritesMetrics.GROUP_COUNTER;
import static cz.bsl.czh.favourites.metrics.FavouritesMetrics.OK;
import static cz.bsl.czh.favourites.metrics.FavouritesMetrics.OP_CREATE;
import static cz.bsl.czh.favourites.metrics.FavouritesMetrics.OP_GET;
import static cz.bsl.czh.favourites.metrics.FavouritesMetrics.TAG_OP;
import static cz.bsl.czh.favourites.metrics.FavouritesMetrics.TAG_OUTCOME;
import static cz.bsl.czh.favourites.metrics.FavouritesMetrics.WAGER_COUNTER;
import static org.junit.jupiter.api.Assertions.assertEquals;

class FavouritesMetricsTest {

    private SimpleMeterRegistry registry;
    private FavouritesMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics  = new FavouritesMetrics(registry);
    }

    @Test
    void wagerOk_incrementsWagerOkCounter() {
        metrics.wagerOk(OP_CREATE);

        double count = counterValue(WAGER_COUNTER, OP_CREATE, OK);
        assertEquals(1.0, count, 0.001);
    }

    @Test
    void wagerFailure_incrementsWagerFailureCounter() {
        metrics.wagerFailure(OP_CREATE);

        double count = counterValue(WAGER_COUNTER, OP_CREATE, FAILURE);
        assertEquals(1.0, count, 0.001);
    }

    @Test
    void groupOk_incrementsGroupOkCounter() {
        metrics.groupOk(OP_CREATE);

        double count = counterValue(GROUP_COUNTER, OP_CREATE, OK);
        assertEquals(1.0, count, 0.001);
    }

    @Test
    void groupFailure_incrementsGroupFailureCounter() {
        metrics.groupFailure(OP_CREATE);

        double count = counterValue(GROUP_COUNTER, OP_CREATE, FAILURE);
        assertEquals(1.0, count, 0.001);
    }

    @Test
    void multipleOps_counterPerOpLabel() {
        metrics.wagerOk(OP_CREATE);
        metrics.wagerOk(OP_CREATE);
        metrics.wagerOk(OP_GET);

        assertEquals(2.0, counterValue(WAGER_COUNTER, OP_CREATE, OK), 0.001);
        assertEquals(1.0, counterValue(WAGER_COUNTER, OP_GET, OK), 0.001);
        // failure counter for create must still be 0
        assertEquals(0.0, counterValue(WAGER_COUNTER, OP_CREATE, FAILURE), 0.001);
    }

    @Test
    void wagerAndGroupCountersAreIndependent() {
        metrics.wagerOk(OP_CREATE);
        metrics.groupOk(OP_CREATE);

        assertEquals(1.0, counterValue(WAGER_COUNTER, OP_CREATE, OK), 0.001);
        assertEquals(1.0, counterValue(GROUP_COUNTER, OP_CREATE, OK), 0.001);
    }

    private double counterValue(String name, String op, String outcome) {
        Counter counter = registry.find(name)
                .tag(TAG_OP, op)
                .tag(TAG_OUTCOME, outcome)
                .counter();
        return counter == null ? 0.0 : counter.count();
    }
}
