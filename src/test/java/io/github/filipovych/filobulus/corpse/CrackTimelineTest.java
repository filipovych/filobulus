package io.github.filipovych.filobulus.corpse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.filipovych.filobulus.corpse.CrackTimeline.Frame;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class CrackTimelineTest {
	@Test
	void startsAndEndsWithNothingToDraw() {
		assertEquals(new Frame(0, 0, 0, -1, -1, 0, 0, 0), CrackTimeline.at(0));
		assertEquals(CrackTimeline.at(0), CrackTimeline.at(-50));
		assertEquals(new Frame(0, 0, 0, 9, 1, 0, 0, 0), CrackTimeline.at(CrackTimeline.TOTAL));
		assertEquals(CrackTimeline.at(CrackTimeline.TOTAL), CrackTimeline.at(CrackTimeline.TOTAL + 5000));
	}

	@Test
	void theBlockPopsInAndIsGoneAtTheShatter() {
		assertTrue(CrackTimeline.at(60).block() > 0);
		assertTrue(CrackTimeline.at(60).block() < 1);
		assertTrue(CrackTimeline.at(150).block() > 1);
		assertTrue(CrackTimeline.at(150).block() < 1.15f);
		assertEquals(1, CrackTimeline.at(250).block());
		assertEquals(1, CrackTimeline.at(2699).block());
		assertEquals(0, CrackTimeline.at(2700).block());
	}

	@Test
	void theCracksComeFasterTowardsTheEnd() {
		List<Long> times = new ArrayList<>();
		for (long t = 0; t < 2700; t++) {
			int cracks = CrackTimeline.at(t).cracks();
			assertTrue(cracks == times.size() - 1 || cracks == times.size(), "at " + t);
			if (cracks == times.size()) {
				times.add(t);
			}
		}
		assertEquals(10, times.size());
		assertTrue(times.get(0) > 250);
		assertTrue(times.get(9) >= 2550);
		for (int i = 2; i < times.size(); i++) {
			assertTrue(times.get(i) - times.get(i - 1) < times.get(i - 1) - times.get(i - 2), "step " + i);
		}
	}

	@Test
	void itOnlyShakesWhileItCracks() {
		for (long t = 0; t <= CrackTimeline.TOTAL; t++) {
			Frame frame = CrackTimeline.at(t);
			if (frame.cracks() < 0 || t >= 2700) {
				assertEquals(0, frame.shakeX(), "at " + t);
				assertEquals(0, frame.shakeY(), "at " + t);
			}
			assertTrue(Math.abs(frame.shakeX()) <= 5 && Math.abs(frame.shakeY()) <= 5, "at " + t);
		}
		assertEquals(CrackTimeline.at(1234), CrackTimeline.at(1234));
	}
}
