package io.github.filipovych.filobulus.client.hud;

import io.github.filipovych.filobulus.shaft.ShaftMaterial;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

// the items a player can put on a route of their own, a key is a gemstone's name or a vanilla item id
public final class RouteIcons {
	// the stacks are never changed, so one per item is enough
	private static final Map<String, ItemStack> STACKS = new HashMap<>();
	public static final String DEFAULT = "compass";

	private static final List<String> VANILLA = List.of(
			"coal", "raw_iron", "raw_gold", "diamond", "emerald", "redstone", "lapis_lazuli", "quartz", "obsidian",
			"glowstone_dust", "prismarine_crystals", "stone", "cobblestone", "sand", "gravel", "netherrack", "end_stone",
			"ice", "diamond_pickaxe", "golden_pickaxe", "tnt", "minecart", "torch", "chest", "nether_star", "clock",
			DEFAULT);
	// what a word in a route's name most likely means
	private static final List<String[]> WORDS = List.of(
			new String[] {"mithril", "prismarine_crystals"}, new String[] {"hard", "stone"}, new String[] {"gold", "raw_gold"},
			new String[] {"iron", "raw_iron"}, new String[] {"lapis", "lapis_lazuli"}, new String[] {"glowstone", "glowstone_dust"},
			new String[] {"powder", "chest"}, new String[] {"nuc", "nether_star"}, new String[] {"glacite", "littlefoot"});

	private RouteIcons() {
	}

	// every key, gemstones first
	public static List<String> keys() {
		List<String> keys = new ArrayList<>();
		for (ShaftMaterial material : ShaftMaterial.values()) {
			if (material != ShaftMaterial.VANGUARD) {
				keys.add(material.name().toLowerCase(Locale.ROOT));
			}
		}
		keys.addAll(VANILLA);
		return keys;
	}

	// an unknown key shows the default, so a file from a newer version still loads
	public static ItemStack of(String key) {
		for (ShaftMaterial material : ShaftMaterial.values()) {
			if (material.name().equalsIgnoreCase(key)) {
				return ShaftIcons.of(material);
			}
		}
		String id = VANILLA.contains(key) ? key : DEFAULT;
		return STACKS.computeIfAbsent(id, each -> new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(each))));
	}

	// a first pick from the name of the route, "ruby loop" gets the ruby
	public static String guess(String name) {
		String text = name.toLowerCase(Locale.ROOT);
		for (String[] word : WORDS) {
			if (text.contains(word[0])) {
				return word[1];
			}
		}
		for (String key : keys()) {
			if (text.contains(key.replace('_', ' ')) || text.contains(key)) {
				return key;
			}
		}
		return DEFAULT;
	}
}
