package io.github.filipovych.filobulus.mining;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BreakTimesTest {
	@Test
	void theMedianOfTheLastFewIsWhatCounts() {
		BreakTimes times = new BreakTimes();
		String key = BreakTimes.key(BlockStrength.JASPER, 6000);
		times.record(key, 8);
		times.record(key, 9);
		assertTrue(times.learned(key).isEmpty());
		times.record(key, 30);
		assertEquals(9, times.learned(key).getAsInt());
		times.record(key, 8);
		times.record(key, 8);
		times.record(key, 8);
		assertEquals(8, times.learned(key).getAsInt());
		assertTrue(times.learned(BreakTimes.key(BlockStrength.RUBY, 6000)).isEmpty());
	}

	@Test
	void aMeasurementUnderOneTickIsNoise() {
		BreakTimes times = new BreakTimes();
		times.record("x", 0);
		times.record("x", -3);
		times.record("x", 5);
		assertTrue(times.learned("x").isEmpty());
	}
}
