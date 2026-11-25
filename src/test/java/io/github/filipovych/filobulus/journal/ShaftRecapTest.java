package io.github.filipovych.filobulus.journal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.filipovych.filobulus.corpse.CorpseTally;
import java.util.List;
import org.junit.jupiter.api.Test;

class ShaftRecapTest {
	private static ShaftVisit paid(long at, String code, long coins) {
		return new ShaftVisit(at, code, 60_000, 0, CorpseTally.NONE, 0, coins);
	}

	@Test
	void theAverageIsOverEarlierShaftsOfTheSameLayoutThatPaid() {
		List<ShaftVisit> journal = List.of(
				paid(1, "RUBY_1", 2_000_000), paid(2, "ONYX_1", 90_000_000), paid(3, "RUBY_1", 4_000_000),
				paid(4, "RUBY_1", 0), paid(5, "RUBY_1", 6_000_000), paid(6, "RUBY_1", 50_000_000), paid(7, "RUBY_1", 1));
		ShaftRecap recap = ShaftRecap.of(journal.get(5), journal);
		assertTrue(recap.hasAverage());
		assertEquals(4_000_000, recap.average());
		assertEquals(3, recap.sampled());
		assertEquals(5, recap.number());
	}

	@Test
	void tooFewEarlierShaftsGiveNoAverage() {
		List<ShaftVisit> journal = List.of(paid(1, "RUBY_1", 2_000_000), paid(2, "RUBY_1", 4_000_000), paid(3, "RUBY_1", 9));
		ShaftRecap recap = ShaftRecap.of(journal.get(2), journal);
		assertFalse(recap.hasAverage());
		assertEquals(0, recap.average());
		assertEquals(3, recap.number());
	}

	@Test
	void crystalShaftsAreOneLayout() {
		List<ShaftVisit> journal = List.of(paid(1, "RUBY_C", 1), paid(2, "ONYX_C", 1), paid(3, "JASP_C", 1));
		assertEquals(3, ShaftRecap.of(journal.get(2), journal).number());
	}

	@Test
	void theDryStreakCountsUpToTheShaftAndAJasperSaysWhatItEnded() {
		List<ShaftVisit> journal = List.of(
				paid(1, "JASP_1", 1), paid(2, "RUBY_1", 1), paid(3, "ONYX_1", 1), paid(4, "RUBY_1", 1), paid(5, "JASP_C", 1),
				paid(6, "RUBY_1", 1));
		assertEquals(3, ShaftRecap.of(journal.get(3), journal).dry());
		assertEquals(3, ShaftRecap.of(journal.get(4), journal).dry());
		assertEquals(1, ShaftRecap.of(journal.get(5), journal).dry());
		assertEquals(0, ShaftRecap.of(journal.get(0), journal).dry());
	}

	@Test
	void coinsPerHourNeedAShaftOfSomeLength() {
		ShaftVisit quick = new ShaftVisit(1, "RUBY_1", 5000, 0, CorpseTally.NONE, 0, 1_000_000);
		assertEquals(0, ShaftRecap.of(quick, List.of(quick)).perHour());
		ShaftVisit normal = new ShaftVisit(1, "RUBY_1", 120_000, 0, CorpseTally.NONE, 1_000_000, 3_000_000);
		assertEquals(120_000_000, ShaftRecap.of(normal, List.of(normal)).perHour());
	}
}
