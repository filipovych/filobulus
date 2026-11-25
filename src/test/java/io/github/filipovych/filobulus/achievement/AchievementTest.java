package io.github.filipovych.filobulus.achievement;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.filipovych.filobulus.corpse.CorpseTally;
import io.github.filipovych.filobulus.journal.ShaftVisit;
import java.util.List;
import org.junit.jupiter.api.Test;

class AchievementTest {
	@Test
	void achievementsComeFromTheJournal() {
		assertFalse(Achievement.FIRST_SHAFT.earned(List.of()));
		List<ShaftVisit> one = List.of(ShaftVisit.entered(1, "RUBY_1"));
		assertTrue(Achievement.FIRST_SHAFT.earned(one));
		assertFalse(Achievement.JASPER.earned(one));
		assertTrue(Achievement.JASPER.earned(List.of(ShaftVisit.entered(1, "JASP_C"))));
		ShaftVisit six = new ShaftVisit(1, "RUBY_1", 0, 0, new CorpseTally("LLLTTU", "LLLTTU"), 0, 0);
		ShaftVisit four = new ShaftVisit(2, "RUBY_1", 0, 0, new CorpseTally("LLTUV", "LLTU"), 0, 0);
		assertFalse(Achievement.CORPSES.earned(List.of(six)));
		assertTrue(Achievement.CORPSES.earned(List.of(six, four)));
	}
}
