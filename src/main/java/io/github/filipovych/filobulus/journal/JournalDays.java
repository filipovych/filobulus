package io.github.filipovych.filobulus.journal;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

// the journal by calendar day: every day from the first shaft to the last one, days without a shaft included
public record JournalDays(List<Day> days) {
	// start is the first millisecond of the day
	public record Day(long start, int shafts, int jasper, long corpseCoins, long minedCoins, long timeInside) {
		public long coins() {
			return corpseCoins + minedCoins;
		}
	}

	public JournalDays {
		days = List.copyOf(days);
	}

	// visits oldest first, only the last maxDays days are kept
	public static JournalDays of(List<ShaftVisit> visits, ZoneId zone, int maxDays) {
		List<Day> days = new ArrayList<>();
		if (visits.isEmpty() || maxDays <= 0) {
			return new JournalDays(days);
		}
		LocalDate first = date(visits.get(0), zone);
		LocalDate last = date(visits.get(visits.size() - 1), zone);
		if (first.plusDays(maxDays - 1L).isBefore(last)) {
			first = last.minusDays(maxDays - 1L);
		}
		int next = 0;
		for (LocalDate day = first; !day.isAfter(last); day = day.plusDays(1)) {
			int shafts = 0;
			int jasper = 0;
			long corpseCoins = 0;
			long minedCoins = 0;
			long timeInside = 0;
			for (; next < visits.size() && !date(visits.get(next), zone).isAfter(day); next++) {
				ShaftVisit visit = visits.get(next);
				if (date(visit, zone).isBefore(day)) {
					continue;
				}
				shafts++;
				jasper += visit.isJasper() ? 1 : 0;
				corpseCoins += visit.corpseCoins();
				minedCoins += visit.minedCoins();
				timeInside += visit.spentMillis();
			}
			days.add(new Day(day.atStartOfDay(zone).toInstant().toEpochMilli(), shafts, jasper, corpseCoins, minedCoins, timeInside));
		}
		return new JournalDays(days);
	}

	// days with at least one shaft
	public int played() {
		return (int) days.stream().filter(day -> day.shafts() > 0).count();
	}

	public long coins() {
		return days.stream().mapToLong(Day::coins).sum();
	}

	public int shafts() {
		return days.stream().mapToInt(Day::shafts).sum();
	}

	// the day that paid the most, null when there is none
	public Day best() {
		Day best = null;
		for (Day day : days) {
			if (day.shafts() > 0 && (best == null || day.coins() > best.coins())) {
				best = day;
			}
		}
		return best;
	}

	private static LocalDate date(ShaftVisit visit, ZoneId zone) {
		return Instant.ofEpochMilli(visit.enteredAt()).atZone(zone).toLocalDate();
	}
}
