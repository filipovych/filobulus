package io.github.filipovych.filobulus.profit;

import java.util.LinkedHashMap;
import java.util.Map;

// how much of each item was gained, and for how long the player was at it
// amounts are kept and priced when shown, so old totals follow today's prices
public final class ProfitTotals {
	private final Map<String, Long> items = new LinkedHashMap<>();
	private long activeMillis;
	// when the last gain came in, counting is false until there was one
	private long lastGain;
	private boolean counting;
	private int version;

	public ProfitTotals() {
	}

	// what was saved, the clock of a saved total starts again with the next gain
	public ProfitTotals(Map<String, Long> items, long activeMillis) {
		items.forEach((id, amount) -> {
			if (amount > 0) {
				this.items.put(id, amount);
			}
		});
		this.activeMillis = Math.max(0, activeMillis);
	}

	// amount can be negative, to take back something that turned out to be no gain
	// idle is how long after a gain the clock keeps running without another one
	public void add(String id, long amount, long now, long idle) {
		if (amount == 0) {
			return;
		}
		version++;
		long total = items.getOrDefault(id, 0L) + amount;
		if (total > 0) {
			items.put(id, total);
		} else {
			items.remove(id);
		}
		if (amount > 0) {
			if (counting && now - lastGain <= idle) {
				activeMillis += Math.max(0, now - lastGain);
			}
			lastGain = now;
			counting = true;
		}
	}

	public Map<String, Long> items() {
		return Map.copyOf(items);
	}

	public boolean isEmpty() {
		return items.isEmpty();
	}

	// the time since the last gain counts while it is short, and is dropped again when nothing more comes
	// so standing around does not pull the coins per hour down
	public long activeMillis(long now, long idle) {
		boolean running = counting && now - lastGain <= idle;
		return activeMillis + (running ? Math.max(0, now - lastGain) : 0);
	}

	// goes up with every change, for saving and for counting again
	public int version() {
		return version;
	}

	public void clear() {
		items.clear();
		activeMillis = 0;
		counting = false;
		version++;
	}
}
