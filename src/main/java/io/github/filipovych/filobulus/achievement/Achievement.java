package io.github.filipovych.filobulus.achievement;

import static io.github.filipovych.filobulus.achievement.AchievementRarity.*;

import io.github.filipovych.filobulus.corpse.CoinText;
import io.github.filipovych.filobulus.shaft.ShaftMaterial;
import java.util.Arrays;
import java.util.Locale;
import java.util.function.ToLongFunction;

// every achievement is worked out from the journal so it can never be missed
// the names are what achievements.json keeps, so they never change
public enum Achievement {
	FIRST_SHAFT("First Descent", "Enter a mineshaft.", COMMON, Unit.COUNT, 1, AchievementFacts::shafts),
	SHAFTS_10("Regular", "Enter 10 mineshafts.", COMMON, Unit.COUNT, 10, AchievementFacts::shafts),
	SHAFTS_100("Tunnel Vision", "Enter 100 mineshafts.", UNCOMMON, Unit.COUNT, 100, AchievementFacts::shafts),
	SHAFTS_500("Deep Roots", "Enter 500 mineshafts.", RARE, Unit.COUNT, 500, AchievementFacts::shafts),
	SHAFTS_1000("Lives Down Here", "Enter 1,000 mineshafts.", EPIC, Unit.COUNT, 1000, AchievementFacts::shafts),
	DAY_10("Busy Day", "Enter 10 mineshafts in one day.", UNCOMMON, Unit.COUNT, 10, AchievementFacts::bestDay),
	DAY_50("No Days Off", "Enter 50 mineshafts in one day.", EPIC, Unit.COUNT, 50, AchievementFacts::bestDay, true),
	TIME_24H("Day Shift", "Spend 24 hours inside mineshafts.", RARE, Unit.HOURS, 24 * 3_600_000L, AchievementFacts::timeInside),
	CRYSTAL("Crystal Clear", "Enter a crystal shaft.", UNCOMMON, Unit.COUNT, 1, AchievementFacts::crystal),
	GEMSTONES("Full Spectrum", "Enter every gemstone shaft at least once.", EPIC, Unit.COUNT, Gemstones.COUNT,
			AchievementFacts::gemstoneKinds),
	VANGUARD_SHAFT("Fair Game", "Enter a Vanguard shaft.", RARE, Unit.COUNT, 1,
			facts -> facts.seen(ShaftMaterial.VANGUARD) ? 1 : 0),
	DEN("Big Feet", "Find Littlefoot's Den.", EPIC, Unit.COUNT, 1,
			facts -> facts.seen(ShaftMaterial.LITTLEFOOT) ? 1 : 0, true),
	JASPER("Pink at Last", "Enter a jasper shaft.", UNCOMMON, Unit.COUNT, 1, AchievementFacts::jasper),
	JASPER_10("Seeing Pink", "Enter 10 jasper shafts.", RARE, Unit.COUNT, 10, AchievementFacts::jasper),
	JASPER_50("Pink Tide", "Enter 50 jasper shafts.", LEGENDARY, Unit.COUNT, 50, AchievementFacts::jasper),
	JASPER_CRYSTAL("Rose Tinted", "Enter a jasper crystal shaft.", LEGENDARY, Unit.COUNT, 1,
			AchievementFacts::jasperCrystal, true),
	JASPER_TWICE("Lightning Strikes Twice", "Enter two jasper shafts in a row.", MYTHIC, Unit.COUNT, 1,
			facts -> facts.jasperRun() >= 2 ? 1 : 0, true),
	DRY_50("Drought", "End a dry streak of 50 shafts with a jasper.", RARE, Unit.COUNT, 50, dry(50)),
	DRY_100("Desert Walker", "End a dry streak of 100 shafts with a jasper.", EPIC, Unit.COUNT, 100, dry(100)),
	CORPSES("Grave Digger", "Open 10 corpses.", COMMON, Unit.COUNT, 10, AchievementFacts::looted),
	CORPSES_1000("Undertaker", "Open 1,000 corpses.", EPIC, Unit.COUNT, 1000, AchievementFacts::looted),
	TUNGSTEN_100("Heavy Metal", "Open 100 tungsten corpses.", RARE, Unit.COUNT, 100, facts -> facts.looted('T')),
	UMBER_100("Earth Tones", "Open 100 umber corpses.", RARE, Unit.COUNT, 100, facts -> facts.looted('U')),
	VANGUARD_CORPSE("Fallen Vanguard", "Open a vanguard corpse.", RARE, Unit.COUNT, 1, facts -> facts.looted('V')),
	VANGUARD_10("Honor Guard", "Open 10 vanguard corpses.", LEGENDARY, Unit.COUNT, 10, facts -> facts.looted('V')),
	FULL_HOUSE("Full House", "Open all four kinds of corpse in one shaft.", EPIC, Unit.COUNT, 1,
			facts -> facts.fullHouse() ? 1 : 0, true),
	PAYDAY("Payday", "Make 10M coins in one shaft.", RARE, Unit.COINS, 10_000_000, AchievementFacts::bestShaft),
	JACKPOT("Jackpot", "Make 50M coins in one shaft.", LEGENDARY, Unit.COINS, 50_000_000, AchievementFacts::bestShaft),
	BILLION("Billionaire", "Make 1B coins in mineshafts.", MYTHIC, Unit.COINS, 1_000_000_000, AchievementFacts::coins),
	SPEEDRUN("Speedrunner", "Finish a shaft route in under 90 seconds.", RARE, Unit.COUNT, 1,
			facts -> facts.fastestRun() > 0 && facts.fastestRun() < 90_000 ? 1 : 0);

	public static final String HIDDEN = "???";

	// how the progress is written
	public enum Unit {
		COUNT, COINS, HOURS
	}

	// current never goes past goal
	public record Progress(long current, long goal) {
		public boolean done() {
			return current >= goal;
		}

		public double fraction() {
			return goal <= 0 ? 1 : (double) current / goal;
		}
	}

	private static final class Gemstones {
		private static final int COUNT = (int) Arrays.stream(ShaftMaterial.values()).filter(ShaftMaterial::isGemstone).count();
	}

	private final String title;
	private final String description;
	private final AchievementRarity rarity;
	private final Unit unit;
	private final long goal;
	private final ToLongFunction<AchievementFacts> value;
	private final boolean hidden;

	Achievement(String title, String description, AchievementRarity rarity, Unit unit, long goal,
			ToLongFunction<AchievementFacts> value) {
		this(title, description, rarity, unit, goal, value, false);
	}

	Achievement(String title, String description, AchievementRarity rarity, Unit unit, long goal,
			ToLongFunction<AchievementFacts> value, boolean hidden) {
		this.title = title;
		this.description = description;
		this.rarity = rarity;
		this.unit = unit;
		this.goal = goal;
		this.value = value;
		this.hidden = hidden;
	}

	// a dry streak still going counts towards it, but only a jasper can finish it
	private static ToLongFunction<AchievementFacts> dry(long goal) {
		return facts -> Math.max(facts.longestDryEnded(), Math.min(facts.currentDry(), goal - 1));
	}

	public String title() {
		return title;
	}

	public String description() {
		return description;
	}

	// a hidden one says nothing about itself until it is unlocked
	public String title(boolean unlocked) {
		return hidden && !unlocked ? HIDDEN : title;
	}

	public String description(boolean unlocked) {
		return hidden && !unlocked ? HIDDEN : description;
	}

	public AchievementRarity rarity() {
		return rarity;
	}

	public boolean hidden() {
		return hidden;
	}

	public Progress progress(AchievementFacts facts) {
		return new Progress(Math.max(0, Math.min(value.applyAsLong(facts), goal)), goal);
	}

	public boolean earned(AchievementFacts facts) {
		return progress(facts).done();
	}

	// one-off achievements have no bar
	public boolean counts() {
		return goal > 1;
	}

	// 73/100, 250M/1.00B, 7/24 h
	public String progressText(Progress progress) {
		return switch (unit) {
			case COUNT -> number(progress.current()) + "/" + number(progress.goal());
			case COINS -> CoinText.amount(progress.current()) + "/" + CoinText.amount(progress.goal());
			case HOURS -> progress.current() / 3_600_000 + "/" + progress.goal() / 3_600_000 + " h";
		};
	}

	private static String number(long value) {
		return String.format(Locale.ROOT, "%,d", value);
	}
}
