package io.github.filipovych.filobulus.journal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.filipovych.filobulus.corpse.CorpseTally;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class JournalStatsTest {
	private static final ZoneId LISBON = ZoneId.of("Europe/Lisbon");

	private static long at(int day, int hour, int minute) {
		return ZonedDateTime.of(2026, 11, day, hour, minute, 0, 0, LISBON).toInstant().toEpochMilli();
	}

	@Test
	void addsUpAListOfVisits() {
		JournalStats stats = JournalStats.of(List.of(
				new ShaftVisit(1, "RUBY_1", 60_000, 0),
				new ShaftVisit(2, "RUBY_C", 30_000, 0),
				new ShaftVisit(3, "JASP_1", 200_000, 190_000),
				new ShaftVisit(4, "ONYX_C", 0, 0),
				new ShaftVisit(5, "TUNG_1", 10_000, 0),
				new ShaftVisit(6, "", 20_000, 0),
				new ShaftVisit(7, "JASP_1", 180_000, 170_000)));
		assertEquals(7, stats.shafts());
		assertEquals(2, stats.jasper());
		assertEquals(3, stats.longestDry());
		assertEquals(500_000, stats.timeInside());
		assertEquals(6, stats.timed());
		assertEquals(83_333, stats.averageTime());
		assertEquals(2.0 / 7, stats.jasperRate());
	}

	@Test
	void groupsByLayoutMostEnteredFirst() {
		List<JournalStats.Group> groups = JournalStats.of(List.of(
				new ShaftVisit(1, "RUBY_1", 60_000, 0),
				new ShaftVisit(2, "RUBY_C", 30_000, 0),
				new ShaftVisit(3, "JASP_1", 200_000, 190_000),
				new ShaftVisit(4, "ONYX_C", 0, 0),
				new ShaftVisit(5, "JASP_1", 180_000, 170_000))).groups();
		assertEquals(List.of("CRYSTAL", "JASP_1", "RUBY_1"), groups.stream().map(JournalStats.Group::key).toList());
		assertEquals(new JournalStats.Group("CRYSTAL", 2, 30_000, 1, 0), groups.get(0));
		assertEquals(new JournalStats.Group("JASP_1", 2, 380_000, 2, 170_000), groups.get(1));
		assertEquals(190_000, groups.get(1).averageTime());
	}

	@Test
	void nothingAddsUpToNothing() {
		JournalStats stats = JournalStats.of(List.of());
		assertEquals(0, stats.shafts());
		assertEquals(0, stats.averageTime());
		assertEquals(0, stats.jasperRate());
	}

	@Test
	void aPeriodStartsWhereItSays() {
		long now = at(24, 15, 30);
		assertEquals(at(24, 9, 0), JournalPeriod.SESSION.start(now, at(24, 9, 0), LISBON));
		assertEquals(at(24, 0, 0), JournalPeriod.TODAY.start(now, 0, LISBON));
		assertEquals(at(17, 15, 30), JournalPeriod.WEEK.start(now, 0, LISBON));
		assertEquals(0, JournalPeriod.ALL.start(now, 0, LISBON));
	}

	@Test
	void writesTimesAndDays() {
		assertEquals("0:05", JournalText.duration(5_900));
		assertEquals("59:59", JournalText.duration(3_599_000));
		assertEquals("14h 02m", JournalText.duration((14 * 60 + 2) * 60_000L));
		long now = at(24, 15, 30);
		assertEquals("Today", JournalText.day(at(24, 0, 1), now, LISBON));
		assertEquals("Yesterday", JournalText.day(at(23, 23, 59), now, LISBON));
		assertEquals("21 Nov", JournalText.day(at(21, 12, 0), now, LISBON));
		assertEquals("21 Nov 2026", JournalText.day(at(21, 12, 0), now + 60L * 24 * 3_600_000, LISBON));
		assertEquals("09:05", JournalText.clock(at(21, 9, 5), LISBON));
		assertEquals("7.5%", JournalText.percent(0.075));
		assertEquals("42%", JournalText.percent(0.424));
		assertEquals("0%", JournalText.percent(0));
	}

	@Test
	void exportsACsv() {
		assertEquals("entered,type,seconds_inside,route_seconds,corpses,corpses_looted,corpse_coins,mined_coins,joined\n"
				+ "2026-11-24 15:30:00,JASP_1,183,171,LTU,LT,-250000,9000000,yes\n"
				+ "2026-11-24 15:40:00,,,,,,,,\n",
				JournalText.csv(List.of(
						new ShaftVisit(at(24, 15, 30), "JASP_1", 183_000, 171_000, new CorpseTally("LTU", "LT"), -250_000, 9_000_000, true),
						ShaftVisit.entered(at(24, 15, 40), "")), LISBON));
	}

	@Test
	void corpsesAddUpPerTypeAndOnlyForShaftsThatWereRead() {
		JournalStats stats = JournalStats.of(List.of(
				new ShaftVisit(1, "RUBY_1", 60_000, 0, new CorpseTally("LLT", "LT"), 1_500_000, 0),
				new ShaftVisit(2, "RUBY_1", 60_000, 0, new CorpseTally("TUV", ""), 0, 0),
				new ShaftVisit(3, "JASP_1", 60_000, 0, new CorpseTally("V", "V"), -2_000_000, 12_000_000),
				new ShaftVisit(4, "ONYX_1", 60_000, 0)));
		JournalStats.Corpses corpses = stats.corpses();
		assertEquals(7, corpses.seen());
		assertEquals(3, corpses.looted());
		assertEquals(2, corpses.keys());
		assertEquals(3, corpses.counted());
		assertEquals(2, corpses.paid());
		assertEquals(-500_000, corpses.coins());
		assertEquals(-250_000, corpses.coinsPerShaft());
		assertEquals(12_000_000, corpses.mined());
		assertEquals(10_000_000, corpses.best());
		assertEquals(2, corpses.seenByType()[0]);
		assertEquals(1, corpses.lootedByType()[1]);
		JournalStats.Group ruby = stats.groups().get(0);
		assertEquals("RUBY_1", ruby.key());
		assertEquals(6, ruby.corpses());
		assertEquals(2, ruby.looted());
		assertEquals(1_500_000, ruby.coins());
	}

	@Test
	void shaftsPerHourLeavesTheBreaksOut() {
		long minute = 60_000;
		// three shafts ten minutes apart, a three hour break, then two more
		JournalStats stats = JournalStats.of(List.of(
				new ShaftVisit(0, "RUBY_1", 2 * minute, 0),
				new ShaftVisit(10 * minute, "RUBY_1", 2 * minute, 0),
				new ShaftVisit(20 * minute, "RUBY_1", 4 * minute, 0),
				new ShaftVisit(200 * minute, "RUBY_1", 2 * minute, 0),
				new ShaftVisit(210 * minute, "RUBY_1", 6 * minute, 0)));
		// 10 + 10 + 4 + 10 + 6 minutes of shafting, but the rate only goes by the three ten minute gaps
		assertEquals(40 * minute, stats.activeMillis());
		assertEquals(6.0, stats.perHour(), 0.001);
		assertEquals(0, JournalStats.of(List.of(ShaftVisit.entered(5, "RUBY_1"))).perHour());
	}
}
