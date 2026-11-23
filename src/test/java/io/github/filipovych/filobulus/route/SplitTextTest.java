package io.github.filipovych.filobulus.route;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SplitTextTest {
	@Test
	void clock() {
		assertEquals("0:00", SplitText.clock(0));
		assertEquals("0:09", SplitText.clock(9999));
		assertEquals("2:31", SplitText.clock(151_000));
		assertEquals("0:00", SplitText.clock(-5));
	}

	@Test
	void shortDeltasShowTenths() {
		assertEquals("-3.2s", SplitText.delta(-3250));
		assertEquals("+0.0s", SplitText.delta(0));
		assertEquals("+59.9s", SplitText.delta(59_999));
	}

	@Test
	void longDeltasShowMinutes() {
		assertEquals("+1:05", SplitText.delta(65_000));
		assertEquals("-2:00", SplitText.delta(-120_000));
	}
}
