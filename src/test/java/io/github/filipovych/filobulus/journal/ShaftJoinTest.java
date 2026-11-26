package io.github.filipovych.filobulus.journal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ShaftJoinTest {
	@Test
	void aShaftWithoutThePortalLineIsJoined() {
		assertTrue(new ShaftJoin().joined(1_000_000));
	}

	@Test
	void theShaftWeFoundIsNotJoined() {
		ShaftJoin join = new ShaftJoin();
		join.portal(1_000_000);
		assertFalse(join.joined(1_020_000));
	}

	@Test
	void thePortalLineIsOldAfterTwoMinutes() {
		ShaftJoin join = new ShaftJoin();
		join.portal(1_000_000);
		assertTrue(join.joined(1_000_000 + ShaftJoin.PORTAL_MILLIS + 1));
		join.portal(2_000_000);
		assertFalse(join.joined(2_000_000 + ShaftJoin.PORTAL_MILLIS));
	}

	@Test
	void onePortalLineCountsForOneShaft() {
		ShaftJoin join = new ShaftJoin();
		join.portal(1_000_000);
		assertFalse(join.joined(1_010_000));
		// out again and into a party member's shaft a minute later
		assertTrue(join.joined(1_070_000));
	}

	@Test
	void aResumedShaftDoesNotUseThePortalLine() {
		ShaftJoin join = new ShaftJoin();
		VisitTracker tracker = new VisitTracker();
		Object lobby = new Object();
		join.portal(1_000);
		tracker.update(true, "FAIR_1", lobby, 5_000);
		assertFalse(join.entered(null, tracker.open(), 5_000));
		tracker.update(false, "", null, 60_000);
		// back into the same shaft after a disconnect, not a new one
		tracker.update(true, "FAIR_1", lobby, 70_000);
		assertFalse(join.entered(null, tracker.open(), 70_000));
		// a shaft we stay in is not entered again
		ShaftVisit open = tracker.open();
		tracker.update(true, "FAIR_1", lobby, 71_000);
		assertFalse(join.entered(open, tracker.open(), 71_000));
		// a new lobby a while later with no portal line before it
		tracker.update(false, "", null, 80_000);
		tracker.update(true, "FAIR_1", new Object(), 200_000);
		assertTrue(join.entered(null, tracker.open(), 200_000));
	}
}
