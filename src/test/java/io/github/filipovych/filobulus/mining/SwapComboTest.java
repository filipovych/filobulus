package io.github.filipovych.filobulus.mining;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.filipovych.filobulus.mining.SwapCombo.Event;
import io.github.filipovych.filobulus.mining.SwapCombo.Rank;
import io.github.filipovych.filobulus.mining.SwapCombo.Timing;
import org.junit.jupiter.api.Test;

class SwapComboTest {
	@Test
	void theWindowHasOneTickOfSlackEachSide() {
		// a 10 tick block with the swap at tick 7
		assertEquals(Timing.EARLY, SwapCombo.timing(5, 7, 10));
		assertEquals(Timing.GLIDE, SwapCombo.timing(6, 7, 10));
		assertEquals(Timing.GLIDE, SwapCombo.timing(9, 7, 10));
		assertEquals(Timing.GLIDE, SwapCombo.timing(11, 7, 10));
		assertEquals(Timing.LATE, SwapCombo.timing(12, 7, 10));
	}

	@Test
	void instaminesAndUnknownBlocksHaveNoWindow() {
		assertEquals(Timing.NONE, SwapCombo.timing(1, 1, 1));
		assertEquals(Timing.NONE, SwapCombo.timing(5, 0, 0));
	}

	@Test
	void goodSwapsCountAndClimbTheRanks() {
		SwapCombo combo = new SwapCombo(0);
		assertEquals(Event.GOOD, combo.good(0));
		assertEquals(Event.GOOD, combo.good(1));
		// frost fills in three swaps
		assertEquals(Event.RANK_UP, combo.good(2));
		assertEquals(Rank.CHILL, combo.rank());
		assertEquals(3, combo.count());
		assertTrue(combo.active());
	}

	@Test
	void theTopRankStaysFull() {
		SwapCombo combo = new SwapCombo(0);
		for (int i = 0; i < 200; i++) {
			combo.good(i);
		}
		assertEquals(Rank.ABSOLUTE_ZERO, combo.rank());
		assertEquals(1, combo.meter());
		assertEquals(200, combo.count());
	}

	@Test
	void anEarlySwapBreaksIt() {
		SwapCombo combo = new SwapCombo(0);
		for (int i = 0; i < 10; i++) {
			combo.good(i);
		}
		assertEquals(Event.BROKEN, combo.broken());
		assertEquals(0, combo.count());
		assertEquals(Rank.FROST, combo.rank());
		assertFalse(combo.active());
		// nothing to break when there is no combo
		assertEquals(Event.NONE, combo.broken());
	}

	@Test
	void idlingDrainsOneRankAtATimeThenEnds() {
		SwapCombo combo = new SwapCombo(0);
		int tick = 0;
		while (combo.rank() != Rank.BRITTLE) {
			combo.good(tick++);
		}
		int last = tick;
		// a short pause costs nothing but the small decay
		for (; tick < last + SwapCombo.IDLE_TICKS; tick++) {
			assertEquals(Event.NONE, combo.tick(tick));
		}
		int downs = 0;
		Event event = Event.NONE;
		while (event != Event.ENDED && tick < 10_000) {
			event = combo.tick(tick++);
			if (event == Event.RANK_DOWN) {
				downs++;
			}
		}
		assertEquals(2, downs);
		assertEquals(Event.ENDED, event);
		assertFalse(combo.active());
		assertEquals(0, combo.count());
	}

	@Test
	void theBestIsKeptAndSavedOnce() {
		SwapCombo combo = new SwapCombo(5);
		for (int i = 0; i < 5; i++) {
			combo.good(i);
		}
		assertFalse(combo.takeNewBest());
		combo.good(6);
		assertEquals(6, combo.best());
		assertTrue(combo.takeNewBest());
		assertFalse(combo.takeNewBest());
		combo.broken();
		assertEquals(6, combo.best());
	}
}
