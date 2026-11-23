package io.github.filipovych.filobulus.route;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RouteSplitsTest {
	private static final long[] BEST = {0, 4000, 9000};

	@Test
	void theClockStartsAtTheFirstStop() {
		RouteSplits run = new RouteSplits(3);
		assertFalse(run.started());
		assertEquals(0, run.elapsed(50_000));
		run.reached(60_000);
		assertTrue(run.started());
		assertEquals(2500, run.elapsed(62_500));
	}

	@Test
	void everyStopGetsItsTime() {
		RouteSplits run = new RouteSplits(3);
		run.reached(60_000);
		run.reached(63_000);
		assertFalse(run.finished());
		run.reached(70_000);
		assertTrue(run.finished());
		assertArrayEquals(new long[] {0, 3000, 10_000}, run.times());
	}

	@Test
	void theClockStopsWhenTheRouteIsDone() {
		RouteSplits run = new RouteSplits(2);
		run.reached(1000);
		run.reached(5000);
		run.reached(9000);
		assertEquals(4000, run.elapsed(20_000));
		assertArrayEquals(new long[] {0, 4000}, run.times());
	}

	@Test
	void behindComparesTheLastStopWithTheBestRun() {
		RouteSplits run = new RouteSplits(3);
		assertEquals(0, run.behind(BEST));
		run.reached(60_000);
		assertEquals(0, run.behind(BEST));
		run.reached(63_000);
		assertEquals(-1000, run.behind(BEST));
	}

	@Test
	void leftCountsDownFromWhatTheBestRunNeeded() {
		RouteSplits run = new RouteSplits(3);
		run.reached(60_000);
		run.reached(63_000);
		// a second ahead, so 8 seconds in total, 3 of them gone
		assertEquals(5000, run.left(BEST, 63_000));
		assertEquals(3000, run.left(BEST, 65_000));
		assertEquals(0, run.left(BEST, 90_000));
	}

	@Test
	void aRouteWithoutStopsNeverStarts() {
		RouteSplits run = new RouteSplits(0);
		run.reached(1000);
		assertFalse(run.started());
		assertFalse(run.finished());
	}
}
