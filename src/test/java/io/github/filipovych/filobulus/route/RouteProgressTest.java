package io.github.filipovych.filobulus.route;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RouteProgressTest {
	private static final List<Waypoint> STOPS = List.of(
			new Waypoint(0, 10, 0, "1"), new Waypoint(20, 10, 0, "2"), new Waypoint(40, 10, 0, "3"));

	private static final double REACH = 5;

	private final RouteProgress progress = new RouteProgress(STOPS);

	private String next() {
		return progress.next().map(Waypoint::label).orElse(null);
	}

	@Test
	void startsAtTheFirstStopAndStaysWhileFarAway() {
		assertEquals("1", next());
		assertFalse(progress.update(10, 10, 0, REACH));
		assertEquals("1", next());
	}

	@Test
	void movesOnWithinReachInAnyDirection() {
		assertTrue(progress.update(0.5, 10.5, 5, REACH));
		assertEquals("2", next());
		// straight above counts too, 6 up is too far, 4 up is close enough
		assertFalse(progress.update(20.5, 16.5, 0.5, REACH));
		assertTrue(progress.update(20.5, 14.5, 0.5, REACH));
		assertEquals("3", next());
	}

	@Test
	void listsTheStopsStillToVisit() {
		assertEquals(STOPS, progress.remaining());
		progress.update(0.5, 10.5, 0.5, REACH);
		assertEquals(STOPS.subList(1, 3), progress.remaining());
	}

	@Test
	void aSmallerReachNeedsYouCloser() {
		assertFalse(progress.update(0.5, 10.5, 4.5, 3));
		assertTrue(progress.update(0.5, 10.5, 4.5, 5));
	}

	@Test
	void doesNotSkipAheadToALaterStop() {
		assertFalse(progress.update(40.5, 10.5, 0.5, REACH));
		assertEquals("1", next());
	}

	@Test
	void theRouteIsDoneAfterTheLastStop() {
		progress.update(0.5, 10.5, 0.5, REACH);
		progress.update(20.5, 10.5, 0.5, REACH);
		assertFalse(progress.finished());
		assertTrue(progress.update(40.5, 10.5, 0.5, REACH));
		assertTrue(progress.finished());
		assertEquals(Optional.empty(), progress.next());
		assertFalse(progress.update(40.5, 10.5, 0.5, REACH));
	}
}
