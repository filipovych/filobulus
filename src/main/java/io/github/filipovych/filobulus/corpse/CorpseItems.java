package io.github.filipovych.filobulus.corpse;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

// turns the reward names of the loot block into bazaar product ids, all the item names we know are here
public final class CorpseItems {
	private static final List<String> GEMS = List.of("jasper", "ruby", "amber", "topaz", "jade", "sapphire",
			"amethyst", "opal", "aquamarine", "citrine", "onyx", "peridot");
	private static final List<String> TIERS = List.of("rough", "flawed", "fine", "flawless", "perfect");

	private static final Map<String, String> TABLE = buildTable();

	private CorpseItems() {
	}

	private static Map<String, String> buildTable() {
		Map<String, String> table = new HashMap<>();
		table.put("tungsten key", "TUNGSTEN_KEY");
		table.put("umber key", "UMBER_KEY");
		table.put("skeleton key", "SKELETON_KEY");
		table.put("tungsten plate", "TUNGSTEN_PLATE");
		table.put("umber plate", "UMBER_PLATE");
		table.put("mithril plate", "MITHRIL_PLATE");
		table.put("refined tungsten", "REFINED_TUNGSTEN");
		table.put("refined umber", "REFINED_UMBER");
		table.put("enchanted glacite", "ENCHANTED_GLACITE");
		table.put("enchanted tungsten", "ENCHANTED_TUNGSTEN");
		table.put("enchanted umber", "ENCHANTED_UMBER");
		table.put("glacite amalgamation", "GLACITE_AMALGAMATION");
		table.put("glacite jewel", "GLACITE_JEWEL");
		table.put("bejeweled handle", "BEJEWELED_HANDLE");
		table.put("suspicious scrap", "SUSPICIOUS_SCRAP");
		table.put("frozen scute", "FROZEN_SCUTE");
		table.put("dwarven o's metallic minis", "DWARVEN_OS_METALLIC_MINIS");
		table.put("goblin egg", "GOBLIN_EGG");
		table.put("green goblin egg", "GOBLIN_EGG_GREEN");
		table.put("yellow goblin egg", "GOBLIN_EGG_YELLOW");
		table.put("red goblin egg", "GOBLIN_EGG_RED");
		table.put("blue goblin egg", "GOBLIN_EGG_BLUE");
		// the level has been seen written both ways
		table.put("enchanted book (ice cold i)", "ENCHANTMENT_ICE_COLD_1");
		table.put("enchanted book (ice cold 1)", "ENCHANTMENT_ICE_COLD_1");
		return Map.copyOf(table);
	}

	// the bazaar product id, empty when the reward has no bazaar price
	public static Optional<String> id(String rewardName) {
		String name = plain(rewardName);
		String id = TABLE.get(name);
		if (id != null) {
			return Optional.of(id);
		}
		// Fine Onyx Gemstone is FINE_ONYX_GEM
		String[] words = name.split(" ");
		if (words.length == 3 && words[2].equals("gemstone") && TIERS.contains(words[0]) && GEMS.contains(words[1])) {
			return Optional.of(gemId(words[0], words[1]));
		}
		return Optional.empty();
	}

	private static String gemId(String tier, String gem) {
		return (tier + "_" + gem + "_GEM").toUpperCase(Locale.ROOT);
	}

	private static String plain(String name) {
		return name == null ? "" : name.strip().toLowerCase(Locale.ROOT).replace('’', '\'');
	}
}
