package io.github.filipovych.filobulus.alert;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.filipovych.filobulus.alert.JasperTimeline.Frame;
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
			assertEquals(0, end.gust());
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
	void theGustBlowsInAndDiesDownBeforeTheEnd() {
		for (boolean big : new boolean[] {false, true}) {
			long hit = JasperTimeline.hit(big);
			assertEquals(1, JasperTimeline.at(hit, big).gust());
			assertTrue(JasperTimeline.at(100, big).gust() > 0);
			float last = 1;
			for (long t = hit; t <= JasperTimeline.total(big); t += 10) {
				float gust = JasperTimeline.at(t, big).gust();
				assertTrue(gust <= last, "at " + t);
				last = gust;
			}
			assertEquals(0, JasperTimeline.at(JasperTimeline.total(big) - 200, big).gust());
		}
	}

	@Test
	void theSnowOnlyEverMovesOneWayAndSlowsDown() {
		float last = 0;
		float step = Float.MAX_VALUE;
		for (long t = 100; t <= JasperTimeline.total(false); t += 100) {
			float drift = JasperTimeline.at(t, false).drift();
			assertTrue(drift >= last, "at " + t);
			assertTrue(drift - last <= step + 0.0001f, "at " + t);
			step = drift - last;
			last = drift;
		}
		assertTrue(JasperTimeline.at(1000, false).drift() > 0.5f);
	}

	@Test
	void theTitleLandsOnTheHit() {
		long hit = JasperTimeline.hit(false);
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
	void noiseStaysBetweenZeroAndOne() {
		for (int i = 0; i < 10_000; i++) {
			float noise = JasperTimeline.noise(i * 31, i);
			assertTrue(noise >= 0 && noise < 1);
		}
		assertEquals(JasperTimeline.noise(5, 6), JasperTimeline.noise(5, 6));
	}
}
