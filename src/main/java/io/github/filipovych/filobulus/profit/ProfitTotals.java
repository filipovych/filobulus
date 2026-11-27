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
	// when the player last did something, and up to when the clock has counted
	// the clock runs on for the pause after the last activity and then stands still, it never goes back
	private long lastActivity;
	private long countedUntil;
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

	public void add(String id, long amount, long now, long pause) {
		add(id, amount, now, pause, now);
	}

	// amount can be negative, to take back something that turned out to be no gain
	// pause is how long after the last activity the clock keeps running
	// since is when the gain started, a sack message adds up the last 30 seconds, so the first one does not count as 0 seconds
	public void add(String id, long amount, long now, long pause, long since) {
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
			activity(now, pause, since);
		}
	}

	// a corpse was opened, coins is its loot with the key taken off
	public void corpse(double coins, long now, long pause) {
		version++;
		corpses++;
		corpseCoins += coins;
		activity(now, pause, now);
	}

	// the player is busy without gaining anything, mining or moving, the clock keeps running but nothing is added
	public void active(long now, long pause) {
		activity(now, pause, now);
	}

	// moves the clock on, called often so the time on show grows smoothly
	public void tick(long now, long pause) {
		if (!counting) {
			return;
		}
		long until = Math.min(now, lastActivity + pause);
		if (until > countedUntil) {
			activeMillis += until - countedUntil;
			countedUntil = until;
		}
	}

	private void activity(long now, long pause, long since) {
		tick(now, pause);
		// the part of what the activity covers that the clock did not count, at most a pause of it
		long from = counting ? Math.max(since, countedUntil) : since;
		activeMillis += Math.min(pause, Math.max(0, now - from));
		lastActivity = now;
		countedUntil = Math.max(countedUntil, now);
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

	// the time the player was at it, with the clock still running for a pause after the last activity
	public long activeMillis(long now, long pause) {
		if (!counting) {
			return activeMillis;
		}
		return activeMillis + Math.max(0, Math.min(now, lastActivity + pause) - countedUntil);
	}

	// true once nothing happened for a pause, the clock stands still then
	public boolean paused(long now, long pause) {
		return counting && now - lastActivity > pause;
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
