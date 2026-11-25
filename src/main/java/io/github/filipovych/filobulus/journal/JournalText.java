package io.github.filipovych.filobulus.journal;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

// how the journal writes times and dates
public final class JournalText {
	private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT);
	private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH);
	private static final DateTimeFormatter DAY_AND_YEAR = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);
	private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT);

	private JournalText() {
	}

	// 3:11, and 14h 02m once it is an hour or more
	public static String duration(long millis) {
		long seconds = Math.max(0, millis) / 1000;
		if (seconds < 3600) {
			return seconds / 60 + ":" + String.format(Locale.ROOT, "%02d", seconds % 60);
		}
		return seconds / 3600 + "h " + String.format(Locale.ROOT, "%02dm", seconds % 3600 / 60);
	}

	// 14:02
	public static String clock(long at, ZoneId zone) {
		return CLOCK.format(Instant.ofEpochMilli(at).atZone(zone));
	}

	// Today, Yesterday, 21 Nov, and with the year when it is not this one
	public static String day(long at, long now, ZoneId zone) {
		ZonedDateTime when = Instant.ofEpochMilli(at).atZone(zone);
		LocalDate today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate();
		if (when.toLocalDate().equals(today)) {
			return "Today";
		}
		if (when.toLocalDate().equals(today.minusDays(1))) {
			return "Yesterday";
		}
		return (when.getYear() == today.getYear() ? DAY : DAY_AND_YEAR).format(when);
	}

	// 7.5%, whole numbers from 10% up
	public static String percent(double share) {
		double percent = share * 100;
		return percent >= 10 || percent == 0
				? Math.round(percent) + "%"
				: String.format(Locale.ROOT, "%.1f%%", percent);
	}

	// for a spreadsheet, oldest first as they come
	public static String csv(List<ShaftVisit> visits, ZoneId zone) {
		StringBuilder text = new StringBuilder(
				"entered,type,seconds_inside,route_seconds,corpses,corpses_looted,corpse_coins,mined_coins\n");
		for (ShaftVisit visit : visits) {
			text.append(STAMP.format(Instant.ofEpochMilli(visit.enteredAt()).atZone(zone))).append(',')
					.append(visit.code()).append(',')
					.append(visit.spentMillis() > 0 ? String.valueOf(visit.spentMillis() / 1000) : "").append(',')
					.append(visit.runMillis() > 0 ? String.valueOf(visit.runMillis() / 1000) : "").append(',')
					.append(visit.corpses().seen()).append(',')
					.append(visit.corpses().looted()).append(',')
					.append(visit.corpseCoins() != 0 ? String.valueOf(visit.corpseCoins()) : "").append(',')
					.append(visit.minedCoins() != 0 ? String.valueOf(visit.minedCoins()) : "").append('\n');
		}
		return text.toString();
	}
}
