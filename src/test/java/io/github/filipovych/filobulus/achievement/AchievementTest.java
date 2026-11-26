package io.github.filipovych.filobulus.achievement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.filipovych.filobulus.corpse.CorpseTally;
import io.github.filipovych.filobulus.journal.ShaftVisit;
import io.github.filipovych.filobulus.shaft.ShaftType;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AchievementTest {
	private static final long HOUR = 3_600_000;

	private static AchievementFacts facts(List<ShaftVisit> visits) {
		return AchievementFacts.of(visits, ZoneOffset.UTC);
	}

	// one shaft an hour apart, so a day holds 24 of them
	private static List<ShaftVisit> shafts(String... codes) {
		List<ShaftVisit> visits = new ArrayList<>();
		for (String code : codes) {
			visits.add(ShaftVisit.entered(visits.size() * HOUR + 1, code));
		}
		return visits;
	}

	private static String[] repeat(String code, int times) {
		String[] codes = new String[times];
		java.util.Arrays.fill(codes, code);
		return codes;
	}

	@Test
	void theFirstThreeStillComeFromTheJournal() {
		assertFalse(Achievement.FIRST_SHAFT.earned(facts(List.of())));
		AchievementFacts one = facts(shafts("RUBY_1"));
		assertTrue(Achievement.FIRST_SHAFT.earned(one));
		assertFalse(Achievement.JASPER.earned(one));
		assertTrue(Achievement.JASPER.earned(facts(shafts("JASP_C"))));
		ShaftVisit six = new ShaftVisit(1, "RUBY_1", 0, 0, new CorpseTally("LLLTTU", "LLLTTU"), 0, 0);
		ShaftVisit four = new ShaftVisit(2, "RUBY_1", 0, 0, new CorpseTally("LLTUV", "LLTU"), 0, 0);
		assertFalse(Achievement.CORPSES.earned(facts(List.of(six))));
		assertTrue(Achievement.CORPSES.earned(facts(List.of(six, four))));
	}

	@Test
	void idsAndTitlesAreUnique() {
		Set<String> titles = new HashSet<>();
		for (Achievement achievement : Achievement.values()) {
			assertTrue(titles.add(achievement.title()), achievement.title());
		}
		assertTrue(Achievement.values().length >= 20 && Achievement.values().length <= 30);
	}

	@Test
	void progressIsCappedAndWritten() {
		AchievementFacts facts = facts(shafts(repeat("RUBY_1", 73)));
		Achievement.Progress progress = Achievement.SHAFTS_100.progress(facts);
		assertEquals(73, progress.current());
		assertEquals("73/100", Achievement.SHAFTS_100.progressText(progress));
		assertEquals("10/10", Achievement.SHAFTS_10.progressText(Achievement.SHAFTS_10.progress(facts)));
		assertEquals("0/1,000", Achievement.SHAFTS_1000.progressText(new Achievement.Progress(0, 1000)));
		assertTrue(Achievement.DAY_10.earned(facts));
		assertFalse(Achievement.DAY_50.earned(facts));
		assertFalse(Achievement.FIRST_SHAFT.counts());

		ShaftVisit rich = new ShaftVisit(1, "JASP_1", 5 * HOUR, 80_000, CorpseTally.NONE, 12_000_000, 240_000_000);
		AchievementFacts paid = facts(List.of(rich));
		assertTrue(Achievement.PAYDAY.earned(paid));
		assertTrue(Achievement.JACKPOT.earned(paid));
		assertEquals("252M/1.00B", Achievement.BILLION.progressText(Achievement.BILLION.progress(paid)));
		assertEquals("5/24 h", Achievement.TIME_24H.progressText(Achievement.TIME_24H.progress(paid)));
		assertTrue(Achievement.SPEEDRUN.earned(paid));
		assertFalse(Achievement.SPEEDRUN.earned(facts(shafts("RUBY_1"))));
	}

	@Test
	void onlyAJasperEndsADryStreak() {
		AchievementFacts going = facts(shafts(repeat("RUBY_1", 60)));
		assertEquals(49, Achievement.DRY_50.progress(going).current());
		assertFalse(Achievement.DRY_50.earned(going));
		List<ShaftVisit> ended = shafts(repeat("RUBY_1", 60));
		ended.add(ShaftVisit.entered(100 * HOUR, "JASP_1"));
		assertTrue(Achievement.DRY_50.earned(facts(ended)));
		assertEquals(60, Achievement.DRY_100.progress(facts(ended)).current());
		assertFalse(Achievement.JASPER_TWICE.earned(facts(ended)));
		ended.add(ShaftVisit.entered(101 * HOUR, "JASP_C"));
		AchievementFacts twice = facts(ended);
		assertTrue(Achievement.JASPER_TWICE.earned(twice));
		assertTrue(Achievement.JASPER_CRYSTAL.earned(twice));
		assertTrue(Achievement.CRYSTAL.earned(twice));
	}

	@Test
	void shaftKindsAndCorpses() {
		List<String> gemstones = new ArrayList<>();
		for (ShaftType type : ShaftType.all()) {
			if (type.material().isGemstone() && !type.isCrystal()) {
				gemstones.add(type.code());
			}
		}
		AchievementFacts all = facts(shafts(gemstones.toArray(String[]::new)));
		assertEquals(12, Achievement.GEMSTONES.progress(all).goal());
		assertTrue(Achievement.GEMSTONES.earned(all));
		assertFalse(Achievement.GEMSTONES.earned(facts(shafts("RUBY_1", "JASP_1"))));
		assertTrue(Achievement.DEN.earned(facts(shafts("LITT_L"))));
		assertTrue(Achievement.VANGUARD_SHAFT.earned(facts(shafts("FAIR_1"))));

		ShaftVisit split = new ShaftVisit(1, "RUBY_1", 0, 0, new CorpseTally("LTU", "LTU"), 0, 0);
		ShaftVisit vanguard = new ShaftVisit(2, "RUBY_1", 0, 0, new CorpseTally("V", "V"), 0, 0);
		ShaftVisit full = new ShaftVisit(3, "RUBY_1", 0, 0, new CorpseTally("LTUVV", "LTUV"), 0, 0);
		assertFalse(Achievement.FULL_HOUSE.earned(facts(List.of(split, vanguard))));
		assertTrue(Achievement.VANGUARD_CORPSE.earned(facts(List.of(split, vanguard))));
		assertTrue(Achievement.FULL_HOUSE.earned(facts(List.of(full))));
	}

	@Test
	void hiddenOnesSayNothingUntilUnlocked() {
		assertTrue(Achievement.JASPER_TWICE.hidden());
		assertEquals("???", Achievement.JASPER_TWICE.title(false));
		assertEquals("???", Achievement.JASPER_TWICE.description(false));
		assertEquals("Lightning Strikes Twice", Achievement.JASPER_TWICE.title(true));
		assertEquals("Pink at Last", Achievement.JASPER.title(false));
	}

	@Test
	void theBookKeepsOldIdsAndHandsOutNewOnesRarestFirst() {
		AchievementBook book = AchievementBook.of(Map.of("first_shaft", 5L, "jasper", 6L, "gone_now", 7L));
		assertEquals(2, book.count());
		assertEquals(5L, book.at(Achievement.FIRST_SHAFT));
		assertNull(book.at(Achievement.CORPSES));
		assertEquals(Map.of("first_shaft", 5L, "jasper", 6L), book.saved());

		List<ShaftVisit> visits = shafts("JASP_1", "JASP_C");
		List<Achievement> fresh = book.update(facts(visits), 99);
		assertEquals(Achievement.JASPER_TWICE, fresh.getFirst());
		assertFalse(fresh.contains(Achievement.JASPER));
		assertEquals(99L, book.at(Achievement.JASPER_CRYSTAL));
		assertTrue(book.update(facts(visits), 100).isEmpty());
	}

	@Test
	void filters() {
		AchievementFilter filter = AchievementFilter.ALL;
		assertTrue(filter.shows(Achievement.JASPER, false));
		filter = filter.withStatus(AchievementFilter.Status.UNLOCKED);
		assertFalse(filter.shows(Achievement.JASPER, false));
		assertTrue(filter.shows(Achievement.JASPER, true));
		filter = filter.nextRarity(false);
		assertEquals(AchievementRarity.COMMON, filter.rarity());
		assertFalse(filter.shows(Achievement.JASPER, true));
		assertTrue(filter.shows(Achievement.FIRST_SHAFT, true));
		assertNull(filter.nextRarity(true).rarity());
		assertEquals(AchievementRarity.MYTHIC, AchievementFilter.ALL.nextRarity(true).rarity());
	}
}
