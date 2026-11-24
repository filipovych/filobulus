package io.github.filipovych.filobulus.corpse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.filipovych.filobulus.corpse.CrackTimeline.Cue;
import io.github.filipovych.filobulus.corpse.CrackTimeline.Frame;
import io.github.filipovych.filobulus.corpse.CrackTimeline.Sound;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class CrackTimelineTest {
	private static final float CLOSE = 0.001f;

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

	// the furthest the block is off its place in a stretch of time
	private static float reach(long from, long to) {
		float reach = 0;
		for (long t = from; t < to; t++) {
			Frame frame = CrackTimeline.at(t);
			reach = Math.max(reach, Math.max(Math.abs(frame.shakeX()), Math.abs(frame.shakeY())));
		}
		return reach;
	}

	@Test
	void theTrembleGrowsAndEveryCrackKicks() {
		// 450 is the first crack and 2610 the last, the kick is over 120 ms later
		float firstKick = reach(450, 570);
		float firstTremble = reach(570, 820);
		float lastKick = reach(2610, 2700);
		assertTrue(firstTremble > 0);
		assertTrue(firstKick > firstTremble * 2);
		assertTrue(lastKick > firstKick);
		assertTrue(reach(2400, 2470) > firstTremble * 2);
	}

	@Test
	void theShatterFlashesAndThrowsShards() {
		assertEquals(-1, CrackTimeline.at(2699).burst());
		assertEquals(0, CrackTimeline.at(2699).flash());
		assertEquals(0, CrackTimeline.at(2700).burst());
		assertEquals(1, CrackTimeline.at(2700).flash());
		assertEquals(0.5f, CrackTimeline.at(2790).flash(), CLOSE);
		assertEquals(0, CrackTimeline.at(2880).flash());
		assertEquals(0.5f, CrackTimeline.at(3050).burst(), CLOSE);
		assertEquals(1, CrackTimeline.at(3400).burst());
		assertEquals(1, CrackTimeline.at(4000).burst());
	}

	@Test
	void theItemSwingsPastItsSizeAndSettles() {
		assertEquals(0, CrackTimeline.at(2699).reveal());
		assertEquals(0, CrackTimeline.at(2700).reveal());
		assertEquals(1.25f, CrackTimeline.at(2875).reveal(), CLOSE);
		assertEquals(1, CrackTimeline.at(3050).reveal());
		assertEquals(1, CrackTimeline.at(4300).reveal());
		assertEquals(0.5f, CrackTimeline.at(4450).reveal(), CLOSE);
		assertEquals(0, CrackTimeline.at(4600).reveal());
		for (long t = 2700; t <= CrackTimeline.TOTAL; t++) {
			assertTrue(CrackTimeline.at(t).reveal() <= 1.26f, "at " + t);
		}
	}

	@Test
	void theWordsFadeInAfterTheShatterAndOutAtTheEnd() {
		assertEquals(0, CrackTimeline.at(2900).text());
		assertEquals(0.5f, CrackTimeline.at(3000).text(), CLOSE);
		assertEquals(1, CrackTimeline.at(3100).text());
		assertEquals(1, CrackTimeline.at(4200).text());
		assertEquals(0.5f, CrackTimeline.at(4400).text(), CLOSE);
		assertEquals(0, CrackTimeline.at(4600).text());
	}

	@Test
	void everySoundComesOnceAndInOrder() {
		List<Sound> all = CrackTimeline.cues(0, CrackTimeline.TOTAL);
		assertEquals(12, all.size());
		for (int step = 0; step < 10; step++) {
			assertEquals(new Sound(Cue.CRACK, step), all.get(step));
		}
		assertEquals(new Sound(Cue.SHATTER, 0), all.get(10));
		assertEquals(new Sound(Cue.REVEAL, 0), all.get(11));

		List<Sound> ticked = new ArrayList<>();
		for (long t = 0; t < CrackTimeline.TOTAL; t += 17) {
			ticked.addAll(CrackTimeline.cues(t, t + 17));
		}
		assertEquals(all, ticked);
	}

	@Test
	void aWindowTakesItsEndAndNotItsStart() {
		assertEquals(List.of(), CrackTimeline.cues(-1, 0));
		assertEquals(List.of(), CrackTimeline.cues(0, 449));
		assertEquals(List.of(new Sound(Cue.CRACK, 0)), CrackTimeline.cues(449, 450));
		assertEquals(List.of(), CrackTimeline.cues(450, 460));
		assertEquals(List.of(new Sound(Cue.SHATTER, 0)), CrackTimeline.cues(2699, 2700));
		assertEquals(List.of(new Sound(Cue.REVEAL, 0)), CrackTimeline.cues(2700, 2850));
		assertEquals(List.of(new Sound(Cue.CRACK, 9), new Sound(Cue.SHATTER, 0), new Sound(Cue.REVEAL, 0)),
				CrackTimeline.cues(2600, 2850));
		assertEquals(List.of(), CrackTimeline.cues(2850, 9000));
		assertEquals(List.of(), CrackTimeline.cues(500, 400));
	}

	@Test
	void theCrackSoundsComeWithTheCracks() {
		for (long t = 1; t < 2700; t++) {
			int before = CrackTimeline.at(t - 1).cracks();
			int now = CrackTimeline.at(t).cracks();
			List<Sound> sounds = CrackTimeline.cues(t - 1, t);
			assertEquals(now == before ? List.of() : List.of(new Sound(Cue.CRACK, now)), sounds, "at " + t);
		}
	}
}
