package io.github.filipovych.filobulus.profit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class MiningItemsTest {
	@Test
	void gemstonesFollowOneRule() {
		assertEquals(Optional.of("FLAWED_JASPER_GEM"), MiningItems.id("Flawed Jasper Gemstone"));
		assertEquals(Optional.of("ROUGH_AQUAMARINE_GEM"), MiningItems.id(" rough aquamarine gemstone "));
		assertEquals(Optional.empty(), MiningItems.id("Shiny Jasper Gemstone"));
		assertEquals(Optional.empty(), MiningItems.id("Flawed Diamond Gemstone"));
	}

	@Test
	void theOresAndTheirEnchantedFormsAreInTheTable() {
		assertEquals(Optional.of("MITHRIL_ORE"), MiningItems.id("Mithril"));
		assertEquals(Optional.of("ENCHANTED_MITHRIL"), MiningItems.id("Enchanted Mithril"));
		assertEquals(Optional.of("ENCHANTED_HARD_STONE"), MiningItems.id("Enchanted Hard Stone"));
		assertEquals(Optional.of("INK_SACK:4"), MiningItems.id("Lapis Lazuli"));
		assertEquals(Optional.empty(), MiningItems.id("Wheat"));
		assertEquals(Optional.empty(), MiningItems.id(null));
	}

	@Test
	void onlyMiningLootCountsAsMining() {
		assertTrue(MiningItems.isMining("FINE_RUBY_GEM"));
		assertTrue(MiningItems.isMining("GLACITE"));
		assertTrue(MiningItems.isMining("ENCHANTED_TUNGSTEN"));
		assertFalse(MiningItems.isMining("WHEAT"));
		assertFalse(MiningItems.isMining("TUNGSTEN_KEY"));
		assertFalse(MiningItems.isMining("FINE_RUBY_GEMS"));
	}

	@Test
	void anIdGetsANameToShow() {
		assertEquals("Flawed Jasper", MiningItems.label("FLAWED_JASPER_GEM"));
		assertEquals("Enchanted Mithril", MiningItems.label("ENCHANTED_MITHRIL"));
		assertEquals("Lapis Lazuli", MiningItems.label("INK_SACK:4"));
		assertEquals("Some New Thing", MiningItems.label("SOME_NEW_THING"));
		assertEquals(Optional.of("flawed"), MiningItems.tier("FLAWED_JASPER_GEM"));
		assertEquals(Optional.empty(), MiningItems.tier("GLACITE"));
	}
}
