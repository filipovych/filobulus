package io.github.filipovych.filobulus.alert;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.filipovych.filobulus.alert.WispTimer.Event;
import org.junit.jupiter.api.Test;

class WispTimerTest {
	private static final long LEAD = 10_000;

	@Test
	void nothingToSayWithoutAWisp() {
		WispTimer timer = new WispTimer();
		assertFalse(timer.known());
		assertEquals(0, timer.left(1000));
		assertEquals(Event.NONE, timer.poll(1000, LEAD));
	}

	@Test
	void aWispLastsFiveMinutes() {
		WispTimer timer = new WispTimer();
		timer.placed(1000);
		assertTrue(timer.known());
		assertEquals(300_000, timer.left(1000));
		assertEquals(60_000, timer.left(241_000));
		assertEquals(0, timer.left(400_000));
	}

	@Test
	void warnsOnceBeforeTheEndAndOnceAtTheEnd() {
		WispTimer timer = new WispTimer();
		timer.placed(0);
		assertEquals(Event.NONE, timer.poll(289_000, LEAD));
		assertEquals(Event.SOON, timer.poll(290_000, LEAD));
		assertEquals(Event.NONE, timer.poll(295_000, LEAD));
		assertEquals(Event.GONE, timer.poll(300_000, LEAD));
		assertEquals(Event.NONE, timer.poll(301_000, LEAD));
	}

	@Test
	void aLateLookOnlySaysItIsGone() {
		WispTimer timer = new WispTimer();
		timer.placed(0);
		assertEquals(Event.GONE, timer.poll(400_000, LEAD));
		assertEquals(Event.NONE, timer.poll(401_000, LEAD));
	}

	@Test
	void aNewWispStartsOver() {
		WispTimer timer = new WispTimer();
		timer.placed(0);
		timer.poll(295_000, LEAD);
		timer.placed(296_000);
		assertEquals(300_000, timer.left(296_000));
		assertEquals(Event.NONE, timer.poll(300_000, LEAD));
		assertEquals(Event.SOON, timer.poll(590_000, LEAD));
	}

	@Test
	void changingWorldsForgetsTheWisp() {
		WispTimer timer = new WispTimer();
		timer.placed(0);
		timer.clear();
		assertFalse(timer.known());
		assertEquals(Event.NONE, timer.poll(300_000, LEAD));
	}
}
