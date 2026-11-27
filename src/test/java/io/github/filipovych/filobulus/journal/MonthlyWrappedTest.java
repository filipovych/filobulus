package io.github.filipovych.filobulus.journal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.filipovych.filobulus.corpse.CorpseTally;
import io.github.filipovych.filobulus.journal.MonthlyWrapped.Card;
import io.github.filipovych.filobulus.journal.MonthlyWrapped.Kind;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MonthlyWrappedTest {
	private static final ZoneId LISBON = ZoneId.of("Europe/Lisbon");
	private static final YearMonth NOVEMBER = YearMonth.of(2026, 11);

	private static long at(int month, int day, int hour) {
		return ZonedDateTime.of(2026, month, day, hour, 0, 0, 0, LISBON).toInstant().toEpochMilli();
	}

	private static ShaftVisit visit(long at, String code, long corpseCoins, long mined, String looted) {
		return new ShaftVisit(at, code, 120_000, 0, new CorpseTally(looted, looted), corpseCoins, mined);
	}

	private static Optional<Card> card(MonthlyWrapped wrapped, Kind kind) {
		return wrapped.cards().stream().filter(card -> card.kind() == kind).findFirst();
	}

	private static final List<ShaftVisit> VISITS = List.of(
			visit(at(10, 5, 12), "RUBY_1", 0, 1_000_000, ""),
			visit(at(10, 6, 12), "RUBY_1", 0, 1_000_000, ""),
			// last minute of october in lisbon does not count for november
			visit(at(10, 31, 23), "ONYX_1", 0, 500_000, ""),
			visit(at(11, 2, 10), "RUBY_1", 1_000_000, 500_000, "LLT"),
			visit(at(11, 2, 11), "RUBY_1", 0, 200_000, "L"),
			visit(at(11, 2, 12), "ONYX_1", 0, 300_000, ""),
			visit(at(11, 3, 9), "JASP_C", 5_000_000, 3_000_000, "LTUV"),
			visit(at(11, 14, 20), "RUBY_1", -200_000, 400_000, "T"),
			visit(at(11, 15, 20), "AMET_1", 0, 0, ""));

	@Test
	void aMonthTellsWhatItAddsUpTo() {
		MonthlyWrapped wrapped = MonthlyWrapped.of(VISITS, NOVEMBER, at(12, 2, 12), LISBON);
		assertEquals(6, wrapped.shafts());
		assertEquals("6", card(wrapped, Kind.SHAFTS).orElseThrow().value());
		assertEquals("across 4 different days", card(wrapped, Kind.SHAFTS).orElseThrow().line());
		assertEquals("12:00", card(wrapped, Kind.TIME).orElseThrow().value());
		assertEquals("about 2:00 a shaft", card(wrapped, Kind.TIME).orElseThrow().line());
		assertEquals("1", card(wrapped, Kind.JASPER).orElseThrow().value());
		assertEquals("17% of your shafts", card(wrapped, Kind.JASPER).orElseThrow().line());

		Card coins = card(wrapped, Kind.COINS).orElseThrow();
		assertEquals(10_200_000, coins.number());
		assertEquals("10.2M", coins.value());
		assertEquals("5.8M from corpses, 4.4M mined", coins.line());

		Card best = card(wrapped, Kind.BEST_SHAFT).orElseThrow();
		assertEquals("8.0M", best.value());
		assertEquals("Jasper Crystal on 3 Nov", best.line());
		assertEquals("JASP_C", best.code());

		Card day = card(wrapped, Kind.BEST_DAY).orElseThrow();
		assertEquals(8_000_000, day.number());
		assertEquals("Tue 3 Nov, 1 shaft", day.line());

		Card favourite = card(wrapped, Kind.FAVOURITE).orElseThrow();
		assertEquals("Ruby 1", favourite.value());
		assertEquals("3 of your 6 shafts", favourite.line());

		Card types = card(wrapped, Kind.CORPSE_TYPES).orElseThrow();
		assertEquals(9, types.number());
		assertEquals(List.of(4, 3, 1, 1), types.parts());
		assertEquals("4 Lapis, 3 Tungsten, 1 Umber, 1 Vanguard", types.line());

		// october made 2.5M, november 10.2M
		Card versus = card(wrapped, Kind.VERSUS).orElseThrow();
		assertEquals("Compared with October", versus.title());
		assertEquals("+308%", versus.value());
		assertEquals("more coins than October's 2.5M", versus.line());
	}

	@Test
	void theDryStreakSaysHowItEnded() {
		MonthlyWrapped wrapped = MonthlyWrapped.of(VISITS, NOVEMBER, at(12, 2, 12), LISBON);
		Card dry = card(wrapped, Kind.DRY).orElseThrow();
		// three before the jasper, two after it that never ended
		assertEquals(3, dry.number());
		assertEquals("shafts without a jasper, ended by a Jasper Crystal on 3 Nov", dry.line());

		List<ShaftVisit> noJasper = List.of(visit(at(11, 2, 10), "RUBY_1", 0, 0, ""), visit(at(11, 3, 10), "ONYX_1", 0, 0, ""));
		assertEquals("shafts without a jasper, and none by the end of the month",
				card(MonthlyWrapped.of(noJasper, NOVEMBER, at(12, 1, 12), LISBON), Kind.DRY).orElseThrow().line());
		assertEquals("shafts without a jasper, and still going",
				card(MonthlyWrapped.of(noJasper, NOVEMBER, at(11, 20, 12), LISBON), Kind.DRY).orElseThrow().line());
	}

	@Test
	void cardsWithNothingToSayAreLeftOut() {
		List<ShaftVisit> quiet = List.of(new ShaftVisit(at(11, 4, 10), "RUBY_1", 0, 0), new ShaftVisit(at(11, 4, 11), "", 0, 0));
		MonthlyWrapped wrapped = MonthlyWrapped.of(quiet, NOVEMBER, at(12, 1, 0), LISBON);
		assertEquals(List.of(Kind.SHAFTS, Kind.DRY, Kind.FAVOURITE), wrapped.cards().stream().map(Card::kind).toList());
		assertEquals("all on one day", wrapped.cards().get(0).line());
		assertTrue(MonthlyWrapped.of(quiet, YearMonth.of(2026, 9), at(12, 1, 0), LISBON).cards().isEmpty());
	}

	@Test
	void withoutCoinsTheMonthsAreComparedByShafts() {
		List<ShaftVisit> visits = List.of(new ShaftVisit(at(10, 4, 10), "RUBY_1", 0, 0), new ShaftVisit(at(10, 5, 10), "RUBY_1", 0, 0),
				new ShaftVisit(at(11, 4, 10), "RUBY_1", 0, 0));
		Card versus = card(MonthlyWrapped.of(visits, NOVEMBER, at(12, 1, 0), LISBON), Kind.VERSUS).orElseThrow();
		assertEquals("-50%", versus.value());
		assertEquals("fewer shafts than October's 2", versus.line());
		assertFalse(card(MonthlyWrapped.of(visits, YearMonth.of(2026, 10), at(12, 1, 0), LISBON), Kind.VERSUS).isPresent());
	}

	@Test
	void numbersCountUp() {
		Card card = new Card(Kind.COINS, "Coins made", MonthlyWrapped.Unit.COINS, 2_000_000, "", "", "", List.of());
		assertEquals("0", card.value(0));
		assertEquals("1.0M", card.value(0.5));
		assertEquals("2.0M", card.value(2));
		Card percent = new Card(Kind.VERSUS, "", MonthlyWrapped.Unit.PERCENT, -40, "", "", "", List.of());
		assertEquals("-20%", percent.value(0.5));
	}

	@Test
	void theDefaultMonthIsTheLastFullOneWithShafts() {
		assertEquals(NOVEMBER, MonthlyWrapped.defaultMonth(VISITS, at(12, 2, 12), LISBON));
		// in november itself october is the last full month
		assertEquals(YearMonth.of(2026, 10), MonthlyWrapped.defaultMonth(VISITS, at(11, 20, 12), LISBON));
		// nothing before this month, so this month so far
		assertEquals(YearMonth.of(2026, 10), MonthlyWrapped.defaultMonth(VISITS.subList(0, 2), at(10, 20, 12), LISBON));
		// december had no shafts, so it skips back to november
		long january = ZonedDateTime.of(2027, 1, 5, 0, 0, 0, 0, LISBON).toInstant().toEpochMilli();
		assertEquals(NOVEMBER, MonthlyWrapped.defaultMonth(VISITS, january, LISBON));
		assertEquals(List.of(NOVEMBER, YearMonth.of(2026, 10)), MonthlyWrapped.months(VISITS, LISBON));
		assertTrue(MonthlyWrapped.hasShafts(VISITS, NOVEMBER, LISBON));
		assertFalse(MonthlyWrapped.hasShafts(VISITS, YearMonth.of(2026, 9), LISBON));
	}
}
