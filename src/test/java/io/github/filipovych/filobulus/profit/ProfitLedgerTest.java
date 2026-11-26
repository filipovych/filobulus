package io.github.filipovych.filobulus.profit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

// what counts as a gain and what is only items being moved around
class ProfitLedgerTest {
	private static final String JASPER = "FLAWED_JASPER_GEM";
	private static final String GLACITE = "GLACITE";
	private static final long SECOND = 1000;
	private static final String FLAWED = "FLAWED_RUBY_GEM";
	private static final String FINE = "FINE_RUBY_GEM";

	private ProfitTotals session;
	private ProfitTotals allTime;
	private ProfitLedger ledger;

	@BeforeEach
	void setUp() {
		session = new ProfitTotals();
		allTime = new ProfitTotals(Map.of(JASPER, 1000L), 0);
		ledger = new ProfitLedger(List.of(session, allTime));
	}

	@Test
	void whatLandsInTheSacksOrTheInventoryIsAGainForEveryTotal() {
		ledger.sack(JASPER, 64, SECOND);
		ledger.inventory(GLACITE, 3, 2 * SECOND, true);
		assertEquals(Map.of(JASPER, 64L, GLACITE, 3L), session.items());
		assertEquals(Map.of(JASPER, 1064L, GLACITE, 3L), allTime.items());
	}

	@Test
	void whatComesInWhileAMenuIsOpenIsNoLoot() {
		ledger.inventory(JASPER, 64, SECOND, false);
		assertTrue(session.isEmpty());
	}

	@Test
	void losingItemsIsNeverALoss() {
		ledger.sack(JASPER, 64, SECOND);
		ledger.sack(JASPER, -64, 200 * SECOND);
		ledger.inventory(GLACITE, 5, 300 * SECOND, true);
		ledger.inventory(GLACITE, -5, 400 * SECOND, true);
		assertEquals(Map.of(JASPER, 64L, GLACITE, 5L), session.items());
	}

	@Test
	void takingItemsOutOfTheSackIsNoGainWhicheverSideIsSeenFirst() {
		ledger.sack(JASPER, 100, SECOND);
		// the sack line first, then the items show in the inventory
		ledger.sack(JASPER, -40, 10 * SECOND);
		ledger.inventory(JASPER, 40, 11 * SECOND, true);
		assertEquals(Map.of(JASPER, 100L), session.items());
		// the items first, the sack line a while after
		ledger.inventory(JASPER, 30, 20 * SECOND, true);
		assertEquals(Map.of(JASPER, 130L), session.items());
		ledger.sack(JASPER, -30, 45 * SECOND);
		assertEquals(Map.of(JASPER, 100L), session.items());
	}

	@Test
	void takingItemsOutThroughTheSackMenuDoesNotEatALaterGain() {
		// not counted on the way in, so the sack line has nothing to take back
		ledger.inventory(JASPER, 40, SECOND, false);
		ledger.sack(JASPER, -40, 5 * SECOND);
		ledger.inventory(JASPER, 7, 6 * SECOND, true);
		assertEquals(Map.of(JASPER, 7L), session.items());
	}

	@Test
	void puttingItemsIntoTheSackIsNoGain() {
		ledger.inventory(JASPER, -64, SECOND, false);
		ledger.sack(JASPER, 70, 20 * SECOND);
		assertEquals(Map.of(JASPER, 6L), session.items());
	}

	@Test
	void aMoveIsOnlyHeldAgainstTheOtherSideForAMinute() {
		ledger.sack(JASPER, -40, SECOND);
		ledger.inventory(JASPER, 40, 90 * SECOND, true);
		assertEquals(Map.of(JASPER, 40L), session.items());
	}

	@Test
	void whatLandsInTheSacksRightAfterBuyingIsNoLoot() {
		ledger.quiet(SECOND);
		ledger.sack(JASPER, 64, 30 * SECOND);
		assertTrue(session.isEmpty());
		ledger.sack(JASPER, 64, 62 * SECOND);
		assertEquals(Map.of(JASPER, 64L), session.items());
	}

	@Test
	void theClockOnlyRunsWhileGainsKeepComing() {
		ledger.idle(60 * SECOND);
		ledger.sack(JASPER, 1, 0);
		ledger.sack(JASPER, 1, 30 * SECOND);
		ledger.sack(JASPER, 1, 50 * SECOND);
		assertEquals(50 * SECOND, session.activeMillis(50 * SECOND, 60 * SECOND));
		// the time since the last gain counts for now
		assertEquals(80 * SECOND, session.activeMillis(80 * SECOND, 60 * SECOND));
		// and is dropped again once it turns out to be a break
		assertEquals(50 * SECOND, session.activeMillis(500 * SECOND, 60 * SECOND));
		ledger.sack(JASPER, 1, 600 * SECOND);
		ledger.sack(JASPER, 1, 610 * SECOND);
		assertEquals(60 * SECOND, session.activeMillis(610 * SECOND, 60 * SECOND));
	}

	@Test
	void clearingATotalLeavesTheOtherOneAlone() {
		ledger.sack(JASPER, 64, SECOND);
		session.clear();
		assertTrue(session.isEmpty());
		assertEquals(0, session.activeMillis(2 * SECOND, 60 * SECOND));
		assertEquals(Map.of(JASPER, 1064L), allTime.items());
	}

	@Test
	void theFirstSackMessageStartsTheClockAsFarBackAsItAddsUp() {
		ledger.sack(JASPER, 64, 100 * SECOND, 30 * SECOND);
		assertEquals(30 * SECOND, session.activeMillis(100 * SECOND, 60 * SECOND));
	}

	@Test
	void corpseLootCountsOnceAsTheCorpse() {
		ledger.corpse(2_000_000, List.of(Map.entry(JASPER, 2L)), SECOND);
		ledger.sack(JASPER, 2, 2 * SECOND);
		ledger.inventory(JASPER, 1, 3 * SECOND, true);
		assertEquals(Map.of(JASPER, 1L), session.items());
		assertEquals(1, session.corpses());
		assertEquals(2_000_000, session.corpseCoins());
	}

	@Test
	void aCompactorDoesNotCountTwice() {
		ledger.inventory("ROUGH_RUBY_GEM", 80, SECOND, true);
		ledger.inventory("ROUGH_RUBY_GEM", -80, 2 * SECOND, true);
		ledger.inventory("FLAWED_RUBY_GEM", 1, 2 * SECOND, true);
		assertEquals(Map.of("FLAWED_RUBY_GEM", 1L), session.items());
	}

	@Test
	void craftingFromTheSacksInOneMessageCountsOnlyTheOutput() {
		ledger.sack(FLAWED, 8000, SECOND);
		ledger.sack(FINE, 100, 40 * SECOND);
		ledger.sack(FLAWED, -8000, 40 * SECOND);
		assertEquals(Map.of(FINE, 100L), session.items());
	}

	@Test
	void theInputLineBeforeTheOutputIsACraft() {
		ledger.sack(FLAWED, 8000, SECOND);
		ledger.sack(FLAWED, -8000, 10 * SECOND);
		// the output lands in the inventory while the crafting menu is still open
		ledger.inventory(FINE, 100, 11 * SECOND, false);
		assertEquals(Map.of(FINE, 100L), session.items());
	}

	@Test
	void theOutputBeforeTheInputLineIsACraft() {
		ledger.sack(FLAWED, 8000, SECOND);
		ledger.inventory(FINE, 100, 10 * SECOND, false);
		assertEquals(Map.of(FLAWED, 8000L), session.items());
		ledger.sack(FLAWED, -8000, 35 * SECOND);
		assertEquals(Map.of(FINE, 100L), session.items());
		// the fines go into the sacks afterwards, that is still no gain
		ledger.inventory(FINE, -100, 40 * SECOND, false);
		ledger.sack(FINE, 100, 65 * SECOND);
		assertEquals(Map.of(FINE, 100L), session.items());
	}

	@Test
	void anOutputInTheSacksBeforeItsInputLineIsACraft() {
		ledger.sack(FLAWED, 8000, SECOND);
		ledger.sack(FINE, 100, 30 * SECOND);
		ledger.sack(FLAWED, -8000, 30 * SECOND);
		assertEquals(Map.of(FINE, 100L), session.items());
	}

	@Test
	void anInventoryCompactorTakesBackOnlyWhatWentIn() {
		ledger.inventory(FLAWED, 200, SECOND, true);
		ledger.inventory(FLAWED, -160, 2 * SECOND, true);
		ledger.inventory(FINE, 2, 2 * SECOND, true);
		assertEquals(Map.of(FLAWED, 40L, FINE, 2L), session.items());
	}

	@Test
	void aWithdrawalFromTheSacksStaysNeutral() {
		ledger.sack(FLAWED, 500, SECOND);
		ledger.sack(FINE, 10, SECOND);
		// the sack menu, items first, the sack line half a minute later
		ledger.inventory(FLAWED, 320, 5 * SECOND, false);
		ledger.inventory(FINE, 4, 5 * SECOND, false);
		ledger.sack(FLAWED, -320, 30 * SECOND);
		ledger.sack(FINE, -4, 30 * SECOND);
		assertEquals(Map.of(FLAWED, 500L, FINE, 10L), session.items());
		// and the sack line first
		ledger.sack(FLAWED, -80, 40 * SECOND);
		ledger.inventory(FLAWED, 80, 41 * SECOND, true);
		ledger.inventory(FINE, 1, 42 * SECOND, true);
		assertEquals(Map.of(FLAWED, 500L, FINE, 11L), session.items());
	}

	@Test
	void sellingTheInputFromTheSacksIsNoCraft() {
		ledger.sack("ROUGH_RUBY_GEM", 8000, SECOND);
		ledger.sack(FLAWED, 10, 30 * SECOND);
		ledger.quiet(40 * SECOND);
		ledger.sack("ROUGH_RUBY_GEM", -800, 45 * SECOND);
		assertEquals(Map.of("ROUGH_RUBY_GEM", 8000L, FLAWED, 10L), session.items());
	}

	@Test
	void miningKeepsTheClockRunningWithoutAddingItems() {
		ledger.idle(60 * SECOND);
		ledger.sack(JASPER, 1, 0);
		for (long at = 30; at <= 150; at += 30) {
			ledger.active(at * SECOND);
		}
		ledger.sack(JASPER, 1, 200 * SECOND);
		assertEquals(200 * SECOND, session.activeMillis(200 * SECOND, 60 * SECOND));
		assertEquals(Map.of(JASPER, 2L), session.items());
	}

	@Test
	void miningStartsTheClockButAnEmptyTotalStaysEmpty() {
		ProfitTotals totals = new ProfitTotals();
		totals.active(10 * SECOND, 60 * SECOND);
		totals.active(20 * SECOND, 60 * SECOND);
		assertTrue(totals.isEmpty());
		assertEquals(10 * SECOND, totals.activeMillis(20 * SECOND, 60 * SECOND));
		// a break still stops it
		totals.active(200 * SECOND, 60 * SECOND);
		assertEquals(10 * SECOND, totals.activeMillis(200 * SECOND, 60 * SECOND));
	}
}
