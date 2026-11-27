package io.github.filipovych.filobulus.profit;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// what a compactor or the crafting grid turns mining items into, one input each, from the recipes of the NEU item repo
// a gain of the output is the input going away, not new loot
public final class Compaction {
	public record Recipe(String input, int count) {
	}

	private static final List<String> GEMS = List.of("JASPER", "RUBY", "AMBER", "TOPAZ", "JADE", "SAPPHIRE", "AMETHYST",
			"OPAL", "ONYX", "AQUAMARINE", "CITRINE", "PERIDOT");
	private static final Map<String, Recipe> RECIPES = build();

	private Compaction() {
	}

	private static Map<String, Recipe> build() {
		Map<String, Recipe> recipes = new HashMap<>();
		for (String gem : GEMS) {
			recipes.put("FLAWED_" + gem + "_GEM", new Recipe("ROUGH_" + gem + "_GEM", 80));
			recipes.put("FINE_" + gem + "_GEM", new Recipe("FLAWED_" + gem + "_GEM", 80));
			recipes.put("FLAWLESS_" + gem + "_GEM", new Recipe("FINE_" + gem + "_GEM", 80));
		}
		recipes.put("ENCHANTED_MITHRIL", new Recipe("MITHRIL_ORE", 160));
		recipes.put("ENCHANTED_GLACITE", new Recipe("GLACITE", 160));
		recipes.put("ENCHANTED_UMBER", new Recipe("UMBER", 160));
		recipes.put("ENCHANTED_TUNGSTEN", new Recipe("TUNGSTEN", 160));
		recipes.put("ENCHANTED_TITANIUM", new Recipe("TITANIUM_ORE", 160));
		recipes.put("ENCHANTED_COBBLESTONE", new Recipe("COBBLESTONE", 160));
		recipes.put("ENCHANTED_COAL", new Recipe("COAL", 160));
		recipes.put("ENCHANTED_IRON", new Recipe("IRON_INGOT", 160));
		recipes.put("ENCHANTED_GOLD", new Recipe("GOLD_INGOT", 160));
		recipes.put("ENCHANTED_DIAMOND", new Recipe("DIAMOND", 160));
		recipes.put("ENCHANTED_EMERALD", new Recipe("EMERALD", 160));
		recipes.put("ENCHANTED_REDSTONE", new Recipe("REDSTONE", 160));
		recipes.put("ENCHANTED_LAPIS_LAZULI", new Recipe("INK_SACK:4", 160));
		recipes.put("ENCHANTED_QUARTZ", new Recipe("QUARTZ", 160));
		recipes.put("ENCHANTED_OBSIDIAN", new Recipe("OBSIDIAN", 160));
		recipes.put("ENCHANTED_GLOWSTONE_DUST", new Recipe("GLOWSTONE_DUST", 192));
		recipes.put("ENCHANTED_FLINT", new Recipe("FLINT", 160));
		recipes.put("ENCHANTED_SAND", new Recipe("SAND", 160));
		recipes.put("ENCHANTED_ENDSTONE", new Recipe("ENDER_STONE", 160));
		recipes.put("ENCHANTED_NETHERRACK", new Recipe("NETHERRACK", 160));
		recipes.put("ENCHANTED_ICE", new Recipe("ICE", 160));
		recipes.put("ENCHANTED_SULPHUR", new Recipe("SULPHUR_ORE", 160));
		recipes.put("ENCHANTED_HARD_STONE", new Recipe("HARD_STONE", 576));
		recipes.put("CONCENTRATED_STONE", new Recipe("ENCHANTED_HARD_STONE", 576));
		return Map.copyOf(recipes);
	}

	public static Optional<Recipe> of(String output) {
		return Optional.ofNullable(RECIPES.get(output));
	}
}
