package io.github.filipovych.filobulus.alert;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

// the big alerts that are on screen right now, each fades in, stays for its time and fades out
public final class AlertQueue {
	// color is rgb, millis is how long it is shown, fades included
	public record Alert(String id, String title, String subtitle, int color, int priority, long millis) {
	}

	// alpha goes from 0 to 1
	public record Shown(Alert alert, float alpha) {
	}

	private record Entry(Alert alert, long since) {
	}

	public static final int MAX_SHOWN = 3;
	private static final long FADE_IN = 150;
	private static final long FADE_OUT = 400;

	private final List<Entry> entries = new ArrayList<>();

	// an alert with the same id is replaced and starts over, so a repeated warning does not stack up
	public void show(Alert alert, long now) {
		entries.removeIf(entry -> entry.alert().id().equals(alert.id()));
		entries.add(new Entry(alert, now));
	}

	public void clear() {
		entries.clear();
	}

	// top to bottom: the most important first, the newest first among equals
	public List<Shown> shown(long now) {
		entries.removeIf(entry -> now - entry.since() >= entry.alert().millis());
		return entries.stream()
				.sorted(Comparator.comparingInt((Entry entry) -> entry.alert().priority())
						.thenComparingLong(Entry::since).reversed())
				.limit(MAX_SHOWN)
				.map(entry -> new Shown(entry.alert(), alpha(entry, now)))
				.toList();
	}

	private static float alpha(Entry entry, long now) {
		long age = Math.max(0, now - entry.since());
		long left = entry.alert().millis() - age;
		return Math.min(1f, Math.min(age / (float) FADE_IN, left / (float) FADE_OUT));
	}
}
