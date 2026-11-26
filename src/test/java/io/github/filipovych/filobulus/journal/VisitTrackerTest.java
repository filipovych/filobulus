package io.github.filipovych.filobulus.journal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.filipovych.filobulus.corpse.CorpseTally;
import io.github.filipovych.filobulus.corpse.CorpseType;
import java.util.List;
import org.junit.jupiter.api.Test;

class VisitTrackerTest {
	private static final Object TUNNELS = new Object();
	private static final Object SHAFT = new Object();
	private static final Object OTHER_SHAFT = new Object();

	@Test
	void aShaftIsLoggedTheMomentWeAreIn() {
		VisitTracker tracker = new VisitTracker();
		assertTrue(tracker.update(false, "", TUNNELS, 500).isEmpty());
		assertEquals(List.of(ShaftVisit.entered(1000, "JASP_1")), tracker.update(true, "JASP_1", SHAFT, 1000));
		assertTrue(tracker.update(true, "JASP_1", SHAFT, 2000).isEmpty());
	}

	@Test
	void theTypeIsFilledInWhenItIsRead() {
		VisitTracker tracker = new VisitTracker();
		assertEquals(List.of(ShaftVisit.entered(1000, "")), tracker.update(true, "", SHAFT, 1000));
		assertEquals(List.of(ShaftVisit.entered(1000, "RUBY_2")), tracker.update(true, "RUBY_2", SHAFT, 1500));
	}

	@Test
	void leavingWritesTheTimeInside() {
		VisitTracker tracker = new VisitTracker();
		tracker.update(true, "JASP_1", SHAFT, 1000);
		assertEquals(List.of(new ShaftVisit(1000, "JASP_1", 0, 170_000)), tracker.ran(170_000));
		assertEquals(List.of(new ShaftVisit(1000, "JASP_1", 180_000, 170_000)),
				tracker.update(false, "", TUNNELS, 181_000));
		assertNull(tracker.open());
		assertTrue(tracker.ran(5000).isEmpty());
	}

	@Test
	void theNextShaftIsANewVisit() {
		VisitTracker tracker = new VisitTracker();
		tracker.update(true, "JASP_1", SHAFT, 1000);
		tracker.update(false, "", TUNNELS, 61_000);
		assertEquals(List.of(ShaftVisit.entered(200_000, "JASP_1")),
				tracker.update(true, "JASP_1", OTHER_SHAFT, 200_000));
	}

	@Test
	void aReadThatFellOutDoesNotLogTheShaftTwice() {
		VisitTracker tracker = new VisitTracker();
		tracker.update(true, "JASP_1", SHAFT, 1000);
		tracker.update(false, "", SHAFT, 61_000);
		assertEquals(List.of(ShaftVisit.entered(1000, "JASP_1")), tracker.update(true, "", SHAFT, 100_000));
		assertEquals(List.of(new ShaftVisit(1000, "JASP_1", 119_000, 0)), tracker.update(false, "", TUNNELS, 120_000));
	}

	@Test
	void theSameTypeRightAfterLeavingIsTheSameShaft() {
		VisitTracker tracker = new VisitTracker();
		tracker.update(true, "JASP_1", SHAFT, 1000);
		tracker.update(false, "", TUNNELS, 61_000);
		assertEquals(List.of(ShaftVisit.entered(1000, "JASP_1")), tracker.update(true, "JASP_1", OTHER_SHAFT, 64_000));
	}

	@Test
	void aStaleReadRightAfterLeavingIsNotANewShaft() {
		VisitTracker tracker = new VisitTracker();
		tracker.update(true, "JASP_1", SHAFT, 1000);
		tracker.update(false, "", TUNNELS, 61_000);
		assertEquals(List.of(ShaftVisit.entered(1000, "JASP_1")), tracker.update(true, "", TUNNELS, 62_000));
		// a real new shaft shows its own type and is split off then
		assertEquals(List.of(new ShaftVisit(1000, "JASP_1", 64_000, 0), ShaftVisit.entered(65_000, "RUBY_1")),
				tracker.update(true, "RUBY_1", TUNNELS, 65_000));
	}

	@Test
	void anotherTypeInTheSameWorldIsAnotherShaft() {
		VisitTracker tracker = new VisitTracker();
		tracker.update(true, "JASP_1", SHAFT, 1000);
		assertEquals(List.of(new ShaftVisit(1000, "JASP_1", 4000, 0), ShaftVisit.entered(5000, "RUBY_1")),
				tracker.update(true, "RUBY_1", SHAFT, 5000));
	}

	@Test
	void corpsesAreWrittenWhenTheTabListSaysSomethingNew() {
		VisitTracker tracker = new VisitTracker();
		assertTrue(tracker.corpses(new CorpseTally("LT", "")).isEmpty());
		tracker.update(true, "JASP_1", SHAFT, 1000);
		assertTrue(tracker.corpses(CorpseTally.NONE).isEmpty());
		assertEquals(List.of(new ShaftVisit(1000, "JASP_1", 0, 0, new CorpseTally("LT", ""), 0, 0)),
				tracker.corpses(new CorpseTally("LT", "")));
		assertTrue(tracker.corpses(new CorpseTally("LT", "")).isEmpty());
	}

	@Test
	void anOpenedCorpseAddsItsCoinsAndStaysAfterLeaving() {
		VisitTracker tracker = new VisitTracker();
		assertTrue(tracker.looted(CorpseType.UMBER, 5).isEmpty());
		tracker.update(true, "JASP_1", SHAFT, 1000);
		tracker.corpses(new CorpseTally("LTU", ""));
		tracker.looted(CorpseType.TUNGSTEN, 1_200_000);
		assertEquals(List.of(new ShaftVisit(1000, "JASP_1", 0, 0, new CorpseTally("LTU", "TU"), 700_000, 0)),
				tracker.looted(CorpseType.UMBER, -500_000));
		assertEquals(List.of(new ShaftVisit(1000, "JASP_1", 0, 0, new CorpseTally("LTU", "TU"), 700_000, 3_000_000)),
				tracker.mined(3_000_000, 50_000));
		assertTrue(tracker.mined(3_000_000, 51_000).isEmpty());
		assertEquals(List.of(new ShaftVisit(1000, "JASP_1", 59_000, 0, new CorpseTally("LTU", "TU"), 700_000, 3_000_000)),
				tracker.update(false, "", TUNNELS, 60_000));
	}

	@Test
	void whatComesInRightAfterLeavingStillGoesToThatShaft() {
		VisitTracker tracker = new VisitTracker();
		assertTrue(tracker.mined(5, 500).isEmpty());
		tracker.update(true, "JASP_1", SHAFT, 1000);
		tracker.mined(3_000_000, 50_000);
		tracker.update(false, "", TUNNELS, 60_000);
		assertEquals(List.of(new ShaftVisit(1000, "JASP_1", 59_000, 0, CorpseTally.NONE, 0, 3_400_000)),
				tracker.mined(3_400_000, 65_000));
		assertEquals(3_400_000, tracker.last().minedCoins());
		// never less than it had, and not once the moment has passed
		assertTrue(tracker.mined(3_000_000, 66_000).isEmpty());
		assertTrue(tracker.mined(9_000_000, 80_000).isEmpty());
	}

	@Test
	void aJoinedShaftStaysJoinedWhenItIsResumed() {
		VisitTracker tracker = new VisitTracker();
		tracker.update(true, "FAIR_1", SHAFT, 1000);
		assertEquals(List.of(ShaftVisit.entered(1000, "FAIR_1").withJoined(true)), tracker.joined());
		assertTrue(tracker.joined().isEmpty());
		tracker.update(false, "", TUNNELS, 5000);
		// back in after a disconnect, the same visit and still joined
		assertEquals(List.of(new ShaftVisit(1000, "FAIR_1", 0, 0).withJoined(true)), tracker.update(true, "FAIR_1", SHAFT, 8000));
	}
}
