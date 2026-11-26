package io.github.filipovych.filobulus.profit;

import java.util.LinkedHashMap;
import java.util.Map;

// how much of each item was gained, and for how long the player was at it
// amounts are kept and priced when shown, so old totals follow today's prices
public final class ProfitTotals {
	private final Map<String, Long> items = new LinkedHashMap<>();
	private long activeMillis;
	// corpses opened and what they paid with the keys taken off, in coins at the time they were opened
	private int corpses;
	private double corpseCoins;
	// when the last gain or activity came in, counting is false until there was one
	private long lastGain;
	private boolean counting;
	private int version;

	public ProfitTotals() {
	}

	// what was saved, the clock of a saved total starts again with the next gain
	public ProfitTotals(Map<String, Long> items, long activeMillis, int corpses, double corpseCoins) {
		this(items, activeMillis);
		this.corpses = Math.max(0, corpses);
		this.corpseCoins = corpseCoins;
	}

	public ProfitTotals(Map<String, Long> items, long activeMillis) {
		items.forEach((id, amount) -> {
			if (amount > 0) {
				this.items.put(id, amount);
			}
		});
		this.activeMillis = Math.max(0, activeMillis);
	}

	public void add(String id, long amount, long now, long idle) {
		add(id, amount, now, idle, now);
	}

	// amount can be negative, to take back something that turned out to be no gain
	// idle is how long after a gain the clock keeps running without another one
	// since is when the gain started, a sack message adds up the last 30 seconds, so the first one does not count as 0 seconds
	public void add(String id, long amount, long now, long idle, long since) {
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
			activity(now, idle, since);
		}
	}

	// a corpse was opened, coins is its loot with the key taken off
	public void corpse(double coins, long now, long idle) {
		version++;
		corpses++;
		corpseCoins += coins;
		activity(now, idle, now);
	}

	// the player is busy without gaining anything, like mining a block, the clock keeps running but nothing is added
	public void active(long now, long idle) {
		version++;
		activity(now, idle, now);
	}

	private void activity(long now, long idle, long since) {
		if (counting && now - lastGain <= idle) {
			activeMillis += Math.max(0, now - lastGain);
		} else {
			activeMillis += Math.min(idle, Math.max(0, now - since));
		}
		lastGain = now;
		counting = true;
	}

	public int corpses() {
		return corpses;
	}

	public double corpseCoins() {
		return corpseCoins;
	}

	public Map<String, Long> items() {
		return Map.copyOf(items);
	}

	public boolean isEmpty() {
		return items.isEmpty() && corpses == 0;
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

	// takes over what another total holds, the clock starts again with the next gain
	public void replaceWith(ProfitTotals other) {
		items.clear();
		items.putAll(other.items);
		activeMillis = other.activeMillis;
		corpses = other.corpses;
		corpseCoins = other.corpseCoins;
		counting = false;
		version++;
	}

	public void clear() {
		items.clear();
		activeMillis = 0;
		corpses = 0;
		corpseCoins = 0;
		counting = false;
		version++;
	}
}
