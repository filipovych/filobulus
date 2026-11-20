package io.github.filipovych.filobulus.detect;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.filipovych.filobulus.route.Location;
import io.github.filipovych.filobulus.shaft.ShaftType;
import org.junit.jupiter.api.Test;

class SteadyLocationTest {
	private static final Location SHAFT = Location.mineshaft(ShaftType.parse("JASP_1").orElseThrow());

	private final SteadyLocation steady = new SteadyLocation();

	@Test
	void enteringIsBelievedAtOnce() {
		assertEquals(Location.glaciteTunnels(), steady.update(Location.glaciteTunnels()));
		assertEquals(SHAFT, steady.update(SHAFT));
	}

	@Test
	void oneOddReadDoesNotLeaveTheShaft() {
		steady.update(SHAFT);
		assertEquals(SHAFT, steady.update(Location.elsewhere()));
		assertEquals(SHAFT, steady.update(Location.elsewhere()));
		assertEquals(SHAFT, steady.update(SHAFT));
		// the count starts again after a good read
		assertEquals(SHAFT, steady.update(Location.elsewhere()));
		assertEquals(SHAFT, steady.update(Location.elsewhere()));
	}

	@Test
	void threeReadsInARowLeaveTheShaft() {
		steady.update(SHAFT);
		steady.update(Location.glaciteTunnels());
		steady.update(Location.glaciteTunnels());
		assertEquals(Location.glaciteTunnels(), steady.update(Location.glaciteTunnels()));
	}

	@Test
	void resetLeavesAtOnce() {
		steady.update(SHAFT);
		steady.reset();
		assertEquals(Location.elsewhere(), steady.current());
	}
}
