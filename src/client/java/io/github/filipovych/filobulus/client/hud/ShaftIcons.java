package io.github.filipovych.filobulus.client.hud;

import io.github.filipovych.filobulus.shaft.ShaftLayout;
import io.github.filipovych.filobulus.shaft.ShaftMaterial;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

// the item shown next to a shaft: the real skyblock item when hypixel's resource pack is loaded, a vanilla look alike otherwise
public final class ShaftIcons {
	private static final String PACK = "hypixel_skyblock";
	// the pack can come and go with the server, so look again now and then
	private static final long RECHECK_MILLIS = 5000;

	private static final Map<ShaftMaterial, ItemStack> SKYBLOCK = new EnumMap<>(ShaftMaterial.class);
	private static final Map<ShaftMaterial, ItemStack> VANILLA = new EnumMap<>(ShaftMaterial.class);
	private static final ItemStack CRYSTAL = skyblock("item/uncategorized/jasper_crystal");
	private static final ItemStack CRYSTAL_VANILLA = new ItemStack(Items.AMETHYST_CLUSTER);

	private static boolean packLoaded;
	private static long checkedAt;

	static {
		for (ShaftMaterial material : ShaftMaterial.values()) {
			SKYBLOCK.put(material, skyblock(model(material)));
			VANILLA.put(material, new ItemStack(vanilla(material)));
		}
	}

	private ShaftIcons() {
	}

	public static ItemStack of(ShaftMaterial material) {
		return (packLoaded() ? SKYBLOCK : VANILLA).get(material);
	}

	public static ItemStack of(ShaftLayout layout) {
		return layout.material().map(ShaftIcons::of).orElseGet(() -> packLoaded() ? CRYSTAL : CRYSTAL_VANILLA);
	}

	// hypixel items are paper with an item model from its own pack, without the pack they would show as a missing texture
	private static boolean packLoaded() {
		long now = System.currentTimeMillis();
		if (now - checkedAt > RECHECK_MILLIS) {
			checkedAt = now;
			Identifier file = Identifier.fromNamespaceAndPath(PACK, "items/" + model(ShaftMaterial.JASPER) + ".json");
			packLoaded = Minecraft.getInstance().getResourceManager().getResource(file).isPresent();
		}
		return packLoaded;
	}

	private static ItemStack skyblock(String model) {
		ItemStack stack = new ItemStack(Items.PAPER);
		stack.set(DataComponents.ITEM_MODEL, Identifier.fromNamespaceAndPath(PACK, model));
		return stack;
	}

	// model names as the pack has them
	private static String model(ShaftMaterial material) {
		String name = material.name().toLowerCase(Locale.ROOT);
		return switch (material) {
			case OPAL -> "item/slayer/blaze/gemstones/fine_opal_gem";
			case TITANIUM -> "item/uncategorized/titanium";
			case TUNGSTEN -> "item/glacite/tungsten/tungsten";
			case UMBER -> "item/glacite/umber/umber";
			case VANGUARD -> "item/glacite/uncategorized/skeleton_key";
			case LITTLEFOOT -> "item/glacite/glacite/glacite";
			default -> "item/collections/gemstone/" + name + "/fine_" + name + "_gem";
		};
	}

	// gemstones as the glass they are mined from, looked up by id because the fields for coloured blocks changed in 26.3
	private static Item vanilla(ShaftMaterial material) {
		String id = switch (material) {
			case JASPER -> "magenta_stained_glass";
			case RUBY -> "red_stained_glass";
			case AMBER -> "orange_stained_glass";
			case TOPAZ -> "yellow_stained_glass";
			case JADE -> "lime_stained_glass";
			case SAPPHIRE -> "light_blue_stained_glass";
			case AMETHYST -> "purple_stained_glass";
			case OPAL -> "white_stained_glass";
			case AQUAMARINE -> "blue_stained_glass";
			case CITRINE -> "brown_stained_glass";
			case ONYX -> "black_stained_glass";
			case PERIDOT -> "green_stained_glass";
			case TITANIUM -> "polished_diorite";
			case TUNGSTEN -> "clay";
			case UMBER -> "terracotta";
			case VANGUARD -> "skeleton_skull";
			case LITTLEFOOT -> "packed_ice";
		};
		return BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(id));
	}
}
