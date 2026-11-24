package io.github.filipovych.filobulus.journal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
}
