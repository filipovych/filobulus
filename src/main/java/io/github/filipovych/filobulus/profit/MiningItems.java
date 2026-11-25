package io.github.filipovych.filobulus.profit;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

// what mining gives: the names the sacks use, turned into skyblock item ids, which are the bazaar's product ids too
public final class MiningItems {
	private static final List<String> GEMS = List.of("jasper", "ruby", "amber", "topaz", "jade", "sapphire",
			"amethyst", "opal", "aquamarine", "citrine", "onyx", "peridot");
	private static final List<String> TIERS = List.of("rough", "flawed", "fine", "flawless", "perfect");
	private static final Map<String, String> TABLE = buildTable();
	private static final Map<String, String> NAMES = buildNames();

	private MiningItems() {
	}

	private static Map<String, String> buildTable() {
		Map<String, String> table = new HashMap<>();
		table.put("mithril", "MITHRIL_ORE");
		table.put("titanium", "TITANIUM_ORE");
		table.put("glacite", "GLACITE");
		table.put("umber", "UMBER");
		table.put("tungsten", "TUNGSTEN");
		table.put("hard stone", "HARD_STONE");
		table.put("cobblestone", "COBBLESTONE");
		table.put("coal", "COAL");
		table.put("iron ingot", "IRON_INGOT");
		table.put("gold ingot", "GOLD_INGOT");
		table.put("diamond", "DIAMOND");
		table.put("emerald", "EMERALD");
		table.put("redstone", "REDSTONE");
		table.put("lapis lazuli", "INK_SACK:4");
		table.put("nether quartz", "QUARTZ");
		table.put("obsidian", "OBSIDIAN");
		table.put("glowstone dust", "GLOWSTONE_DUST");
		table.put("gravel", "GRAVEL");
		table.put("flint", "FLINT");
		table.put("sand", "SAND");
		table.put("end stone", "ENDER_STONE");
		table.put("netherrack", "NETHERRACK");
		table.put("ice", "ICE");
		table.put("sulphur", "SULPHUR_ORE");
		table.put("starfall", "STARFALL");
		table.put("treasurite", "TREASURITE");
		table.put("sludge juice", "SLUDGE_JUICE");
		table.put("oil barrel", "OIL_BARREL");
		table.put("glacite jewel", "GLACITE_JEWEL");
		// the enchanted forms, for players whose compactor makes them as they mine
		for (String name : List.of("mithril", "titanium", "glacite", "umber", "tungsten", "hard stone", "cobblestone",
				"coal", "diamond", "emerald", "redstone", "obsidian", "flint", "sand", "ice", "netherrack")) {
			table.put("enchanted " + name, "ENCHANTED_" + name.toUpperCase(Locale.ROOT).replace(' ', '_'));
		}
		table.put("enchanted iron", "ENCHANTED_IRON");
		table.put("enchanted gold", "ENCHANTED_GOLD");
		table.put("enchanted lapis lazuli", "ENCHANTED_LAPIS_LAZULI");
		table.put("enchanted quartz", "ENCHANTED_QUARTZ");
		table.put("enchanted glowstone dust", "ENCHANTED_GLOWSTONE_DUST");
		table.put("enchanted end stone", "ENCHANTED_ENDSTONE");
		table.put("enchanted sulphur", "ENCHANTED_SULPHUR");
		return Map.copyOf(table);
	}

	private static Map<String, String> buildNames() {
		Map<String, String> names = new HashMap<>();
		TABLE.forEach((name, id) -> names.put(id, title(name)));
		return Map.copyOf(names);
	}

	// the id of something named the way the sacks name it, empty when it is nothing we know as mining loot
	public static Optional<String> id(String name) {
		String plain = name == null ? "" : name.strip().toLowerCase(Locale.ROOT);
		String id = TABLE.get(plain);
		if (id != null) {
			return Optional.of(id);
		}
		// Flawed Jasper Gemstone is FLAWED_JASPER_GEM
		String[] words = plain.split(" ");
		if (words.length == 3 && words[2].equals("gemstone") && TIERS.contains(words[0]) && GEMS.contains(words[1])) {
			return Optional.of((words[0] + "_" + words[1] + "_GEM").toUpperCase(Locale.ROOT));
		}
		return Optional.empty();
	}

	public static boolean isMining(String id) {
		return NAMES.containsKey(id) || gem(id).isPresent();
	}

	// the gemstone of an id like FLAWED_JASPER_GEM, lower case
	public static Optional<String> gem(String id) {
		String[] parts = id.toLowerCase(Locale.ROOT).split("_");
		return parts.length == 3 && parts[2].equals("gem") && TIERS.contains(parts[0]) && GEMS.contains(parts[1])
				? Optional.of(parts[1])
				: Optional.empty();
	}

	// rough, flawed and so on, empty for anything that is not a gemstone
	public static Optional<String> tier(String id) {
		return gem(id).map(gem -> id.toLowerCase(Locale.ROOT).split("_")[0]);
	}

	// a name to show for an id when the game never told us one: Flawed Jasper, Enchanted Mithril
	public static String label(String id) {
		String known = NAMES.get(id);
		if (known != null) {
			return known;
		}
		Optional<String> gem = gem(id);
		if (gem.isPresent()) {
			return title(tier(id).orElse("") + " " + gem.get());
		}
		return title(id.toLowerCase(Locale.ROOT).replace('_', ' ').replace(':', ' '));
	}

	private static String title(String words) {
		StringBuilder text = new StringBuilder();
		for (String word : words.strip().split(" +")) {
			if (!word.isEmpty()) {
				text.append(text.isEmpty() ? "" : " ").append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
			}
		}
		return text.toString();
	}
}
