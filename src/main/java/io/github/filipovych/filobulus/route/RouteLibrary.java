package io.github.filipovych.filobulus.route;

import io.github.filipovych.filobulus.shaft.ShaftMaterial;
import io.github.filipovych.filobulus.shaft.ShaftType;
import io.github.filipovych.filobulus.shaft.ShaftVariant;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class RouteLibrary {
	private static final String RESOURCE_FOLDER = "/filobulus/routes/";

	// extra routes for a shaft, listed after the default one
	private record Alternative(String routeKey, String suffix, String author) {
	}

	private static final List<Alternative> ALTERNATIVES = List.of(new Alternative("JASP_1", "failsayfe", "Failsayfe"));

	private final Map<String, LibraryRoute> byId = new LinkedHashMap<>();
	private final Map<String, List<LibraryRoute>> byRouteKey = new LinkedHashMap<>();
	private final Map<String, String> aliases = new LinkedHashMap<>();

	public RouteLibrary(Collection<LibraryRoute> routes) {
		for (LibraryRoute route : routes) {
			byId.put(route.id(), route);
			byRouteKey.computeIfAbsent(route.routeKey(), key -> new ArrayList<>()).add(route);
		}
		buildAliases();
	}

	public static RouteLibrary loadBundled() throws IOException, RouteFormatException {
		List<LibraryRoute> routes = new ArrayList<>();
		Map<String, ShaftType> firstTypePerKey = new LinkedHashMap<>();
		for (ShaftType type : ShaftType.all()) {
			firstTypePerKey.putIfAbsent(type.routeKey(), type);
		}
		for (Map.Entry<String, ShaftType> entry : firstTypePerKey.entrySet()) {
			String routeKey = entry.getKey();
			ShaftType type = entry.getValue();
			String file = routeKey.toLowerCase(Locale.ROOT);
			String text = readResource(file + ".json");
			if (text != null) {
				routes.add(new LibraryRoute(defaultId(type), defaultTitle(type), routeKey, RouteCodec.parseColeweight(text)));
			}
			for (Alternative alternative : ALTERNATIVES) {
				if (!alternative.routeKey().equals(routeKey)) {
					continue;
				}
				String altText = readResource(file + "." + alternative.suffix() + ".json");
				if (altText != null) {
					routes.add(new LibraryRoute(
							defaultId(type) + "_" + alternative.suffix(),
							defaultTitle(type) + " (" + alternative.author() + ")",
							routeKey,
							RouteCodec.parseColeweight(altText)));
				}
			}
		}
		return new RouteLibrary(routes);
	}

	public Collection<LibraryRoute> all() {
		return byId.values();
	}

	public Optional<LibraryRoute> byId(String id) {
		return Optional.ofNullable(byId.get(id));
	}

	public List<LibraryRoute> routesFor(ShaftType type) {
		return byRouteKey.getOrDefault(type.routeKey(), List.of());
	}

	public Optional<LibraryRoute> routeFor(ShaftType type, String preferredId) {
		List<LibraryRoute> candidates = routesFor(type);
		for (LibraryRoute route : candidates) {
			if (route.id().equals(preferredId)) {
				return Optional.of(route);
			}
		}
		return candidates.stream().findFirst();
	}

	// takes an id (jasper), a type code (JASP_1) or an alias (jasper_crystal)
	public Optional<LibraryRoute> find(String name) {
		if (name == null) {
			return Optional.empty();
		}
		String key = name.strip().toLowerCase(Locale.ROOT);
		LibraryRoute direct = byId.get(key);
		if (direct != null) {
			return Optional.of(direct);
		}
		return Optional.ofNullable(aliases.get(key)).map(byId::get);
	}

	public Collection<String> ids() {
		return byId.keySet();
	}

	private void buildAliases() {
		for (ShaftType type : ShaftType.all()) {
			List<LibraryRoute> routes = routesFor(type);
			if (routes.isEmpty()) {
				continue;
			}
			String id = routes.get(0).id();
			aliases.putIfAbsent(type.code().toLowerCase(Locale.ROOT), id);
			if (type.isCrystal()) {
				aliases.putIfAbsent(materialName(type.material()) + "_crystal", id);
			}
		}
		// "topaz" works as long as only one topaz layout has a route
		for (ShaftMaterial material : ShaftMaterial.values()) {
			List<LibraryRoute> plain = new ArrayList<>();
			for (ShaftType type : ShaftType.all()) {
				if (type.material() == material && !type.isCrystal()) {
					plain.addAll(routesFor(type).stream().limit(1).toList());
				}
			}
			if (plain.size() == 1) {
				aliases.putIfAbsent(materialName(material), plain.get(0).id());
			}
		}
	}

	private static String defaultId(ShaftType type) {
		if (type.isCrystal()) {
			return "crystal";
		}
		String material = materialName(type.material());
		return hasTwoLayouts(type.material()) ? material + "_" + type.variant().suffix() : material;
	}

	private static String defaultTitle(ShaftType type) {
		if (type.isCrystal()) {
			return "Crystal";
		}
		String material = type.material().displayName();
		return hasTwoLayouts(type.material()) ? material + " " + type.variant().suffix() : material;
	}

	private static boolean hasTwoLayouts(ShaftMaterial material) {
		return ShaftType.all().stream().anyMatch(t -> t.material() == material && t.variant() == ShaftVariant.TWO);
	}

	private static String materialName(ShaftMaterial material) {
		return material.name().toLowerCase(Locale.ROOT);
	}

	private static String readResource(String fileName) throws IOException {
		try (InputStream in = RouteLibrary.class.getResourceAsStream(RESOURCE_FOLDER + fileName)) {
			return in == null ? null : new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}
}
