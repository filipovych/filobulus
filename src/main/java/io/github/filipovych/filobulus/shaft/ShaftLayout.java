package io.github.filipovych.filobulus.shaft;

import static io.github.filipovych.filobulus.shaft.ShaftMaterial.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

// what a route is made for: every type code is its own layout, except that all crystal shafts are the same inside
// key is the type code or CRYSTAL, name is what you type in /filo load
public record ShaftLayout(String key, String title, String name, List<ShaftType> types) {
	// the order of the routes screen, the shafts people mine the most come first
	private static final List<ShaftMaterial> ORDER = List.of(
			JASPER, RUBY, ONYX, AQUAMARINE, CITRINE, PERIDOT, OPAL, TOPAZ, SAPPHIRE, AMETHYST, AMBER, JADE,
			TITANIUM, TUNGSTEN, UMBER, VANGUARD, LITTLEFOOT);

	private static final List<ShaftLayout> ALL = build();

	public ShaftLayout {
		types = List.copyOf(types);
	}

	private static List<ShaftLayout> build() {
		List<ShaftLayout> layouts = new ArrayList<>();
		List<ShaftType> crystal = ShaftType.all().stream().filter(ShaftType::isCrystal).toList();
		for (ShaftMaterial material : ORDER) {
			List<ShaftType> plain = ShaftType.all().stream()
					.filter(type -> type.material() == material && !type.isCrystal()).toList();
			for (ShaftType type : plain) {
				// only gemstones with two layouts get a number
				String number = plain.size() > 1 ? String.valueOf(type.variant().suffix()) : "";
				String word = material.name().toLowerCase(Locale.ROOT);
				layouts.add(new ShaftLayout(
						type.code(),
						number.isEmpty() ? material.displayName() : material.displayName() + " " + number,
						number.isEmpty() ? word : word + "_" + number,
						List.of(type)));
			}
			if (material == JASPER) {
				layouts.add(new ShaftLayout(ShaftType.CRYSTAL_ROUTE_KEY, "Crystal", "crystal", crystal));
			}
		}
		return List.copyOf(layouts);
	}

	public static List<ShaftLayout> all() {
		return ALL;
	}

	public static ShaftLayout of(ShaftType type) {
		return byKey(type.routeKey()).orElseThrow();
	}

	public static Optional<ShaftLayout> byKey(String key) {
		return ALL.stream().filter(layout -> layout.key().equals(key)).findFirst();
	}

	// for names that come with imported routes: a type code, a key, a title or a command name, any case
	public static Optional<ShaftLayout> named(String text) {
		if (text == null) {
			return Optional.empty();
		}
		String wanted = text.strip().replace(' ', '_');
		Optional<ShaftType> type = ShaftType.parse(wanted.toUpperCase(Locale.ROOT));
		if (type.isPresent()) {
			return type.map(ShaftLayout::of);
		}
		return ALL.stream()
				.filter(layout -> layout.key().equalsIgnoreCase(wanted) || layout.name().equalsIgnoreCase(wanted))
				.findFirst();
	}

	public boolean isCrystal() {
		return key.equals(ShaftType.CRYSTAL_ROUTE_KEY);
	}

	// crystal shafts come in seven gemstones, so that layout has no material of its own
	public Optional<ShaftMaterial> material() {
		return isCrystal() ? Optional.empty() : Optional.of(types.get(0).material());
	}
}
