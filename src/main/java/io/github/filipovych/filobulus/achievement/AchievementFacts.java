package io.github.filipovych.filobulus.achievement;

import io.github.filipovych.filobulus.corpse.CorpseTally;
import io.github.filipovych.filobulus.journal.ShaftVisit;
import io.github.filipovych.filobulus.shaft.ShaftMaterial;
import io.github.filipovych.filobulus.shaft.ShaftType;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

// everything the achievements look at, counted in one go over the journal
// lootedByType is lapis, tungsten, umber, vanguard, fastestRun is 0 while no route was timed
// longestDryEnded is the longest run of shafts without jasper that a jasper ended, currentDry the one still going
public record AchievementFacts(int shafts, int crystal, int jasper, int jasperCrystal, int jasperRun,
		int longestDryEnded, int currentDry, Set<ShaftMaterial> materials, int[] lootedByType, boolean fullHouse,
		long bestShaft, long coins, int bestDay, long fastestRun, long timeInside) {
	public static final AchievementFacts NONE = of(List.of(), ZoneId.systemDefault());

	public AchievementFacts {
		materials = materials.isEmpty() ? Set.of() : Set.copyOf(materials);
	}

	public static AchievementFacts of(List<ShaftVisit> visits, ZoneId zone) {
		int crystal = 0;
		int jasper = 0;
		int jasperCrystal = 0;
		int run = 0;
		int jasperRun = 0;
		int dry = 0;
		int longestDryEnded = 0;
		Set<ShaftMaterial> materials = EnumSet.noneOf(ShaftMaterial.class);
		int[] looted = new int[4];
		boolean fullHouse = false;
		long bestShaft = 0;
		long coins = 0;
		LocalDate day = null;
		int today = 0;
		int bestDay = 0;
		long fastestRun = 0;
		long timeInside = 0;
		for (ShaftVisit visit : visits) {
			ShaftType type = visit.type().orElse(null);
			if (type != null) {
				materials.add(type.material());
				crystal += type.isCrystal() ? 1 : 0;
			}
			if (visit.isJasper()) {
				jasper++;
				jasperCrystal += type.isCrystal() ? 1 : 0;
				jasperRun = Math.max(jasperRun, ++run);
				longestDryEnded = Math.max(longestDryEnded, dry);
				dry = 0;
			} else {
				run = 0;
				dry++;
			}
			CorpseTally corpses = visit.corpses();
			boolean all = true;
			for (int i = 0; i < 4; i++) {
				int count = corpses.looted(CorpseTally.ORDER.charAt(i));
				looted[i] += count;
				all &= count > 0;
			}
			fullHouse |= all;
			bestShaft = Math.max(bestShaft, visit.coins());
			coins += visit.coins();
			LocalDate date = Instant.ofEpochMilli(visit.enteredAt()).atZone(zone).toLocalDate();
			today = date.equals(day) ? today + 1 : 1;
			day = date;
			bestDay = Math.max(bestDay, today);
			if (visit.runMillis() > 0 && (fastestRun == 0 || visit.runMillis() < fastestRun)) {
				fastestRun = visit.runMillis();
			}
			timeInside += visit.spentMillis();
		}
		return new AchievementFacts(visits.size(), crystal, jasper, jasperCrystal, jasperRun, longestDryEnded, dry,
				materials, looted, fullHouse, bestShaft, coins, bestDay, fastestRun, timeInside);
	}

	public int looted() {
		return lootedByType[0] + lootedByType[1] + lootedByType[2] + lootedByType[3];
	}

	public int looted(char letter) {
		return lootedByType[CorpseTally.ORDER.indexOf(letter)];
	}

	public boolean seen(ShaftMaterial material) {
		return materials.contains(material);
	}

	public int gemstoneKinds() {
		return (int) materials.stream().filter(ShaftMaterial::isGemstone).count();
	}
}
