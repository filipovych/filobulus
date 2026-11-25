package io.github.filipovych.filobulus.alert;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ShaftPortalTest {
	@Test
	void theSpawnLineIsKnown() {
		assertTrue(ShaftPortal.spawned("WOW! You found a Glacite Mineshaft portal!"));
		assertFalse(ShaftPortal.spawned("Someone: WOW! You found a Glacite Mineshaft portal!"));
		assertFalse(ShaftPortal.spawned(null));
	}

	@Test
	void theWarningComesOnceBeforeThePortalCloses() {
		ShaftPortal portal = new ShaftPortal();
		assertEquals(ShaftPortal.Event.NONE, portal.poll(0, 5000));
		portal.spawn(1000);
		assertEquals(ShaftPortal.Event.NONE, portal.poll(20_000, 5000));
		assertEquals(ShaftPortal.Event.CLOSING, portal.poll(26_000, 5000));
		assertEquals(5000, portal.left(26_000));
		assertEquals(ShaftPortal.Event.NONE, portal.poll(27_000, 5000));
		assertEquals(ShaftPortal.Event.CLOSED, portal.poll(31_000, 5000));
		assertFalse(portal.open());
	}

	@Test
	void goingInEndsIt() {
		ShaftPortal portal = new ShaftPortal();
		portal.spawn(1000);
		portal.clear();
		assertEquals(ShaftPortal.Event.NONE, portal.poll(27_000, 5000));
	}
}
