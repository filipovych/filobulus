package io.github.filipovych.filobulus.alert;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.filipovych.filobulus.alert.JasperTimeline.Frame;
import io.github.filipovych.filobulus.alert.JasperTimeline.Segment;
import java.util.List;
import org.junit.jupiter.api.Test;

class JasperTimelineTest {
	private static final float CLOSE = 0.001f;

	@Test
	void aLongDryStreakGetsTheBigOne() {
		assertFalse(JasperTimeline.big(0));
		assertFalse(JasperTimeline.big(29));
		assertTrue(JasperTimeline.big(30));
		assertTrue(JasperTimeline.total(true) > JasperTimeline.total(false));
		assertTrue(JasperTimeline.shards(true).size() > JasperTimeline.shards(false).size());
	}

	@Test
	void startsAndEndsWithNothingToDraw() {
		for (boolean big : new boolean[] {false, true}) {
			Frame start = JasperTimeline.at(0, big);
			assertEquals(new Frame(0, 0, 0, 0, 0, -1, 0, 0, 0), start);
			assertEquals(start, JasperTimeline.at(-100, big));
			Frame end = JasperTimeline.at(JasperTimeline.total(big), big);
			assertEquals(0, end.frost());
			assertEquals(0, end.crackAlpha());
			assertEquals(0, end.title());
			assertEquals(0, end.subtitle());
			assertEquals(0, end.dry());
			assertEquals(end, JasperTimeline.at(JasperTimeline.total(big) + 9000, big));
		}
	}

	@Test
	void itIsNeverLong() {
		assertTrue(JasperTimeline.total(false) <= 3500);
		assertTrue(JasperTimeline.total(true) <= 5000);
	}

	@Test
	void theFrostGrowsInAndNeverGoesPastFull() {
		float last = 0;
		for (long t = 0; t <= 900; t += 10) {
			float frost = JasperTimeline.at(t, false).frost();
			assertTrue(frost >= last, "at " + t);
			assertTrue(frost <= 1, "at " + t);
			last = frost;
		}
		assertEquals(1, JasperTimeline.at(900, false).frost());
		assertEquals(1, JasperTimeline.at(2000, false).frost());
		assertTrue(JasperTimeline.at(1000, true).frost() < 1);
	}

	@Test
	void theCrackRunsAcrossBeforeTheTitleLands() {
		long hit = JasperTimeline.hit(false);
		assertEquals(0, JasperTimeline.at(150, false).crack());
		assertEquals(0.5f, JasperTimeline.at((150 + hit) / 2, false).crack(), CLOSE);
		assertEquals(1, JasperTimeline.at(hit, false).crack());
		assertEquals(0, JasperTimeline.at(hit - 1, false).title());
		assertEquals(0, JasperTimeline.at(hit - 1, false).titleScale());
		assertEquals(-1, JasperTimeline.at(hit - 1, false).burst());
	}

	@Test
	void theTitleComesInBigAndSettles() {
		long hit = JasperTimeline.hit(false);
		assertEquals(3.5f, JasperTimeline.at(hit, false).titleScale(), CLOSE);
		assertEquals(1, JasperTimeline.at(hit, false).flash());
		assertEquals(0, JasperTimeline.at(hit, false).burst());
		float last = 10;
		for (long t = hit; t <= hit + 300; t += 5) {
			float scale = JasperTimeline.at(t, false).titleScale();
			assertTrue(scale <= last && scale >= 1, "at " + t);
			last = scale;
		}
		assertEquals(1, JasperTimeline.at(hit + 300, false).titleScale());
		assertEquals(1, JasperTimeline.at(hit + 300, false).title());
		assertEquals(0, JasperTimeline.at(hit + 200, false).flash());
		assertEquals(1, JasperTimeline.at(hit + 800, false).burst());
		assertEquals(5f, JasperTimeline.at(JasperTimeline.hit(true), true).titleScale(), CLOSE);
	}

	@Test
	void onlyTheBigOneSaysHowDryItWas() {
		for (long t = 0; t < JasperTimeline.total(false); t += 10) {
			assertEquals(0, JasperTimeline.at(t, false).dry(), "at " + t);
		}
		long hit = JasperTimeline.hit(true);
		assertEquals(0, JasperTimeline.at(hit + 600, true).dry());
		assertEquals(1, JasperTimeline.at(hit + 900, true).dry());
	}

	@Test
	void theLandingIsHeardOnce() {
		for (boolean big : new boolean[] {false, true}) {
			int heard = 0;
			for (long t = 0; t < JasperTimeline.total(big); t += 17) {
				heard += JasperTimeline.lands(t, t + 17, big) ? 1 : 0;
			}
			assertEquals(1, heard);
			assertFalse(JasperTimeline.lands(JasperTimeline.hit(big), JasperTimeline.hit(big) + 50, big));
			assertTrue(JasperTimeline.lands(JasperTimeline.hit(big) - 1, JasperTimeline.hit(big), big));
		}
	}

	@Test
	void theCrackGoesFromEdgeToEdgeInOnePiece() {
		for (int seed = 0; seed < 50; seed++) {
			List<Segment> crack = JasperTimeline.crack(seed);
			assertEquals(crack, JasperTimeline.crack(seed));
			List<Segment> main = crack.stream().filter(segment -> !segment.branch()).toList();
			assertEquals(0, main.getFirst().x1());
			assertEquals(1, main.getLast().x2(), CLOSE);
			for (int i = 1; i < main.size(); i++) {
				assertEquals(main.get(i - 1).x2(), main.get(i).x1());
				assertEquals(main.get(i - 1).y2(), main.get(i).y1());
				assertEquals(main.get(i - 1).to(), main.get(i).from(), CLOSE);
			}
			for (Segment segment : crack) {
				assertTrue(segment.y1() > 0.05f && segment.y1() < 0.95f);
				assertTrue(segment.y2() > 0.05f && segment.y2() < 0.95f);
				assertTrue(segment.from() < segment.to() && segment.to() <= 1);
			}
			assertTrue(crack.size() > main.size());
		}
		assertFalse(JasperTimeline.crack(1).equals(JasperTimeline.crack(2)));
	}

	@Test
	void noiseStaysBetweenZeroAndOne() {
		for (int i = 0; i < 10_000; i++) {
			float noise = JasperTimeline.noise(i * 31, i);
			assertTrue(noise >= 0 && noise < 1);
		}
		assertEquals(JasperTimeline.noise(5, 6), JasperTimeline.noise(5, 6));
	}
}
