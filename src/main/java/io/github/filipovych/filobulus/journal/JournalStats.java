package io.github.filipovych.filobulus.journal;

import io.github.filipovych.filobulus.corpse.CorpseTally;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// what a list of visits adds up to
// timed is how many of them have a known time inside, the average only counts those
// activeMillis is the time spent shafting, the breaks between sessions left out
public record JournalStats(int shafts, int jasper, int longestDry, long timeInside, int timed, long activeMillis, int gaps,
		Corpses corpses, List<Group> groups) {
	// a gap this long between two shafts is a break and not time spent looking for the next one
	private static final long BREAK_MILLIS = 20 * 60_000;

	// the visits of one shaft layout, key is a type code, CRYSTAL, or empty for shafts whose type was never read
	public record Group(String key, int shafts, long timeInside, int timed, long bestRun, int corpses, int looted, long coins) {
		public Group(String key, int shafts, long timeInside, int timed, long bestRun) {
			this(key, shafts, timeInside, timed, bestRun, 0, 0, 0);
		}

		public long averageTime() {
			return timed == 0 ? 0 : timeInside / timed;
		}
	}

	// the two arrays are per corpse type in the order lapis, tungsten, umber, vanguard
	// counted is the shafts whose corpses were read, paid the ones where a corpse was opened
	public record Corpses(int[] seenByType, int[] lootedByType, int counted, int paid, long coins, long mined, long best) {
		public int seen() {
			return seenByType[0] + seenByType[1] + seenByType[2] + seenByType[3];
		}

		public int looted() {
			return lootedByType[0] + lootedByType[1] + lootedByType[2] + lootedByType[3];
		}

		// lapis opens without one
		public int keys() {
			return lootedByType[1] + lootedByType[2] + lootedByType[3];
		}

		public double perShaft() {
			return counted == 0 ? 0 : (double) seen() / counted;
		}

		public long coinsPerShaft() {
			return paid == 0 ? 0 : coins / paid;
		}
	}

	public JournalStats {
		groups = List.copyOf(groups);
	}

	public static JournalStats of(List<ShaftVisit> visits) {
		Map<String, long[]> byKey = new LinkedHashMap<>();
		int jasper = 0;
		int dry = 0;
		int longestDry = 0;
		long timeInside = 0;
		int timed = 0;
		long active = 0;
		int gaps = 0;
		int[] seen = new int[4];
		int[] looted = new int[4];
		int counted = 0;
		int paid = 0;
		long coins = 0;
		long mined = 0;
		long best = 0;
		for (int i = 0; i < visits.size(); i++) {
			ShaftVisit visit = visits.get(i);
			// shafts, time inside, timed, best run, corpses, looted, coins
			long[] sums = byKey.computeIfAbsent(visit.key(), key -> new long[7]);
			sums[0]++;
			if (visit.spentMillis() > 0) {
				sums[1] += visit.spentMillis();
				sums[2]++;
				timeInside += visit.spentMillis();
				timed++;
			}
			if (visit.runMillis() > 0 && (sums[3] == 0 || visit.runMillis() < sums[3])) {
				sums[3] = visit.runMillis();
			}
			if (visit.isJasper()) {
				jasper++;
				dry = 0;
			} else {
				longestDry = Math.max(longestDry, ++dry);
			}
			// up to the next shaft it is all shafting, unless that took so long it was a break
			long gap = i + 1 < visits.size() ? visits.get(i + 1).enteredAt() - visit.enteredAt() : Long.MAX_VALUE;
			if (gap <= BREAK_MILLIS) {
				active += gap;
				gaps++;
			} else {
				active += visit.spentMillis();
			}

			CorpseTally corpses = visit.corpses();
			if (!corpses.isEmpty()) {
				counted++;
				for (int type = 0; type < 4; type++) {
					char letter = CorpseTally.ORDER.charAt(type);
					seen[type] += corpses.seen(letter);
					looted[type] += corpses.looted(letter);
				}
				sums[4] += corpses.seen().length();
				sums[5] += corpses.looted().length();
				if (!corpses.looted().isEmpty()) {
					paid++;
				}
			}
			sums[6] += visit.coins();
			coins += visit.corpseCoins();
			mined += visit.minedCoins();
			best = Math.max(best, visit.coins());
		}
		List<Group> groups = new ArrayList<>();
		byKey.forEach((key, sums) -> groups.add(new Group(key, (int) sums[0], sums[1], (int) sums[2], sums[3],
				(int) sums[4], (int) sums[5], sums[6])));
		// most entered first, the key keeps the order steady between equal ones
		groups.sort(Comparator.comparingInt(Group::shafts).reversed().thenComparing(Group::key));
		return new JournalStats(visits.size(), jasper, longestDry, timeInside, timed, active, gaps,
				new Corpses(seen, looted, counted, paid, coins, mined, best), groups);
	}

	public long averageTime() {
		return timed == 0 ? 0 : timeInside / timed;
	}

	// share of the shafts that were jasper, 0 to 1
	public double jasperRate() {
		return shafts == 0 ? 0 : (double) jasper / shafts;
	}

	// shafts entered per hour of shafting, 0 while there is too little to go by
	public double perHour() {
		return activeMillis < 60_000 ? 0 : gaps * 3_600_000d / activeMillis;
	}
}
