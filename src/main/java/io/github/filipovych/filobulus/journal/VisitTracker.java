package io.github.filipovych.filobulus.journal;

import io.github.filipovych.filobulus.corpse.CorpseTally;
import io.github.filipovych.filobulus.corpse.CorpseType;
import java.util.ArrayList;
import java.util.List;

// turns "in a shaft or not" into visits, every method gives back the visits that changed and have to be written
public final class VisitTracker {
	// nobody gets from one shaft into the next this fast, so it is still the same one
	private static final long SAME_SHAFT_MILLIS = 10_000;

	private ShaftVisit open;
	private ShaftVisit last;
	// the world of the open visit, or of the last one once it is closed
	private Object world;

	// the shaft we are in right now
	public ShaftVisit open() {
		return open;
	}

	// world is whatever stands for the server we are on, a new shaft always comes with a new one
	public List<ShaftVisit> update(boolean inside, String code, Object world, long now) {
		List<ShaftVisit> changed = new ArrayList<>();
		if (open != null && (!inside || !code.isEmpty() && !open.code().isEmpty() && !code.equals(open.code()))) {
			last = open.withSpent(now - open.enteredAt());
			open = null;
			changed.add(last);
		}
		if (!inside) {
			return changed;
		}
		if (open == null) {
			// a read that fell out for a moment must not log the same shaft twice
			open = resumes(code, world, now) ? last.withSpent(0) : ShaftVisit.entered(now, code);
			this.world = world;
			changed.add(open);
		}
		// the type code is on the sidebar a moment after the area is in the tab list
		if (open.code().isEmpty() && !code.isEmpty()) {
			open = open.withCode(code);
			changed.add(open);
		}
		return changed;
	}

	// the route of the shaft we are in was finished
	public List<ShaftVisit> ran(long runMillis) {
		if (open == null || runMillis <= 0) {
			return List.of();
		}
		open = open.withRun(runMillis);
		return List.of(open);
	}

	// the corpses the tab list shows right now, written when it tells us something new
	public List<ShaftVisit> corpses(CorpseTally shown) {
		if (open == null) {
			return List.of();
		}
		CorpseTally known = open.corpses().merge(shown);
		if (known.equals(open.corpses())) {
			return List.of();
		}
		open = open.withCorpses(known, open.corpseCoins());
		return List.of(open);
	}

	// a corpse was opened, coins is what its loot was worth with the key taken off
	public List<ShaftVisit> looted(CorpseType type, long coins) {
		if (open == null) {
			return List.of();
		}
		open = open.withCorpses(open.corpses().withLooted(type), open.corpseCoins() + coins);
		return List.of(open);
	}

	// what the shaft paid in mined items so far
	public List<ShaftVisit> mined(long coins) {
		if (open == null || coins == open.minedCoins()) {
			return List.of();
		}
		open = open.withMined(coins);
		return List.of(open);
	}

	private boolean resumes(String code, Object world, long now) {
		if (last == null) {
			return false;
		}
		boolean typeFits = code.equals(last.code()) || code.isEmpty() || last.code().isEmpty();
		boolean justLeft = now - (last.enteredAt() + last.spentMillis()) < SAME_SHAFT_MILLIS;
		return typeFits && (world == this.world || justLeft);
	}
}
