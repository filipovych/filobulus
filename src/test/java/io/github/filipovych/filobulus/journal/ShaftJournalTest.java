package io.github.filipovych.filobulus.journal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ShaftJournalTest {
	@Test
	void aVisitIsOneLine() {
		ShaftVisit visit = new ShaftVisit(1000, "JASP_1", 183_000, 171_000);
		assertEquals("{\"at\":1000,\"type\":\"JASP_1\",\"spent\":183000,\"run\":171000}", JournalCodec.write(visit));
		assertEquals(visit, JournalCodec.read(JournalCodec.write(visit)).orElseThrow());
		assertEquals("{\"at\":5,\"type\":\"\"}", JournalCodec.write(ShaftVisit.entered(5, "")));
		assertEquals(ShaftVisit.entered(5, ""), JournalCodec.read("{\"at\":5}").orElseThrow());
	}

	@Test
	void aBadLineIsNoVisit() {
		assertTrue(JournalCodec.read("").isEmpty());
		assertTrue(JournalCodec.read("{\"at\":").isEmpty());
		assertTrue(JournalCodec.read("[1,2]").isEmpty());
		assertTrue(JournalCodec.read("{\"type\":\"JASP_1\"}").isEmpty());
		assertTrue(JournalCodec.read("{\"at\":0,\"type\":\"JASP_1\"}").isEmpty());
	}

	@Test
	void aLaterLineAboutTheSameShaftReplacesTheFirst() {
		ShaftJournal journal = new ShaftJournal();
		journal.put(ShaftVisit.entered(1000, ""));
		journal.put(ShaftVisit.entered(2000, "RUBY_1"));
		journal.put(new ShaftVisit(1000, "JASP_1", 60_000, 0));
		assertEquals(List.of(new ShaftVisit(1000, "JASP_1", 60_000, 0), ShaftVisit.entered(2000, "RUBY_1")),
				journal.visits());
	}

	@Test
	void visitsStayInOrder() {
		ShaftJournal journal = new ShaftJournal();
		journal.put(ShaftVisit.entered(3000, "RUBY_1"));
		journal.put(ShaftVisit.entered(1000, "JASP_1"));
		journal.put(ShaftVisit.entered(2000, "ONYX_2"));
		assertEquals(List.of(1000L, 2000L, 3000L), journal.visits().stream().map(ShaftVisit::enteredAt).toList());
		assertEquals(List.of(2000L, 3000L), journal.since(2000).stream().map(ShaftVisit::enteredAt).toList());
		assertTrue(journal.since(3001).isEmpty());
	}

	@Test
	void countsShaftsSinceTheLastJasper() {
		ShaftJournal journal = new ShaftJournal();
		assertEquals(0, journal.sinceJasper());
		journal.put(ShaftVisit.entered(1, "RUBY_1"));
		assertEquals(1, journal.sinceJasper());
		journal.put(ShaftVisit.entered(2, "JASP_1"));
		assertEquals(0, journal.sinceJasper());
		journal.put(ShaftVisit.entered(3, "RUBY_C"));
		journal.put(ShaftVisit.entered(4, ""));
		assertEquals(2, journal.sinceJasper());
		journal.put(ShaftVisit.entered(5, "JASP_C"));
		assertEquals(0, journal.sinceJasper());
	}

	@Test
	void bothJasperShaftsAreJasper() {
		assertTrue(ShaftVisit.entered(1, "JASP_1").isJasper());
		assertTrue(ShaftVisit.entered(1, "JASP_C").isJasper());
		assertFalse(ShaftVisit.entered(1, "OPAL_C").isJasper());
		assertFalse(ShaftVisit.entered(1, "").isJasper());
		assertEquals("CRYSTAL", ShaftVisit.entered(1, "RUBY_C").key());
		assertEquals("RUBY_2", ShaftVisit.entered(1, "RUBY_2").key());
		assertEquals("", ShaftVisit.entered(1, "NOPE_9").key());
	}
}
