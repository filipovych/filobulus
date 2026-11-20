package io.github.filipovych.filobulus.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class HudPlacementTest {
	private static final int WIDTH = 80;
	private static final int HEIGHT = 40;

	private static HudPlacement dropped(int left, int top) {
		return HudPlacement.at(left, top, WIDTH, HEIGHT, 960, 540, 1f);
	}

	@Test
	void comesBackWhereItWasDropped() {
		for (int left : new int[] {0, 7, 440, 613, 880}) {
			for (int top : new int[] {0, 9, 250, 377, 500}) {
				HudPlacement placement = dropped(left, top);
				assertEquals(left, placement.left(WIDTH, 960));
				assertEquals(top, placement.top(HEIGHT, 540));
			}
		}
	}

	@Test
	void picksTheNearestAnchor() {
		assertEquals(HudAnchor.START, dropped(10, 10).anchorX());
		assertEquals(HudAnchor.CENTRE, dropped(440, 250).anchorX());
		assertEquals(HudAnchor.CENTRE, dropped(440, 250).anchorY());
		assertEquals(HudAnchor.END, dropped(870, 490).anchorX());
		assertEquals(HudAnchor.END, dropped(870, 490).anchorY());
	}

	@Test
	void keepsItsGapToTheEdgeWhenTheWindowChanges() {
		HudPlacement right = dropped(870, 490);
		assertEquals(1920 - 10 - WIDTH, right.left(WIDTH, 1920));
		assertEquals(1080 - 10 - HEIGHT, right.top(HEIGHT, 1080));

		HudPlacement middle = dropped(440, 250);
		assertEquals((1920 - WIDTH) / 2, middle.left(WIDTH, 1920));
	}

	@Test
	void staysOnScreenWhenTheWindowShrinks() {
		assertEquals(320 - WIDTH, dropped(600, 10).left(WIDTH, 320));
		assertEquals(0, dropped(870, 10).left(WIDTH, 60));
		assertEquals(0, new HudPlacement(HudAnchor.START, -30, HudAnchor.START, -5, 1f).left(WIDTH, 960));
	}

	@Test
	void snapsToEdgesAndMiddleOnlyWhenClose() {
		assertEquals(0, HudPlacement.snap(4, WIDTH, 960));
		assertEquals(0, HudPlacement.snap(-3, WIDTH, 960));
		assertEquals(880, HudPlacement.snap(876, WIDTH, 960));
		assertEquals(440, HudPlacement.snap(445, WIDTH, 960));
		assertEquals(300, HudPlacement.snap(300, WIDTH, 960));
	}

	@Test
	void keepsTheScaleInRange() {
		assertEquals(HudPlacement.MIN_SCALE, dropped(0, 0).withScale(0.1f).scale());
		assertEquals(HudPlacement.MAX_SCALE, dropped(0, 0).withScale(9f).scale());
		assertEquals(1.5f, dropped(0, 0).withScale(1.5f).scale());
	}
}
