package io.github.filipovych.filobulus.mining;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class BreakpointsTest {
	@Test
	void ticksMatchTheWikiTables() {
		// ruby at 2,500 strength: 1,240 speed for 60 ticks, 2,460 for 30
		assertEquals(60, Breakpoints.ticks(2500, 1240, false));
		assertEquals(61, Breakpoints.ticks(2500, 1239, false));
		assertEquals(30, Breakpoints.ticks(2500, 2460, false));
		assertEquals(1240, Breakpoints.speedFor(2500, 60, false));
		assertEquals(2460, Breakpoints.speedFor(2500, 30, false));
	}

	@Test
	void neverUnderFourTicksUntilItInstamines() {
		assertEquals(4, Breakpoints.ticks(2500, 100_000, false));
		assertEquals(4, Breakpoints.ticks(2500, 150_000, false));
		assertEquals(1, Breakpoints.ticks(2500, 150_001, false));
		assertEquals(1, Breakpoints.ticks(50, 1501, true));
	}

	@Test
	void theNextBreakpointIsOneTickLessOrTheInstamine() {
		Breakpoints.Next next = Breakpoints.next(2500, 2460, false);
		assertEquals(29, next.ticks());
		assertEquals(Breakpoints.speedFor(2500, 29, false), next.speed());
		assertEquals(1, Breakpoints.next(2500, 100_000, false).ticks());
		assertEquals(0, Breakpoints.next(2500, 200_000, false).ticks());
	}

	@Test
	void halfThePingComesOffForTheSwap() {
		assertEquals(18, Breakpoints.swapTick(20, 200));
		assertEquals(1, Breakpoints.swapTick(2, 400));
	}

	@Test
	void glassAndPanesAreTheSameGemstone() {
		assertEquals(BlockStrength.JASPER, BlockStrength.ofBlock("minecraft:magenta_stained_glass_pane").orElseThrow());
		assertEquals(4800, BlockStrength.JASPER.strength(false));
		assertEquals(4000, BlockStrength.JASPER.strength(true));
	}
}
