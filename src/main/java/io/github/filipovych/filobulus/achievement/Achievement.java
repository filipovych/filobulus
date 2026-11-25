package io.github.filipovych.filobulus.achievement;

import io.github.filipovych.filobulus.journal.ShaftVisit;
import java.util.List;
import java.util.function.Predicate;

// a first, small set to try the system with, each one is worked out from the journal so it can never be missed
public enum Achievement {
	FIRST_SHAFT("First Descent", "Enter a mineshaft.", visits -> !visits.isEmpty()),
	JASPER("Pink at Last", "Enter a jasper shaft.", visits -> visits.stream().anyMatch(ShaftVisit::isJasper)),
	CORPSES("Grave Digger", "Open 10 corpses.", visits -> visits.stream().mapToInt(visit -> visit.corpses().looted().length()).sum() >= 10);

	private final String title;
	private final String description;
	private final Predicate<List<ShaftVisit>> earned;

	Achievement(String title, String description, Predicate<List<ShaftVisit>> earned) {
		this.title = title;
		this.description = description;
		this.earned = earned;
	}

	public String title() {
		return title;
	}

	public String description() {
		return description;
	}

	public boolean earned(List<ShaftVisit> visits) {
		return earned.test(visits);
	}
}
