package io.github.filipovych.filobulus.corpse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class CorpseItemsTest {
	@Test
	void gemstonesFollowOneRule() {
		assertEquals(Optional.of("FINE_ONYX_GEM"), CorpseItems.id("Fine Onyx Gemstone"));
		assertEquals(Optional.of("FLAWED_PERIDOT_GEM"), CorpseItems.id("Flawed Peridot Gemstone"));
		assertEquals(Optional.of("FLAWLESS_AQUAMARINE_GEM"), CorpseItems.id("Flawless Aquamarine Gemstone"));
		assertEquals(Optional.of("ROUGH_JASPER_GEM"), CorpseItems.id("Rough Jasper Gemstone"));
		assertEquals(Optional.of("PERFECT_CITRINE_GEM"), CorpseItems.id("Perfect Citrine Gemstone"));
		assertEquals(Optional.empty(), CorpseItems.id("Shiny Onyx Gemstone"));
		assertEquals(Optional.empty(), CorpseItems.id("Fine Glacite Gemstone"));
		assertEquals(Optional.empty(), CorpseItems.id("Fine Onyx Gemstone Gauntlet"));
	}

	@Test
	void theRestComesFromTheTable() {
		assertEquals(Optional.of("TUNGSTEN_KEY"), CorpseItems.id("Tungsten Key"));
		assertEquals(Optional.of("UMBER_KEY"), CorpseItems.id("Umber Key"));
		assertEquals(Optional.of("SKELETON_KEY"), CorpseItems.id("Skeleton Key"));
		assertEquals(Optional.of("TUNGSTEN_PLATE"), CorpseItems.id("Tungsten Plate"));
		assertEquals(Optional.of("UMBER_PLATE"), CorpseItems.id("Umber Plate"));
		assertEquals(Optional.of("MITHRIL_PLATE"), CorpseItems.id("Mithril Plate"));
		assertEquals(Optional.of("REFINED_TUNGSTEN"), CorpseItems.id("Refined Tungsten"));
		assertEquals(Optional.of("REFINED_UMBER"), CorpseItems.id("Refined Umber"));
		assertEquals(Optional.of("ENCHANTED_GLACITE"), CorpseItems.id("Enchanted Glacite"));
		assertEquals(Optional.of("ENCHANTED_TUNGSTEN"), CorpseItems.id("Enchanted Tungsten"));
		assertEquals(Optional.of("ENCHANTED_UMBER"), CorpseItems.id("Enchanted Umber"));
		assertEquals(Optional.of("GLACITE_AMALGAMATION"), CorpseItems.id("Glacite Amalgamation"));
		assertEquals(Optional.of("GLACITE_JEWEL"), CorpseItems.id("Glacite Jewel"));
		assertEquals(Optional.of("BEJEWELED_HANDLE"), CorpseItems.id("Bejeweled Handle"));
		assertEquals(Optional.of("SUSPICIOUS_SCRAP"), CorpseItems.id("Suspicious Scrap"));
		assertEquals(Optional.of("GOBLIN_EGG"), CorpseItems.id("Goblin Egg"));
		assertEquals(Optional.of("GOBLIN_EGG_GREEN"), CorpseItems.id("Green Goblin Egg"));
		assertEquals(Optional.of("GOBLIN_EGG_YELLOW"), CorpseItems.id("Yellow Goblin Egg"));
		assertEquals(Optional.of("GOBLIN_EGG_RED"), CorpseItems.id("Red Goblin Egg"));
		assertEquals(Optional.of("GOBLIN_EGG_BLUE"), CorpseItems.id("Blue Goblin Egg"));
		assertEquals(Optional.of("DWARVEN_OS_METALLIC_MINIS"), CorpseItems.id("Dwarven O's Metallic Minis"));
		assertEquals(Optional.of("ENCHANTMENT_ICE_COLD_1"), CorpseItems.id("Enchanted Book (Ice Cold I)"));
		assertEquals(Optional.of("ENCHANTMENT_ICE_COLD_1"), CorpseItems.id("Enchanted Book (Ice Cold 1)"));
	}

	@Test
	void matchingIgnoresCase() {
		assertEquals(Optional.of("FROZEN_SCUTE"), CorpseItems.id("FROZEN SCUTE"));
		assertEquals(Optional.of("FINE_ONYX_GEM"), CorpseItems.id(" fine onyx gemstone "));
		assertTrue(CorpseItems.rare("shattered locket"));
		assertTrue(CorpseItems.worthless("GLACITE POWDER"));
	}

	@Test
	void aCurlyApostropheIsReadLikeAStraightOne() {
		assertEquals(Optional.of("DWARVEN_OS_METALLIC_MINIS"), CorpseItems.id("Dwarven O\u2019s Metallic Minis"));
		assertTrue(CorpseItems.worthless("Dead Man\u2019s Chestplate"));
	}

	@Test
	void theRareOnesHaveNoBazaarId() {
		for (String name : new String[] {"Shattered Locket", "Frostbitten Dye", "Caged Wisp"}) {
			assertTrue(CorpseItems.rare(name), name);
			assertEquals(Optional.empty(), CorpseItems.id(name), name);
			assertFalse(CorpseItems.worthless(name), name);
		}
		assertFalse(CorpseItems.rare("Skeleton Key"));
		assertFalse(CorpseItems.rare("Umber Plate"));
	}

	@Test
	void knowsWhatCountsAsNothing() {
		for (String name : new String[] {"Glacite Powder", "HOTM Experience", "+1,000 HOTM Experience", "Onyx Crystal",
				"Opal Crystal", "Jasper Crystal", "Dead Man's Chestplate"}) {
			assertTrue(CorpseItems.worthless(name), name);
			assertEquals(Optional.empty(), CorpseItems.id(name), name);
			assertFalse(CorpseItems.rare(name), name);
		}
		assertFalse(CorpseItems.worthless("Fine Onyx Gemstone"));
		assertFalse(CorpseItems.worthless("Glacite Crystal"));
	}

	@Test
	void anUnknownNameIsNoneOfThem() {
		for (String name : new String[] {"Mystery Thing", "", null}) {
			assertEquals(Optional.empty(), CorpseItems.id(name));
			assertFalse(CorpseItems.rare(name));
			assertFalse(CorpseItems.worthless(name));
		}
	}
}
