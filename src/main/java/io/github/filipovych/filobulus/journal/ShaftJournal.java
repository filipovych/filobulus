package io.github.filipovych.filobulus.journal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// every shaft the player was ever in, oldest first
public final class ShaftJournal {
	private final List<ShaftVisit> visits = new ArrayList<>();
	private int version;

	public List<ShaftVisit> visits() {
		return Collections.unmodifiableList(visits);
	}

	// goes up with every change, so a screen knows when to count again
	public int version() {
		return version;
	}

	// a visit that is already there is replaced, that is how a shaft gets its time once we leave it
	public void put(ShaftVisit visit) {
		version++;
		// nearly always about the newest one, so look from the end
		int i = visits.size() - 1;
		while (i >= 0 && visits.get(i).enteredAt() > visit.enteredAt()) {
			i--;
		}
		if (i >= 0 && visits.get(i).enteredAt() == visit.enteredAt()) {
			visits.set(i, visit);
		} else {
			visits.add(i + 1, visit);
		}
	}

	// the visits from a moment on
	public List<ShaftVisit> since(long from) {
		int i = visits.size();
		while (i > 0 && visits.get(i - 1).enteredAt() >= from) {
			i--;
		}
		return Collections.unmodifiableList(visits.subList(i, visits.size()));
	}

	// shafts entered after the last jasper one, all of them when there never was one
	public int sinceJasper() {
		int count = 0;
		for (int i = visits.size() - 1; i >= 0 && !visits.get(i).isJasper(); i--) {
			count++;
		}
		return count;
	}
}
