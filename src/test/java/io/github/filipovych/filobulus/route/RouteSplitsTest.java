package io.github.filipovych.filobulus.route;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RouteSplitsTest {
	private static final long[] BEST = {2000, 6000, 11_000};

	@Test
	void theClockRunsFromTheStartOfTheRun() {
		RouteSplits run = new RouteSplits(3, 50_000);
		assertFalse(run.started());
		assertEquals(2500, run.elapsed(52_500));
		run.reached(60_000);
		assertTrue(run.started());
		assertEquals(12_500, run.elapsed(62_500));
	}

	@Test
	void everyStopGetsItsTimeFromTheStart() {
		RouteSplits run = new RouteSplits(3, 58_000);
		run.reached(60_000);
		run.reached(63_000);
		assertFalse(run.finished());
		run.reached(70_000);
		assertTrue(run.finished());
		assertArrayEquals(new long[] {2000, 5000, 12_000}, run.times());
	}

	@Test
	void theClockStopsWhenTheRouteIsDone() {
		RouteSplits run = new RouteSplits(2, 0);
		run.reached(1000);
		run.reached(5000);
		run.reached(9000);
		assertEquals(5000, run.elapsed(20_000));
		assertArrayEquals(new long[] {1000, 5000}, run.times());
	}

	@Test
	void behindComparesTheLastStopWithTheBestRun() {
		RouteSplits run = new RouteSplits(3, 58_000);
		assertEquals(0, run.behind(BEST));
		run.reached(60_000);
		assertEquals(0, run.behind(BEST));
		run.reached(63_000);
		assertEquals(-1000, run.behind(BEST));
	}

	@Test
	void aRunWithSkippedStopsIsNotClean() {
		RouteSplits run = new RouteSplits(4, 0);
		run.reached(1000, 1);
		assertTrue(run.clean());
		run.reached(5000, 2);
		assertFalse(run.clean());
		run.reached(9000, 1);
		assertTrue(run.finished());
		assertArrayEquals(new long[] {1000, 5000, 5000, 9000}, run.times());
	}

	@Test
	void aRouteWithoutStopsNeverStarts() {
		RouteSplits run = new RouteSplits(0, 0);
		run.reached(1000);
		assertFalse(run.started());
		assertFalse(run.finished());
	}
}
