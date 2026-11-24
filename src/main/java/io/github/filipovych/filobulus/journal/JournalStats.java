package io.github.filipovych.filobulus.journal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// what a list of visits adds up to
// timed is how many of them have a known time inside, the average only counts those
public record JournalStats(int shafts, int jasper, int longestDry, long timeInside, int timed, List<Group> groups) {
	// the visits of one shaft layout, key is a type code, CRYSTAL, or empty for shafts whose type was never read
	public record Group(String key, int shafts, long timeInside, int timed, long bestRun) {
		public long averageTime() {
			return timed == 0 ? 0 : timeInside / timed;
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
		for (ShaftVisit visit : visits) {
			// shafts, time inside, timed, best run
			long[] sums = byKey.computeIfAbsent(visit.key(), key -> new long[4]);
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
		}
		List<Group> groups = new ArrayList<>();
		byKey.forEach((key, sums) -> groups.add(new Group(key, (int) sums[0], sums[1], (int) sums[2], sums[3])));
		// most entered first, the key keeps the order steady between equal ones
		groups.sort(Comparator.comparingInt(Group::shafts).reversed().thenComparing(Group::key));
		return new JournalStats(visits.size(), jasper, longestDry, timeInside, timed, groups);
	}

	public long averageTime() {
		return timed == 0 ? 0 : timeInside / timed;
	}

	// share of the shafts that were jasper, 0 to 1
	public double jasperRate() {
		return shafts == 0 ? 0 : (double) jasper / shafts;
	}
}
