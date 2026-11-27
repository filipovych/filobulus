package io.github.filipovych.filobulus.shaft;

import static io.github.filipovych.filobulus.shaft.ShaftMaterial.*;
import static io.github.filipovych.filobulus.shaft.ShaftVariant.*;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public record ShaftType(ShaftMaterial material, ShaftVariant variant) {
	// all crystal shafts have the same layout, so they share one route
	public static final String CRYSTAL_ROUTE_KEY = "CRYSTAL";

	// only the 34 codes that actually exist, anything else gives empty so we never guess
	private static final Map<String, ShaftType> KNOWN = buildKnown();

	private static Map<String, ShaftType> buildKnown() {
		Map<String, ShaftType> map = new LinkedHashMap<>();
		for (ShaftMaterial m : new ShaftMaterial[] {TOPAZ, SAPPHIRE, AMETHYST, AMBER, JADE}) {
			add(map, m, ONE, TWO);
		}
		for (ShaftMaterial m : new ShaftMaterial[] {RUBY, ONYX, AQUAMARINE, CITRINE, PERIDOT}) {
			add(map, m, ONE, TWO, CRYSTAL);
		}
		for (ShaftMaterial m : new ShaftMaterial[] {JASPER, OPAL}) {
			add(map, m, ONE, CRYSTAL);
		}
		for (ShaftMaterial m : new ShaftMaterial[] {TITANIUM, UMBER, TUNGSTEN, VANGUARD}) {
			add(map, m, ONE);
		}
		add(map, LITTLEFOOT, DEN);
		return Collections.unmodifiableMap(map);
	}

	private static void add(Map<String, ShaftType> map, ShaftMaterial material, ShaftVariant... variants) {
		for (ShaftVariant variant : variants) {
			ShaftType type = new ShaftType(material, variant);
			map.put(type.code(), type);
		}
	}

	public static Collection<ShaftType> all() {
		return KNOWN.values();
	}

	public static Optional<ShaftType> parse(String code) {
		return code == null ? Optional.empty() : Optional.ofNullable(KNOWN.get(code));
	}

	// the type code is the last word of the sidebar line, colour codes already stripped
	public static Optional<ShaftType> fromLastToken(String line) {
		if (line == null) {
			return Optional.empty();
		}
		String trimmed = line.strip();
		int cut = Math.max(trimmed.lastIndexOf(' '), trimmed.lastIndexOf('\u00a0'));
		return parse(trimmed.substring(cut + 1));
	}

	public String code() {
		return material.prefix() + "_" + variant.suffix();
	}

	public boolean isCrystal() {
		return variant == CRYSTAL;
	}

	public String routeKey() {
		return isCrystal() ? CRYSTAL_ROUTE_KEY : code();
	}

	public String displayName() {
		return switch (variant) {
			case CRYSTAL -> material.displayName() + " Crystal";
			case TWO -> material.displayName() + " 2";
			case ONE, DEN -> material.displayName();
		};
	}
}
