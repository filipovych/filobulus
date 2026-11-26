package io.github.filipovych.filobulus.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.filipovych.filobulus.hud.FrostPattern.Tick;
import java.util.List;
import org.junit.jupiter.api.Test;

class FrostPatternTest {
	@Test
	void aPanelAlwaysGetsTheSameFrost() {
		assertEquals(FrostPattern.ticks("Mining panel", 120, 60), FrostPattern.ticks("Mining panel", 120, 60));
		assertNotEquals(FrostPattern.ticks("Mining panel", 120, 60), FrostPattern.ticks("Profit tracker", 120, 60));
	}

	@Test
	void theFrostStaysOnTheEdgeInsideThePanel() {
		List<Tick> ticks = FrostPattern.ticks("Profit tracker", 140, 80);
		assertTrue(ticks.size() > 40);
		for (Tick tick : ticks) {
			assertTrue(tick.x() >= 0 && tick.y() >= 0, tick.toString());
			assertTrue(tick.x() + tick.width() <= 140 && tick.y() + tick.height() <= 80, tick.toString());
			boolean edge = tick.x() == 0 || tick.y() == 0 || tick.x() + tick.width() == 140 || tick.y() + tick.height() == 80;
			boolean corner = Math.min(tick.x(), 139 - tick.x()) + Math.min(tick.y(), 79 - tick.y()) < 6;
			assertTrue(edge || corner, tick.toString());
			assertTrue(tick.width() <= 4 && tick.height() <= 4, tick.toString());
			assertTrue(tick.bright() > 0 && tick.bright() <= 1);
		}
	}

	@Test
	void theCornersAreNeverBare() {
		List<Tick> ticks = FrostPattern.ticks("Auto recap", 100, 50);
		assertTrue(ticks.stream().anyMatch(tick -> tick.x() == 0 && tick.y() == 0));
		assertTrue(ticks.stream().anyMatch(tick -> tick.x() + tick.width() >= 98 && tick.y() == 0));
	}

	@Test
	void aTinyPanelHasNone() {
		assertEquals(List.of(), FrostPattern.ticks("x", 2, 40));
	}

	@Test
	void theShimmerIsSlowAndNeverGoesOut() {
		Tick tick = new Tick(0, 0, 1, 2, 1.3f, 0.8f);
		for (long now = 0; now < 20_000; now += 50) {
			float shimmer = FrostPattern.shimmer(tick, now);
			assertTrue(shimmer >= 0.8f * 0.35f - 0.001f && shimmer <= 0.8f + 0.001f);
			assertTrue(Math.abs(shimmer - FrostPattern.shimmer(tick, now + 50)) < 0.05f);
		}
	}

	@Test
	void aGlintCrossesOnceAndIsGone() {
		assertEquals(-1, FrostPattern.glint(-1));
		assertEquals(0, FrostPattern.glint(0));
		assertEquals(0.5f, FrostPattern.glint(FrostPattern.GLINT_MILLIS / 2), 0.01f);
		assertEquals(-1, FrostPattern.glint(FrostPattern.GLINT_MILLIS));
		float last = -1;
		for (long t = 0; t < FrostPattern.GLINT_MILLIS; t += 10) {
			assertTrue(FrostPattern.glint(t) >= last);
			last = FrostPattern.glint(t);
		}
	}
}
