package io.github.filipovych.filobulus.profit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.filipovych.filobulus.profit.SackReader.Change;
import java.util.List;
import org.junit.jupiter.api.Test;

// made up lines in the shape other mods read them, until there is a capture of a real one
class SackReaderTest {
	@Test
	void aSackLineIsKnownByItsStart() {
		assertTrue(SackReader.isSackLine("[Sacks] +1,034 items. (Last 5s.)"));
		assertTrue(SackReader.isSackLine("  [Sacks] +3 items, -2 items. (Last 30s.)"));
		assertFalse(SackReader.isSackLine("Someone: [Sacks] +3 items"));
		assertFalse(SackReader.isSackLine(null));
	}

	@Test
	void theHoverTextGivesOneChangePerItem() {
		String hover = "Added items:\n"
				+ "  +1,024 Flawed Jasper Gemstone (Gemstones Sack)\n"
				+ "  +10 Glacite (Mining Sack)\n"
				+ "\n"
				+ "This message can be disabled in the settings.";
		assertEquals(List.of(new Change("Flawed Jasper Gemstone", 1024), new Change("Glacite", 10)), SackReader.read(hover));
	}

	@Test
	void whatWasTakenOutIsNegative() {
		assertEquals(List.of(new Change("Enchanted Mithril", -64)),
				SackReader.read("Removed items:\n  -64 Enchanted Mithril (Mining Sack)"));
	}

	@Test
	void anIconInFrontOfTheNameGoes() {
		assertEquals(List.of(new Change("Rough Amber Gemstone", 3)),
				SackReader.read("  +3 ⸕ Rough Amber Gemstone (Gemstones Sack)"));
	}

	@Test
	void linesThatAreNoItemAreSkipped() {
		assertTrue(SackReader.read("Added items:\n  and 3 more...\n  +0 Coal (Mining Sack)\n  +x Coal (Mining Sack)").isEmpty());
		assertTrue(SackReader.read(null).isEmpty());
	}

	@Test
	void theHoverSaysHowLongItAddsUp() {
		assertEquals(30_000, SackReader.windowMillis("Added items:\n  +64 Rough Ruby Gemstone (Gemstones Sack)\n\n(Last 30s.)"));
		assertEquals(0, SackReader.windowMillis("Added items:"));
	}
}
