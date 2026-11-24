package io.github.filipovych.filobulus.corpse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.filipovych.filobulus.corpse.CorpseLoot.Reward;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CorpseLootReaderTest {
	private static final String BAR = "▬".repeat(64);

	// feeds the lines in order, every line but the last must give nothing
	private static Optional<CorpseLoot> feed(CorpseLootReader reader, String... lines) {
		Optional<CorpseLoot> last = Optional.empty();
		for (int i = 0; i < lines.length; i++) {
			last = reader.read(lines[i]);
			if (i < lines.length - 1) {
				assertEquals(Optional.empty(), last, lines[i]);
			}
		}
		return last;
	}

	@Test
	void readsATungstenBlockWithIconsAndAmounts() {
		Optional<CorpseLoot> loot = feed(new CorpseLootReader(),
				BAR,
				"  TUNGSTEN CORPSE LOOT! ",
				"    \uE007 Fine Onyx Gemstone x8",
				"    ☠ Flawless Peridot Gemstone",
				"    Glacite Powder x7,412",
				"    Enchanted Book (Ice Cold I)",
				"    Suspicious Scrap x4",
				"    +1,000 HOTM Experience",
				BAR);
		assertEquals(Optional.of(new CorpseLoot(CorpseType.TUNGSTEN, List.of(
				new Reward("Fine Onyx Gemstone", 8),
				new Reward("Flawless Peridot Gemstone", 1),
				new Reward("Glacite Powder", 7412),
				new Reward("Enchanted Book (Ice Cold I)", 1),
				new Reward("Suspicious Scrap", 4),
				new Reward("HOTM Experience", 1000)))), loot);
	}

	@Test
	void readsALapisBlock() {
		Optional<CorpseLoot> loot = feed(new CorpseLootReader(),
				"  LAPIS CORPSE LOOT!",
				"    \uE054 Flawed Citrine Gemstone x40",
				"    Green Goblin Egg x2",
				"    Glacite Powder x1,204",
				"    +100 HOTM Experience",
				BAR);
		assertEquals(Optional.of(new CorpseLoot(CorpseType.LAPIS, List.of(
				new Reward("Flawed Citrine Gemstone", 40),
				new Reward("Green Goblin Egg", 2),
				new Reward("Glacite Powder", 1204),
				new Reward("HOTM Experience", 100)))), loot);
	}

	@Test
	void aNewHeaderStartsOver() {
		Optional<CorpseLoot> loot = feed(new CorpseLootReader(),
				"  UMBER CORPSE LOOT!",
				"    Umber Plate",
				"  VANGUARD CORPSE LOOT!",
				"    Skeleton Key",
				BAR);
		assertEquals(Optional.of(new CorpseLoot(CorpseType.VANGUARD, List.of(new Reward("Skeleton Key", 1)))), loot);
	}

	@Test
	void otherLinesInsideABlockAreIgnored() {
		Optional<CorpseLoot> loot = feed(new CorpseLootReader(),
				"  UMBER CORPSE LOOT!",
				"",
				"  REWARDS",
				"[MVP+] Someone: anyone got a spare key x2",
				"     five spaces is not a reward",
				"   three neither",
				"    Refined Umber",
				"  MITHRIL CORPSE LOOT!",
				BAR);
		assertEquals(Optional.of(new CorpseLoot(CorpseType.UMBER, List.of(new Reward("Refined Umber", 1)))), loot);
	}

	@Test
	void nothingIsReadOutsideABlock() {
		CorpseLootReader reader = new CorpseLootReader();
		assertEquals(Optional.empty(), feed(reader, BAR, "    Umber Plate", BAR, null));
		assertEquals(Optional.empty(), feed(reader, " TUNGSTEN CORPSE LOOT!", "    Umber Plate", BAR));
		assertEquals(Optional.empty(), feed(reader, "  TUNGSTEN CORPSE LOOT", "    Umber Plate", BAR));
	}

	@Test
	void aBlockIsOnlyGivenOnce() {
		CorpseLootReader reader = new CorpseLootReader();
		assertTrue(feed(reader, "  UMBER CORPSE LOOT!", "    Umber Plate", BAR).isPresent());
		assertEquals(Optional.empty(), reader.read(BAR));
		assertEquals(Optional.empty(), feed(reader, "    Umber Plate", BAR));
	}

	@Test
	void aShortRowOfBarsOrABlockWithoutRewardsGivesNothing() {
		CorpseLootReader reader = new CorpseLootReader();
		assertEquals(Optional.empty(), feed(reader, "  UMBER CORPSE LOOT!", "    Umber Plate", "▬▬▬", "▬".repeat(20) + " x"));
		assertTrue(reader.read("▬".repeat(10)).isPresent());
		assertEquals(Optional.empty(), feed(reader, "  UMBER CORPSE LOOT!", BAR));
	}

	@Test
	void aBlockThatNeverClosesIsDropped() {
		CorpseLootReader reader = new CorpseLootReader();
		reader.read("  UMBER CORPSE LOOT!");
		for (int i = 0; i < 40; i++) {
			reader.read("    Umber Plate");
		}
		assertTrue(reader.read(BAR).isPresent());

		reader.read("  UMBER CORPSE LOOT!");
		for (int i = 0; i < 41; i++) {
			reader.read("    Umber Plate");
		}
		assertEquals(Optional.empty(), reader.read(BAR));
	}

	@Test
	void resetForgetsAHalfReadBlock() {
		CorpseLootReader reader = new CorpseLootReader();
		feed(reader, "  UMBER CORPSE LOOT!", "    Umber Plate");
		reader.reset();
		assertEquals(Optional.empty(), reader.read(BAR));
	}

	@Test
	void anAmountNeedsItsOwnWord() {
		Optional<CorpseLoot> loot = feed(new CorpseLootReader(),
				"  UMBER CORPSE LOOT!",
				"    Box x",
				"    Boxx3",
				"    Onyx Crystal  ",
				BAR);
		assertEquals(List.of(new Reward("Box x", 1), new Reward("Boxx3", 1), new Reward("Onyx Crystal", 1)),
				loot.orElseThrow().rewards());
	}
}
