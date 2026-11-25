package io.github.filipovych.filobulus.route;

import io.github.filipovych.filobulus.shaft.ShaftLayout;
import io.github.filipovych.filobulus.shaft.ShaftMaterial;
import io.github.filipovych.filobulus.shaft.ShaftType;
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
			put(route);
		}
		buildAliases();
	}

	private void put(LibraryRoute route) {
		byId.put(route.id(), route);
		byRouteKey.computeIfAbsent(route.routeKey(), key -> new ArrayList<>()).add(route);
	}

	public static RouteLibrary loadBundled() throws IOException, RouteFormatException {
		List<LibraryRoute> routes = new ArrayList<>();
		Map<String, ShaftType> firstTypePerKey = new LinkedHashMap<>();
		for (ShaftType type : ShaftType.all()) {
			firstTypePerKey.putIfAbsent(type.routeKey(), type);
		}
		for (Map.Entry<String, ShaftType> entry : firstTypePerKey.entrySet()) {
			String routeKey = entry.getKey();
			ShaftLayout layout = ShaftLayout.of(entry.getValue());
			String file = routeKey.toLowerCase(Locale.ROOT);
			String text = readResource(file + ".json");
			if (text != null) {
				routes.add(new LibraryRoute(layout.name(), layout.title(), routeKey, RouteCodec.parseColeweight(text)));
			}
			for (Alternative alternative : ALTERNATIVES) {
				if (!alternative.routeKey().equals(routeKey)) {
					continue;
				}
				String altText = readResource(file + "." + alternative.suffix() + ".json");
				if (altText != null) {
					routes.add(new LibraryRoute(
							layout.name() + "_" + alternative.suffix(),
							layout.title() + " (" + alternative.author() + ")",
							routeKey,
							RouteCodec.parseColeweight(altText),
							alternative.author(), alternative.author(), true));
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
		return routesFor(type.routeKey());
	}

	public List<LibraryRoute> routesFor(String routeKey) {
		return List.copyOf(byRouteKey.getOrDefault(routeKey, List.of()));
	}

	public Optional<LibraryRoute> routeFor(ShaftType type, String preferredId) {
		return routeFor(type.routeKey(), preferredId);
	}

	// the route picked for a shaft, or its first one when nothing was picked or the pick is gone
	public Optional<LibraryRoute> routeFor(String routeKey, String preferredId) {
		List<LibraryRoute> candidates = routesFor(routeKey);
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

	// adds a route of the player's own, the same stops twice for one shaft give back the route that is already there
	// wantedId keeps the id of a saved route the same after a restart
	public LibraryRoute addUser(ShaftLayout layout, String name, String author, List<Waypoint> stops, String wantedId) {
		for (LibraryRoute route : routesFor(layout.key())) {
			if (route.sameStops(stops)) {
				return route;
			}
		}
		String shown = name == null || name.isBlank() ? "Imported" : name.strip();
		String id = wantedId != null && !wantedId.isBlank() && isFree(wantedId) ? wantedId : freeId(layout, shown);
		LibraryRoute route = new LibraryRoute(
				id, layout.title() + " (" + shown + ")", layout.key(), stops, shown, author, false);
		put(route);
		buildAliases();
		return route;
	}

	// a new name for a route the player added, the id stays so its pick and its best run are kept
	public Optional<LibraryRoute> renameUser(String id, String name, String author) {
		LibraryRoute route = byId.get(id);
		Optional<ShaftLayout> layout = route == null ? Optional.empty() : ShaftLayout.byKey(route.routeKey());
		if (route == null || route.bundled() || layout.isEmpty()) {
			return Optional.empty();
		}
		String shown = name == null || name.isBlank() ? route.name() : name.strip();
		LibraryRoute renamed = new LibraryRoute(
				id, layout.get().title() + " (" + shown + ")", route.routeKey(), route.stops(), shown, author, false);
		byId.put(id, renamed);
		List<LibraryRoute> siblings = byRouteKey.get(route.routeKey());
		siblings.set(siblings.indexOf(route), renamed);
		return Optional.of(renamed);
	}

	// only routes the player added can go, the bundled ones stay
	public boolean removeUser(String id) {
		LibraryRoute route = byId.get(id);
		if (route == null || route.bundled()) {
			return false;
		}
		byId.remove(id);
		byRouteKey.get(route.routeKey()).remove(route);
		buildAliases();
		return true;
	}

	public List<LibraryRoute> userRoutes() {
		return byId.values().stream().filter(route -> !route.bundled()).toList();
	}

	private boolean isFree(String id) {
		return !byId.containsKey(id) && !aliases.containsKey(id);
	}

	private String freeId(ShaftLayout layout, String name) {
		String slug = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
		String base = layout.name() + "_" + (slug.isEmpty() ? "custom" : slug);
		String id = base;
		for (int n = 2; !isFree(id); n++) {
			id = base + "_" + n;
		}
		return id;
	}

	private void buildAliases() {
		aliases.clear();
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

	private static String materialName(ShaftMaterial material) {
		return material.name().toLowerCase(Locale.ROOT);
	}

	private static String readResource(String fileName) throws IOException {
		try (InputStream in = RouteLibrary.class.getResourceAsStream(RESOURCE_FOLDER + fileName)) {
			return in == null ? null : new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}
}
