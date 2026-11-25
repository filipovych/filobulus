package io.github.filipovych.filobulus.journal;

import java.util.List;

// what there is to say about one shaft once it is over, next to the shafts that came before it
// average is what earlier shafts of the same layout paid, over sampled of them, 0 when there are too few to say
// number is which shaft of its layout this was, dry the shafts without a jasper that led up to it, this one included when it is none
public record ShaftRecap(ShaftVisit visit, long average, int sampled, int number, int dry) {
	private static final int MIN_SAMPLED = 3;

	// journal is every visit, oldest first, the one the recap is about can be anywhere in it
	public static ShaftRecap of(ShaftVisit visit, List<ShaftVisit> journal) {
		long coins = 0;
		int sampled = 0;
		int number = 1;
		int dry = visit.isJasper() ? 0 : 1;
		for (ShaftVisit earlier : journal) {
			if (earlier.enteredAt() >= visit.enteredAt()) {
				break;
			}
			dry = earlier.isJasper() ? (visit.isJasper() ? 0 : 1) : dry + 1;
			if (!earlier.key().equals(visit.key())) {
				continue;
			}
			number++;
			if (earlier.coins() != 0) {
				coins += earlier.coins();
				sampled++;
			}
		}
		return new ShaftRecap(visit, sampled < MIN_SAMPLED ? 0 : coins / sampled, sampled, number, dry);
	}

	public boolean hasAverage() {
		return sampled >= MIN_SAMPLED;
	}

	// coins per hour of the time inside, 0 when the shaft was too short to say
	public long perHour() {
		long spent = visit.spentMillis();
		return spent < 10_000 ? 0 : Math.round(visit.coins() * 3_600_000d / spent);
	}
}
