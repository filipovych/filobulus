package io.github.filipovych.filobulus.profit;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

// decides what counts as a gain: items that show up in the sacks or in the inventory
// moving items between the two is no gain, so each move is held against the other side for a minute
public final class ProfitLedger {
	private static final long WINDOW = 60_000;

	private static final class Move {
		final String id;
		final long at;
		final boolean counted;
		long amount;
		// for something made, the inventory gain it came in with
		Move arrival;

		Move(String id, long at, long amount, boolean counted) {
			this.id = id;
			this.at = at;
			this.amount = amount;
			this.counted = counted;
		}
	}

	private final List<ProfitTotals> totals;
	// taken out of the sacks, waiting to show up in the inventory
	private final List<Move> leftSacks = new ArrayList<>();
	// taken out of the inventory with a menu open, waiting to show up in the sacks
	private final List<Move> leftInventory = new ArrayList<>();
	// came into the inventory, a sack line that comes later may say where from
	private final List<Move> arrived = new ArrayList<>();
	// loot of a corpse, already counted as the corpse, so it is no gain when it lands in the sacks or the inventory
	private final List<Move> fromCorpse = new ArrayList<>();
	// left the inventory with no menu open, a compactor or the crafting grid may turn it into something else
	private final List<Move> used = new ArrayList<>();
	// an item a recipe makes was gained, the sack line for what went into it may still come
	private final List<Move> made = new ArrayList<>();
	private long quietUntil;
	private long idle = 60_000;

	public ProfitLedger(List<ProfitTotals> totals) {
		this.totals = List.copyOf(totals);
	}

	public void idle(long millis) {
		idle = millis;
	}

	// the player is mining, the clock keeps running even when no gain came in for a while
	public void active(long now) {
		for (ProfitTotals each : totals) {
			each.active(now, idle);
		}
	}

	// something was bought or claimed, what lands in the sacks in the next minute is not loot
	public void quiet(long now) {
		quietUntil = now + WINDOW;
	}

	public void sack(String id, long amount, long now) {
		sack(id, amount, now, 0);
	}

	// a corpse was opened, its coins count once and its items do not count again when they arrive
	public void corpse(double coins, List<Map.Entry<String, Long>> items, long now) {
		prune(now);
		for (Map.Entry<String, Long> item : items) {
			fromCorpse.add(new Move(item.getKey(), now, item.getValue(), false));
		}
		for (ProfitTotals each : totals) {
			each.corpse(coins, now, idle);
		}
	}

	// a line of a sack message, amount is negative for what was taken out
	// window is how long the message adds up, the clock is started that far back when it was not running
	public void sack(String id, long amount, long now, long window) {
		prune(now);
		if (amount > 0) {
			if (now < quietUntil) {
				return;
			}
			long left = amount - take(fromCorpse, id, amount);
			left -= take(leftInventory, id, left);
			if (left > 0) {
				credit(id, left, now, now - window);
				compacted(id, left, now, null);
			}
			return;
		}
		long left = -amount;
		// the newest first, that is the one the sack line is most likely about
		for (int i = arrived.size() - 1; i >= 0 && left > 0; i--) {
			Move move = arrived.get(i);
			if (!move.id.equals(id)) {
				continue;
			}
			long part = Math.min(left, move.amount);
			move.amount -= part;
			left -= part;
			// it came out of the sack, so it was not made either
			take(made, id, part);
			if (move.counted) {
				credit(id, -part, now, now);
			}
		}
		// right after a trade a line going out is a sale, not a craft
		if (now >= quietUntil) {
			left = usedFor(id, left, now);
		}
		if (left > 0) {
			leftSacks.add(new Move(id, now, left, false));
		}
	}

	// the inventory holds more or less of an item than a tick ago
	// counted is false while a menu is open or the world just changed, then a gain is no loot
	public void inventory(String id, long delta, long now, boolean counted) {
		prune(now);
		if (delta < 0) {
			// putting items into a sack happens in its menu, anything else that leaves is not coming back as loot
			if (!counted) {
				leftInventory.add(new Move(id, now, -delta, false));
			} else {
				used.add(new Move(id, now, -delta, false));
			}
			return;
		}
		long gained = delta - take(leftSacks, id, delta);
		if (gained > 0) {
			gained -= take(fromCorpse, id, gained);
		}
		if (gained <= 0) {
			return;
		}
		if (counted) {
			credit(id, gained, now, now);
		}
		Move arrival = new Move(id, now, gained, counted);
		arrived.add(arrival);
		compacted(id, gained, now, arrival);
	}

	// 80 rough turning into 1 flawed is no new loot, the rough that went into it is taken back off
	// the input may have left the inventory or the sacks, before or after, arrival is null for a sack line
	// an output that was not counted, because a menu was open, counts when it was made from inputs we saw go
	private void compacted(String output, long gained, long now, Move arrival) {
		Compaction.Recipe recipe = Compaction.of(output).orElse(null);
		if (recipe == null) {
			return;
		}
		long wanted = recipe.count() * gained;
		long taken = take(used, recipe.input(), wanted);
		taken += take(leftSacks, recipe.input(), wanted - taken);
		taken += take(leftInventory, recipe.input(), wanted - taken);
		credit(recipe.input(), -taken, now, now);
		long crafted = Math.min(gained, Math.ceilDiv(taken, recipe.count()));
		boolean counted = arrival == null || arrival.counted;
		if (!counted && crafted > 0) {
			credit(output, crafted, now, now);
			countArrival(arrival, crafted, now);
		}
		if (gained > crafted) {
			Move waiting = new Move(output, now, gained - crafted, counted);
			waiting.arrival = arrival;
			made.add(waiting);
		}
	}

	// a sack line took out an input after its output was gained, so it went into that output
	// gives back how much of the line is left over
	private long usedFor(String input, long amount, long now) {
		long left = amount;
		for (Move move : made) {
			if (left == 0) {
				break;
			}
			Compaction.Recipe recipe = Compaction.of(move.id).orElse(null);
			if (recipe == null || !recipe.input().equals(input) || move.amount <= 0) {
				continue;
			}
			long outputs = Math.min(move.amount, Math.ceilDiv(left, recipe.count()));
			long inputs = Math.min(left, outputs * recipe.count());
			move.amount -= outputs;
			left -= inputs;
			credit(input, -inputs, now, now);
			if (!move.counted) {
				credit(move.id, outputs, now, now);
				countArrival(move.arrival, outputs, now);
			}
		}
		return left;
	}

	// part of an inventory gain that was not counted turned out to be made, so it counts now
	// split off, so a sack line that later says it was taken out takes it back again
	private void countArrival(Move arrival, long amount, long now) {
		long part = Math.min(amount, arrival.amount);
		if (part > 0) {
			arrival.amount -= part;
			arrived.add(new Move(arrival.id, now, part, true));
		}
	}

	// uses up to amount of what is waiting for that item, gives back how much it was
	private static long take(List<Move> moves, String id, long amount) {
		long taken = 0;
		for (Move move : moves) {
			if (taken == amount) {
				break;
			}
			if (move.id.equals(id)) {
				long part = Math.min(amount - taken, move.amount);
				move.amount -= part;
				taken += part;
			}
		}
		return taken;
	}

	private void credit(String id, long amount, long now, long since) {
		if (amount == 0) {
			return;
		}
		for (ProfitTotals each : totals) {
			each.add(id, amount, now, idle, since);
		}
	}

	private void prune(long now) {
		for (List<Move> moves : List.of(leftSacks, leftInventory, arrived, fromCorpse, used, made)) {
			for (Iterator<Move> each = moves.iterator(); each.hasNext();) {
				Move move = each.next();
				if (move.amount <= 0 || now - move.at > WINDOW) {
					each.remove();
				}
			}
		}
	}
}
