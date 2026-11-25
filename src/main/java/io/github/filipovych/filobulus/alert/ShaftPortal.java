package io.github.filipovych.filobulus.alert;

import java.util.regex.Pattern;

// the portal of a shaft we spawned stays for 30 seconds, this counts them down
// the chat line is the one skyhanni matches on, colour codes stripped
public final class ShaftPortal {
	private static final Pattern SPAWNED = Pattern.compile("\\s*WOW! You found a Glacite Mineshaft portal!\\s*");
	public static final long OPEN_MILLIS = 30_000;

	public enum Event {
		NONE,
		CLOSING,
		CLOSED
	}

	private long spawnedAt;
	private boolean warned;

	public static boolean spawned(String line) {
		return line != null && SPAWNED.matcher(line).matches();
	}

	public void spawn(long now) {
		spawnedAt = now;
		warned = false;
	}

	// we went in, or the world changed
	public void clear() {
		spawnedAt = 0;
	}

	public boolean open() {
		return spawnedAt > 0;
	}

	public long left(long now) {
		return spawnedAt == 0 ? 0 : Math.max(0, spawnedAt + OPEN_MILLIS - now);
	}

	// lead is how long before the end the warning comes, each event is given once
	public Event poll(long now, long lead) {
		if (spawnedAt == 0) {
			return Event.NONE;
		}
		long left = left(now);
		if (left == 0) {
			spawnedAt = 0;
			return Event.CLOSED;
		}
		if (!warned && left <= lead) {
			warned = true;
			return Event.CLOSING;
		}
		return Event.NONE;
	}
}
