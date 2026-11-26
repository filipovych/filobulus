package io.github.filipovych.filobulus.alert;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.filipovych.filobulus.alert.JasperStorm.Cell;
import io.github.filipovych.filobulus.alert.JasperStorm.Flake;
import io.github.filipovych.filobulus.alert.JasperStorm.Spot;
import io.github.filipovych.filobulus.alert.JasperTimeline.Frame;
import java.util.List;
import org.junit.jupiter.api.Test;

class JasperStormTest {
	@Test
	void theMiddleOfTheScreenIsNeverSnowedOn() {
		for (boolean big : new boolean[] {false, true}) {
			for (int seed = 0; seed < 20; seed++) {
				List<Flake> flakes = JasperStorm.flakes(seed, big);
				for (long t = 0; t <= JasperTimeline.total(big); t += 25) {
					Frame frame = JasperTimeline.at(t, big);
					for (Spot spot : JasperStorm.spots(flakes, seed, frame.gust(), frame.drift())) {
						assertTrue(spot.alpha() > 0 && spot.alpha() <= 1);
						// the head and the end of its streak both stay out of the middle
						assertTrue(JasperStorm.far(spot.x(), spot.y()) >= JasperStorm.CLEAR, "at " + t);
						assertTrue(JasperStorm.far(Math.max(0, spot.x() - spot.tail()), spot.y()) >= 0.4f, "at " + t);
					}
				}
			}
		}
	}

	@Test
	void theCrosshairAreaIsClearAtAnyGust() {
		for (float gust = 0; gust <= 1; gust += 0.05f) {
			for (float x = 0.3f; x <= 0.7f; x += 0.01f) {
				for (float y = 0.3f; y <= 0.7f; y += 0.01f) {
					assertEquals(0, JasperStorm.edge(x, y, gust));
				}
			}
			assertEquals(1, JasperStorm.edge(0, 0, gust));
		}
	}

	@Test
	void theStormPushesInFurtherAtAStrongGust() {
		assertTrue(JasperStorm.edge(0.1f, 0.5f, 1) > JasperStorm.edge(0.1f, 0.5f, 0.2f));
	}

	@Test
	void thereIsNoSnowWithoutWindAndItThinsOut() {
		List<Flake> flakes = JasperStorm.flakes(3, false);
		assertTrue(JasperStorm.spots(flakes, 3, 0, 1).isEmpty());
		int strong = JasperStorm.spots(flakes, 3, 1, 0.3f).size();
		int weak = JasperStorm.spots(flakes, 3, 0.3f, 0.3f).size();
		assertTrue(strong > weak && weak > 0);
		assertTrue(JasperStorm.flakes(3, true).size() > flakes.size());
		assertTrue(flakes.stream().anyMatch(flake -> flake.tint() == JasperStorm.Tint.PINK));
	}

	@Test
	void theCornerFrostIsThickInTheCornerAndGrowsOutwards() {
		for (int seed = 0; seed < 20; seed++) {
			List<Cell> cells = JasperStorm.frost(seed);
			assertEquals(cells, JasperStorm.frost(seed));
			assertTrue(cells.stream().anyMatch(cell -> cell.dx() == 0 && cell.dy() == 0 && cell.at() < 0.2f));
			for (Cell cell : cells) {
				assertTrue(cell.at() >= 0 && cell.at() < 1);
				assertTrue(cell.dx() < JasperStorm.CELLS && cell.dy() < JasperStorm.CELLS);
			}
		}
		assertFalse(JasperStorm.frost(1).equals(JasperStorm.frost(2)));
	}

	@Test
	void theShineRunsOutFromTheCornerAfterTheHit() {
		long hit = JasperTimeline.hit(false);
		assertEquals(0, JasperStorm.shine(0, hit - 300, false));
		assertEquals(1, JasperStorm.shine(0, hit, false));
		assertEquals(1, JasperStorm.shine(0.5f, hit + 250, false));
		assertEquals(0, JasperStorm.shine(0, hit + 250, false));
	}
}
