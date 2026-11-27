package io.github.filipovych.filobulus.client.profit;

import io.github.filipovych.filobulus.client.hud.ShaftIcons;
import io.github.filipovych.filobulus.profit.MiningItems;
import io.github.filipovych.filobulus.shaft.ShaftMaterial;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

// the item drawn in front of a line of the profit tracker
public final class ProfitIcons {
	// the stacks are never changed, so one per item is enough
	private static final Map<String, ItemStack> STACKS = new HashMap<>();
	// the real items, as they were seen in the inventory
	private static final Map<String, ItemStack> SEEN = new ConcurrentHashMap<>();
	private static final Map<String, String> VANILLA = Map.ofEntries(
			Map.entry("MITHRIL_ORE", "prismarine_crystals"), Map.entry("HARD_STONE", "stone"),
			Map.entry("COBBLESTONE", "cobblestone"), Map.entry("COAL", "coal"), Map.entry("IRON_INGOT", "iron_ingot"),
			Map.entry("GOLD_INGOT", "gold_ingot"), Map.entry("DIAMOND", "diamond"), Map.entry("EMERALD", "emerald"),
			Map.entry("REDSTONE", "redstone"), Map.entry("INK_SACK:4", "lapis_lazuli"), Map.entry("QUARTZ", "quartz"),
			Map.entry("OBSIDIAN", "obsidian"), Map.entry("GLOWSTONE_DUST", "glowstone_dust"), Map.entry("GRAVEL", "gravel"),
			Map.entry("FLINT", "flint"), Map.entry("SAND", "sand"), Map.entry("ENDER_STONE", "end_stone"),
			Map.entry("NETHERRACK", "netherrack"), Map.entry("ICE", "ice"), Map.entry("SULPHUR_ORE", "glowstone_dust"));
	private static final ItemStack COINS = vanilla("gold_nugget");

	private ProfitIcons() {
	}

	public static void remember(String id, ItemStack stack) {
		SEEN.putIfAbsent(id, stack.copyWithCount(1));
	}

	public static ItemStack coins() {
		return COINS;
	}

	public static ItemStack of(String id) {
		ItemStack seen = SEEN.get(id);
		if (seen != null) {
			return seen;
		}
		Optional<String> gem = MiningItems.gem(id);
		if (gem.isPresent()) {
			return ShaftIcons.of(ShaftMaterial.valueOf(gem.get().toUpperCase(Locale.ROOT)));
		}
		// an enchanted item looks like the plain one
		String plain = id.startsWith("ENCHANTED_") ? id.substring("ENCHANTED_".length()) : id;
		for (ShaftMaterial material : new ShaftMaterial[] {ShaftMaterial.TITANIUM, ShaftMaterial.TUNGSTEN, ShaftMaterial.UMBER}) {
			if (plain.startsWith(material.name())) {
				return ShaftIcons.of(material);
			}
		}
		if (plain.startsWith("GLACITE")) {
			return ShaftIcons.of(ShaftMaterial.LITTLEFOOT);
		}
		String vanilla = VANILLA.getOrDefault(plain, VANILLA.get(plain + "_ORE"));
		return vanilla == null ? ItemStack.EMPTY : vanilla(vanilla);
	}

	private static ItemStack vanilla(String id) {
		return STACKS.computeIfAbsent(id, each -> new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(each))));
	}
}
