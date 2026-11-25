package io.github.filipovych.filobulus.profit;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

// decides what counts as a gain: items that show up in the sacks or in the inventory
// moving items between the two is no gain, so each move is held against the other side for a minute
public final class ProfitLedger {
	private static final long WINDOW = 60_000;

	private static final class Move {
		final String id;
		final long at;
		final boolean counted;
		long amount;

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
	private long quietUntil;
	private long idle = 60_000;

	public ProfitLedger(List<ProfitTotals> totals) {
		this.totals = List.copyOf(totals);
	}

	public void idle(long millis) {
		idle = millis;
	}

	// something was bought or claimed, what lands in the sacks in the next minute is not loot
	public void quiet(long now) {
		quietUntil = now + WINDOW;
	}

	// a line of a sack message, amount is negative for what was taken out
	public void sack(String id, long amount, long now) {
		prune(now);
		if (amount > 0) {
			if (now < quietUntil) {
				return;
			}
			credit(id, amount - take(leftInventory, id, amount), now);
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
			if (move.counted) {
				credit(id, -part, now);
			}
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
			}
			return;
		}
		long gained = delta - take(leftSacks, id, delta);
		if (gained <= 0) {
			return;
		}
		if (counted) {
			credit(id, gained, now);
		}
		arrived.add(new Move(id, now, gained, counted));
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

	private void credit(String id, long amount, long now) {
		for (ProfitTotals each : totals) {
			each.add(id, amount, now, idle);
		}
	}

	private void prune(long now) {
		for (List<Move> moves : List.of(leftSacks, leftInventory, arrived)) {
			for (Iterator<Move> each = moves.iterator(); each.hasNext();) {
				Move move = each.next();
				if (move.amount <= 0 || now - move.at > WINDOW) {
					each.remove();
				}
			}
		}
	}
}
