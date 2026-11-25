package io.github.filipovych.filobulus.journal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.filipovych.filobulus.corpse.CorpseTally;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class JournalDaysTest {
	private static final ZoneId LISBON = ZoneId.of("Europe/Lisbon");

	private static ShaftVisit paid(long at, String code, long coins) {
		return new ShaftVisit(at, code, 60_000, 0, CorpseTally.NONE, 0, coins);
	}

	private static long at(int day, int hour) {
		return ZonedDateTime.of(2026, 11, day, hour, 0, 0, 0, LISBON).toInstant().toEpochMilli();
	}

	@Test
	void daysRunFromTheFirstShaftToTheLastWithTheEmptyOnesBetween() {
		JournalDays days = JournalDays.of(List.of(
				new ShaftVisit(at(20, 23), "RUBY_1", 60_000, 0, CorpseTally.NONE, 1_000_000, 2_000_000),
				new ShaftVisit(at(21, 1), "JASP_1", 120_000, 0, CorpseTally.NONE, -500_000, 9_000_000),
				new ShaftVisit(at(21, 2), "RUBY_1", 30_000, 0),
				new ShaftVisit(at(23, 12), "ONYX_1", 10_000, 0, CorpseTally.NONE, 0, 400_000)), LISBON, 60);
		assertEquals(4, days.days().size());
		assertEquals(new JournalDays.Day(at(20, 0), 1, 0, 1_000_000, 2_000_000, 60_000), days.days().get(0));
		assertEquals(new JournalDays.Day(at(21, 0), 2, 1, -500_000, 9_000_000, 150_000), days.days().get(1));
		assertEquals(0, days.days().get(2).shafts());
		assertEquals(3, days.played());
		assertEquals(4, days.shafts());
		assertEquals(11_900_000, days.coins());
		assertEquals(at(21, 0), days.best().start());
	}

	@Test
	void onlyTheLastDaysAreKeptWhenThereAreTooMany() {
		JournalDays days = JournalDays.of(List.of(paid(at(1, 12), "RUBY_1", 5), paid(at(10, 12), "RUBY_1", 7), paid(at(12, 12), "RUBY_1", 9)),
				LISBON, 3);
		assertEquals(3, days.days().size());
		assertEquals(at(10, 0), days.days().get(0).start());
		assertEquals(16, days.coins());
		assertTrue(JournalDays.of(List.of(), LISBON, 3).days().isEmpty());
		assertEquals(null, JournalDays.of(List.of(), LISBON, 3).best());
	}
}
