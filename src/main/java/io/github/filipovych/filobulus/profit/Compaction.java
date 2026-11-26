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
		recipes.put("ENCHANTED_HARD_STONE", new Recipe("HARD_STONE", 576));
		recipes.put("CONCENTRATED_STONE", new Recipe("ENCHANTED_HARD_STONE", 576));
		return Map.copyOf(recipes);
	}

	public static Optional<Recipe> of(String output) {
		return Optional.ofNullable(RECIPES.get(output));
	}
}
