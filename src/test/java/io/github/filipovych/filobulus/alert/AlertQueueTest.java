package io.github.filipovych.filobulus.alert;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.filipovych.filobulus.alert.AlertQueue.Alert;
import java.util.List;
import org.junit.jupiter.api.Test;

class AlertQueueTest {
	private static Alert alert(String id, int priority) {
		return new Alert(id, id, "", 0xFFFFFF, priority, 2000);
	}

	private static List<String> ids(AlertQueue queue, long now) {
		return queue.shown(now).stream().map(shown -> shown.alert().id()).toList();
	}

	@Test
	void anAlertFadesInStaysAndFadesOut() {
		AlertQueue queue = new AlertQueue();
		queue.show(alert("best", 0), 1000);
		assertEquals(0f, queue.shown(1000).get(0).alpha());
		assertEquals(0.5f, queue.shown(1075).get(0).alpha());
		assertEquals(1f, queue.shown(1150).get(0).alpha());
		assertEquals(1f, queue.shown(2600).get(0).alpha());
		assertEquals(0.5f, queue.shown(2800).get(0).alpha());
		assertTrue(queue.shown(3000).isEmpty());
	}

	@Test
	void theSameIdStartsOverInsteadOfStacking() {
		AlertQueue queue = new AlertQueue();
		queue.show(alert("wisp", 0), 1000);
		queue.show(alert("wisp", 0), 2500);
		assertEquals(List.of("wisp"), ids(queue, 3500));
		assertTrue(queue.shown(4500).isEmpty());
	}

	@Test
	void theMostImportantIsOnTopThenTheNewest() {
		AlertQueue queue = new AlertQueue();
		queue.show(alert("shaft", 0), 1000);
		queue.show(alert("cold", 5), 1100);
		queue.show(alert("best", 0), 1200);
		assertEquals(List.of("cold", "best", "shaft"), ids(queue, 1300));
	}

	@Test
	void onlyAFewAreShownAtOnce() {
		AlertQueue queue = new AlertQueue();
		for (int i = 0; i < 5; i++) {
			queue.show(alert("a" + i, i), 1000);
		}
		assertEquals(List.of("a4", "a3", "a2"), ids(queue, 1100));
		queue.clear();
		assertTrue(queue.shown(1100).isEmpty());
	}
}
