package io.github.filipovych.filobulus.journal;

import java.time.Instant;
import java.time.ZoneId;

// the stretch of time the journal screen looks at
public enum JournalPeriod {
	SESSION("Session"),
	TODAY("Today"),
	WEEK("7 days"),
	MONTH("30 days"),
	ALL("All time");

	private static final long DAY = 24 * 60 * 60 * 1000L;

	private final String title;

	JournalPeriod(String title) {
		this.title = title;
	}

	public String title() {
		return title;
	}

	// the first moment that counts, a session starts when the game does
	public long start(long now, long sessionStart, ZoneId zone) {
		return switch (this) {
			case SESSION -> sessionStart;
			case TODAY -> Instant.ofEpochMilli(now).atZone(zone).toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli();
			case WEEK -> now - 7 * DAY;
			case MONTH -> now - 30 * DAY;
			case ALL -> 0;
		};
	}
}
