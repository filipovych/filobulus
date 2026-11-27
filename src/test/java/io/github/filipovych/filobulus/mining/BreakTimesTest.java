package io.github.filipovych.filobulus.mining;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BreakTimesTest {
	@Test
	void theMedianOfTheLastFewIsWhatCounts() {
		BreakTimes times = new BreakTimes();
		String key = BreakTimes.key(BlockStrength.JASPER, 6000);
		times.record(key, 8, 9);
		times.record(key, 9, 9);
		assertTrue(times.learned(key).isEmpty());
		times.record(key, 15, 9);
		assertEquals(9, times.learned(key).getAsInt());
		times.record(key, 8, 9);
		times.record(key, 8, 9);
		times.record(key, 8, 9);
		assertEquals(8, times.learned(key).getAsInt());
		assertTrue(times.learned(BreakTimes.key(BlockStrength.RUBY, 6000)).isEmpty());
	}

	@Test
	void aMeasurementUnderOneTickIsNoise() {
		BreakTimes times = new BreakTimes();
		times.record("x", 0, 5);
		times.record("x", -3, 5);
		times.record("x", 5, 5);
		assertTrue(times.learned("x").isEmpty());
	}

	@Test
	void aMeasurementFarFromTheSumIsThrownAway() {
		BreakTimes times = new BreakTimes();
		String key = BreakTimes.key(BlockStrength.JASPER, 6000);
		// a pickobulus took it at once, someone else took it, or it was mined for ages
		times.record(key, 2, 10);
		times.record(key, 25, 10);
		times.record(key, 4, 10);
		assertTrue(times.learned(key).isEmpty());
		times.record(key, 5, 10);
		times.record(key, 20, 10);
		times.record(key, 11, 10);
		assertEquals(11, times.learned(key).getAsInt());
	}
}
