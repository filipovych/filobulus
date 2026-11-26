package io.github.filipovych.filobulus.journal;

import io.github.filipovych.filobulus.corpse.CoinText;
import io.github.filipovych.filobulus.corpse.CorpseType;
import io.github.filipovych.filobulus.shaft.ShaftLayout;
import io.github.filipovych.filobulus.shaft.ShaftType;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// one calendar month of the journal as cards, shown one at a time like a year in review
// a card that would have nothing to say is left out, a month without shafts has none
public record MonthlyWrapped(YearMonth month, int shafts, List<Card> cards) {
	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH);
	private static final DateTimeFormatter WEEKDAY = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH);

	public enum Kind {
		SHAFTS, TIME, JASPER, COINS, BEST_SHAFT, BEST_DAY, MOST_CORPSES, DRY, FAVOURITE, CORPSE_TYPES, VERSUS
	}

	public enum Unit {
		COUNT, COINS, TIME, PERCENT, TEXT
	}

	// number is what counts up, text is the value of a TEXT card
	// code is a type code or a layout key when the card is about one kind of shaft, empty otherwise
	// parts is corpses opened per type for CORPSE_TYPES, lapis tungsten umber vanguard
	public record Card(Kind kind, String title, Unit unit, long number, String text, String line, String code,
			List<Integer> parts) {
		public Card {
			parts = List.copyOf(parts);
		}

		static Card of(Kind kind, String title, Unit unit, long number, String line) {
			return new Card(kind, title, unit, number, "", line, "", List.of());
		}

		Card withCode(String code) {
			return new Card(kind, title, unit, number, text, line, code, parts);
		}

		public String value() {
			return value(1);
		}

		// the value part way through counting up, progress goes from 0 to 1
		public String value(double progress) {
			long shown = Math.round(number * Math.max(0, Math.min(1, progress)));
			return switch (unit) {
				case COUNT -> String.valueOf(shown);
				case COINS -> coins(shown);
				case TIME -> JournalText.duration(shown);
				case PERCENT -> (number < 0 ? "-" : "+") + Math.abs(shown) + "%";
				case TEXT -> text;
			};
		}
	}

	public MonthlyWrapped {
		cards = List.copyOf(cards);
	}

	// visits oldest first as the journal keeps them, now is only used to tell if the month is still going
	public static MonthlyWrapped of(List<ShaftVisit> visits, YearMonth month, long now, ZoneId zone) {
		List<ShaftVisit> inMonth = within(visits, month, zone);
		List<Card> cards = new ArrayList<>();
		if (inMonth.isEmpty()) {
			return new MonthlyWrapped(month, 0, cards);
		}
		JournalStats stats = JournalStats.of(inMonth);
		JournalDays days = JournalDays.of(inMonth, zone, 31);

		int played = days.played();
		cards.add(Card.of(Kind.SHAFTS, "Shafts entered", Unit.COUNT, stats.shafts(),
				played == 1 ? "all on one day" : "across " + played + " different days"));
		if (stats.timeInside() > 0) {
			cards.add(Card.of(Kind.TIME, "Time inside", Unit.TIME, stats.timeInside(),
					"about " + JournalText.duration(stats.averageTime()) + " a shaft"));
		}
		if (stats.jasper() > 0) {
			cards.add(Card.of(Kind.JASPER, "Jasper shafts", Unit.COUNT, stats.jasper(),
					JournalText.percent(stats.jasperRate()) + " of your shafts").withCode("JASP_1"));
		}

		long corpseCoins = stats.corpses().coins();
		long mined = stats.corpses().mined();
		if (corpseCoins != 0 || mined != 0) {
			String line = corpseCoins == 0 ? "all of it mined"
					: mined == 0 ? "all of it from corpses"
					: coins(corpseCoins) + " from corpses, " + coins(mined) + " mined";
			cards.add(Card.of(Kind.COINS, "Coins made", Unit.COINS, corpseCoins + mined, line));
		}

		ShaftVisit best = null;
		ShaftVisit mostCorpses = null;
		for (ShaftVisit visit : inMonth) {
			if (visit.coins() > 0 && (best == null || visit.coins() > best.coins())) {
				best = visit;
			}
			int looted = visit.corpses().looted().length();
			if (looted > 0 && (mostCorpses == null || looted > mostCorpses.corpses().looted().length())) {
				mostCorpses = visit;
			}
		}
		if (best != null) {
			cards.add(Card.of(Kind.BEST_SHAFT, "Best shaft", Unit.COINS, best.coins(),
					name(best) + " on " + date(best.enteredAt(), DATE, zone)).withCode(best.code()));
		}
		JournalDays.Day bestDay = days.best();
		// with one day played the best day is just the month again
		if (played > 1 && bestDay != null && bestDay.coins() > 0) {
			cards.add(Card.of(Kind.BEST_DAY, "Best day", Unit.COINS, bestDay.coins(),
					date(bestDay.start(), WEEKDAY, zone) + ", " + plural(bestDay.shafts(), "shaft")));
		}
		if (mostCorpses != null) {
			cards.add(Card.of(Kind.MOST_CORPSES, "Most corpses in one shaft", Unit.COUNT,
					mostCorpses.corpses().looted().length(),
					"opened in " + article(name(mostCorpses)) + " on " + date(mostCorpses.enteredAt(), DATE, zone))
					.withCode(mostCorpses.code()));
		}

		addDry(cards, inMonth, YearMonth.from(Instant.ofEpochMilli(now).atZone(zone)).equals(month), zone);

		stats.groups().stream().filter(group -> !group.key().isEmpty()).findFirst().ifPresent(group -> {
			String title = ShaftLayout.byKey(group.key()).map(ShaftLayout::title).orElse(group.key());
			cards.add(new Card(Kind.FAVOURITE, "Favourite shaft", Unit.TEXT, group.shafts(), title,
					group.shafts() + " of your " + plural(stats.shafts(), "shaft"), group.key(), List.of()));
		});

		int[] byType = stats.corpses().lootedByType();
		if (stats.corpses().looted() > 0) {
			List<String> parts = new ArrayList<>();
			for (int type = 0; type < byType.length; type++) {
				if (byType[type] > 0) {
					parts.add(byType[type] + " " + CorpseType.values()[type].displayName());
				}
			}
			cards.add(new Card(Kind.CORPSE_TYPES, "Corpses opened", Unit.COUNT, stats.corpses().looted(), "",
					String.join(", ", parts), "", List.of(byType[0], byType[1], byType[2], byType[3])));
		}

		addVersus(cards, stats, JournalStats.of(within(visits, month.minusMonths(1), zone)), month.minusMonths(1));
		return new MonthlyWrapped(month, inMonth.size(), cards);
	}

	// only the shafts of this month count, a streak that started the month before starts again at the 1st
	private static void addDry(List<Card> cards, List<ShaftVisit> inMonth, boolean going, ZoneId zone) {
		int dry = 0;
		int longest = 0;
		ShaftVisit endedBy = null;
		for (ShaftVisit visit : inMonth) {
			if (!visit.isJasper()) {
				dry++;
				continue;
			}
			if (dry > longest) {
				longest = dry;
				endedBy = visit;
			}
			dry = 0;
		}
		if (dry > longest) {
			longest = dry;
			endedBy = null;
		}
		if (longest == 0) {
			return;
		}
		String line = "shafts without a jasper, ";
		line += endedBy != null
				? "ended by " + article(name(endedBy)) + " on " + date(endedBy.enteredAt(), DATE, zone)
				: going ? "and still going" : "and none by the end of the month";
		Card card = Card.of(Kind.DRY, "Longest dry streak", Unit.COUNT, longest, line);
		cards.add(endedBy != null ? card.withCode(endedBy.code()) : card);
	}

	// coins when both months made some, otherwise the shafts entered
	private static void addVersus(List<Card> cards, JournalStats now, JournalStats before, YearMonth previous) {
		if (before.shafts() == 0) {
			return;
		}
		long nowCoins = now.corpses().coins() + now.corpses().mined();
		long beforeCoins = before.corpses().coins() + before.corpses().mined();
		boolean byCoins = beforeCoins > 0 && nowCoins != 0;
		long current = byCoins ? nowCoins : now.shafts();
		long old = byCoins ? beforeCoins : before.shafts();
		long change = Math.round((current - old) * 100.0 / old);
		String monthName = monthName(previous);
		String what = byCoins ? coins(old) : String.valueOf(old);
		String line = change == 0 ? "the same as " + monthName + "'s " + what
				: (change < 0 ? "fewer " : "more ") + (byCoins ? "coins" : "shafts") + " than " + monthName + "'s " + what;
		cards.add(Card.of(Kind.VERSUS, "Compared with " + monthName, Unit.PERCENT, change, line));
	}

	// the month the wrapped opens on: the newest full month with shafts, this one so far when there is none
	public static YearMonth defaultMonth(List<ShaftVisit> visits, long now, ZoneId zone) {
		YearMonth current = YearMonth.from(Instant.ofEpochMilli(now).atZone(zone));
		for (int i = visits.size() - 1; i >= 0; i--) {
			YearMonth month = month(visits.get(i), zone);
			if (month.isBefore(current)) {
				return month;
			}
		}
		return current;
	}

	public static boolean hasShafts(List<ShaftVisit> visits, YearMonth month, ZoneId zone) {
		return !within(visits, month, zone).isEmpty();
	}

	// every month with a shaft, newest first
	public static List<YearMonth> months(List<ShaftVisit> visits, ZoneId zone) {
		List<YearMonth> months = new ArrayList<>();
		for (int i = visits.size() - 1; i >= 0; i--) {
			YearMonth month = month(visits.get(i), zone);
			if (months.isEmpty() || !months.get(months.size() - 1).equals(month)) {
				months.add(month);
			}
		}
		return months;
	}

	public static String monthName(YearMonth month) {
		return month.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
	}

	// the visits of one month, they are sorted so this is one slice
	static List<ShaftVisit> within(List<ShaftVisit> visits, YearMonth month, ZoneId zone) {
		long from = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli();
		long to = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli();
		int start = 0;
		while (start < visits.size() && visits.get(start).enteredAt() < from) {
			start++;
		}
		int end = start;
		while (end < visits.size() && visits.get(end).enteredAt() < to) {
			end++;
		}
		return visits.subList(start, end);
	}

	private static YearMonth month(ShaftVisit visit, ZoneId zone) {
		return YearMonth.from(Instant.ofEpochMilli(visit.enteredAt()).atZone(zone));
	}

	private static String name(ShaftVisit visit) {
		return visit.type().map(ShaftType::displayName).orElse("Mineshaft");
	}

	private static String article(String name) {
		return ("AEIOU".indexOf(name.charAt(0)) >= 0 ? "an " : "a ") + name;
	}

	private static String date(long at, DateTimeFormatter format, ZoneId zone) {
		return format.format(Instant.ofEpochMilli(at).atZone(zone));
	}

	private static String plural(int count, String word) {
		return count + " " + word + (count == 1 ? "" : "s");
	}

	private static String coins(long coins) {
		return (coins < 0 ? "-" : "") + CoinText.amount(coins);
	}
}
