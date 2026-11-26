package io.github.filipovych.filobulus.mining;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

// skyblock blocks with their block strength, found by the minecraft block they look like
// the gemstones changed on the alpha server in november 2026, both sets are kept until the change reaches everyone
public enum BlockStrength {
	RUBY("Ruby", 2300, 2500),
	AMBER("Amber", 3000, 3000),
	SAPPHIRE("Sapphire", 3000, 3000),
	JADE("Jade", 3000, 3000),
	AMETHYST("Amethyst", 3000, 3000),
	OPAL("Opal", 3000, 3000),
	TOPAZ("Topaz", 3800, 3500),
	JASPER("Jasper", 4800, 4000),
	ONYX("Onyx", 5200, 4000),
	AQUAMARINE("Aquamarine", 5200, 4000),
	CITRINE("Citrine", 5200, 4000),
	PERIDOT("Peridot", 5200, 4000),
	HARD_STONE("Hard Stone", 50, 50),
	MITHRIL_GRAY("Mithril", 500, 500),
	MITHRIL_GREEN("Mithril", 800, 800),
	MITHRIL_BLUE("Mithril", 1500, 1500),
	TITANIUM("Titanium", 2000, 2000),
	UMBER("Umber", 5600, 5600),
	TUNGSTEN("Tungsten", 5600, 5600),
	GLACITE("Glacite", 6000, 6000);

	// minecraft block ids, without minecraft:, glass and its pane are the same gemstone
	private static final Map<String, BlockStrength> BY_BLOCK = Map.ofEntries(
			Map.entry("red_stained_glass", RUBY), Map.entry("orange_stained_glass", AMBER),
			Map.entry("light_blue_stained_glass", SAPPHIRE), Map.entry("lime_stained_glass", JADE),
			Map.entry("purple_stained_glass", AMETHYST), Map.entry("white_stained_glass", OPAL),
			Map.entry("yellow_stained_glass", TOPAZ), Map.entry("magenta_stained_glass", JASPER),
			Map.entry("black_stained_glass", ONYX), Map.entry("blue_stained_glass", AQUAMARINE),
			Map.entry("brown_stained_glass", CITRINE), Map.entry("green_stained_glass", PERIDOT),
			Map.entry("stone", HARD_STONE), Map.entry("gray_wool", MITHRIL_GRAY), Map.entry("cyan_terracotta", MITHRIL_GRAY),
			Map.entry("prismarine", MITHRIL_GREEN), Map.entry("prismarine_bricks", MITHRIL_GREEN),
			Map.entry("dark_prismarine", MITHRIL_GREEN), Map.entry("light_blue_wool", MITHRIL_BLUE),
			Map.entry("polished_diorite", TITANIUM), Map.entry("terracotta", UMBER), Map.entry("brown_terracotta", UMBER),
			Map.entry("smooth_red_sandstone", UMBER), Map.entry("clay", TUNGSTEN), Map.entry("infested_cobblestone", TUNGSTEN),
			Map.entry("packed_ice", GLACITE));

	private final String title;
	private final int live;
	private final int alpha;

	BlockStrength(String title, int live, int alpha) {
		this.title = title;
		this.live = live;
		this.alpha = alpha;
	}

	public String title() {
		return title;
	}

	public int strength(boolean alphaValues) {
		return alphaValues ? alpha : live;
	}

	// hard stone breaks in one tick at half the speed of an ore
	public boolean plain() {
		return this == HARD_STONE;
	}

	public static Optional<BlockStrength> ofBlock(String blockId) {
		String id = blockId.startsWith("minecraft:") ? blockId.substring("minecraft:".length()) : blockId;
		return Optional.ofNullable(BY_BLOCK.get(id.replace("_pane", "")));
	}

	// the gemstone of a shaft, by its material name like JASPER
	public static Optional<BlockStrength> ofGem(String material) {
		try {
			BlockStrength strength = valueOf(material.toUpperCase(Locale.ROOT));
			return strength.ordinal() <= PERIDOT.ordinal() ? Optional.of(strength) : Optional.empty();
		} catch (IllegalArgumentException e) {
			return Optional.empty();
		}
	}
}
