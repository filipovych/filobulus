package io.github.filipovych.filobulus.mining;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;

// how many ticks the server really took for the blocks we watched, by block kind and the speed the sum used
// the sum can be off, the stats widget, the boost or the strength table, a block we saw go is the truth
// a measurement is the tick the block showed gone, less the ping, the median of the last few is what counts
public final class BreakTimes {
	static final int KEEP = 5;
	static final int ENOUGH = 3;

	private final Map<String, int[]> seen = new HashMap<>();
	private final Map<String, Integer> counts = new HashMap<>();

	public static String key(BlockStrength block, int speed) {
		return block.name() + ":" + speed;
	}

	// formula is what the sum says for the block, the real time is never far off that
	// so anything under half or over double it is something else, a pickobulus, another player or time carried over
	public void record(String key, int ticks, int formula) {
		if (ticks < 1 || ticks * 2 < formula || ticks > formula * 2) {
			return;
		}
		int[] ring = seen.computeIfAbsent(key, k -> new int[KEEP]);
		int count = counts.getOrDefault(key, 0);
		ring[count % KEEP] = ticks;
		counts.put(key, count + 1);
	}

	// the median of the last measurements, empty until there are enough to trust
	public OptionalInt learned(String key) {
		int count = counts.getOrDefault(key, 0);
		if (count < ENOUGH) {
			return OptionalInt.empty();
		}
		int[] ring = seen.get(key);
		int[] sorted = Arrays.copyOf(ring, Math.min(count, KEEP));
		Arrays.sort(sorted);
		return OptionalInt.of(sorted[sorted.length / 2]);
	}

	public void clear() {
		seen.clear();
		counts.clear();
	}
}
