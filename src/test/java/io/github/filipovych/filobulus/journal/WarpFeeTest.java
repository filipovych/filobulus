package io.github.filipovych.filobulus.journal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.filipovych.filobulus.corpse.CorpseType;
import org.junit.jupiter.api.Test;

class WarpFeeTest {
	private static final long COST = 1_500_000;

	@Test
	void aJoinedVanguardShaftPaysOnce() {
		WarpFee fee = new WarpFee();
		ShaftVisit shaft = ShaftVisit.entered(1000, "FAIR_1").withJoined(true);
		assertEquals(COST, fee.take(shaft, CorpseType.VANGUARD, COST));
		assertEquals(0, fee.take(shaft, CorpseType.VANGUARD, COST));
		// the same shaft after a disconnect, it still paid already
		assertEquals(0, fee.take(shaft.withSpent(0), CorpseType.LAPIS, COST));
	}

	@Test
	void theNextShaftPaysAgain() {
		WarpFee fee = new WarpFee();
		assertEquals(COST, fee.take(ShaftVisit.entered(1000, "FAIR_1").withJoined(true), CorpseType.VANGUARD, COST));
		assertEquals(COST, fee.take(ShaftVisit.entered(90_000, "FAIR_1").withJoined(true), CorpseType.VANGUARD, COST));
	}

	@Test
	void onlyJoinedVanguardShaftsPay() {
		WarpFee fee = new WarpFee();
		assertEquals(0, fee.take(ShaftVisit.entered(1000, "FAIR_1"), CorpseType.VANGUARD, COST));
		assertEquals(0, fee.take(ShaftVisit.entered(2000, "TUNG_1").withJoined(true), CorpseType.TUNGSTEN, COST));
		assertEquals(0, fee.take(ShaftVisit.entered(3000, "FAIR_1").withJoined(true), CorpseType.VANGUARD, 0));
		assertEquals(0, fee.take(null, CorpseType.VANGUARD, COST));
	}

	@Test
	void aVanguardCorpseIsEnoughWhenTheTypeWasNotRead() {
		WarpFee fee = new WarpFee();
		assertEquals(0, fee.take(ShaftVisit.entered(1000, "").withJoined(true), CorpseType.LAPIS, COST));
		assertEquals(COST, fee.take(ShaftVisit.entered(1000, "").withJoined(true), CorpseType.VANGUARD, COST));
	}
}
