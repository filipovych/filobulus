package io.github.filipovych.filobulus.route;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

// the player's routes for everything that is not a shaft, in the order they were added
public final class CustomRoutes {
	private static final String FALLBACK_NAME = "Route";

	private final List<CustomRoute> routes = new ArrayList<>();

	public List<CustomRoute> all() {
		return List.copyOf(routes);
	}

	public Optional<CustomRoute> byId(String id) {
		return routes.stream().filter(route -> route.id().equals(id)).findFirst();
	}

	// wantedId keeps the id of a saved route the same after a restart
	public CustomRoute add(String name, String author, String icon, String area, boolean loop, List<Waypoint> stops,
			String wantedId) {
		String shown = name == null || name.isBlank() ? FALLBACK_NAME : name.strip();
		String id = wantedId != null && !wantedId.isBlank() && byId(wantedId).isEmpty() ? wantedId : freeId(shown);
		CustomRoute route = CustomRoute.of(id, shown, author, icon, area, loop, stops);
		routes.add(route);
		return route;
	}

	// puts a changed route in the place of the one with its id
	public void replace(CustomRoute changed) {
		for (int i = 0; i < routes.size(); i++) {
			if (routes.get(i).id().equals(changed.id())) {
				routes.set(i, changed);
			}
		}
	}

	public boolean remove(String id) {
		return routes.removeIf(route -> route.id().equals(id));
	}

	// only one route is in use per area, switching one on switches the others of that area off
	public void use(String id, boolean on) {
		Optional<CustomRoute> wanted = byId(id);
		if (wanted.isEmpty()) {
			return;
		}
		for (int i = 0; i < routes.size(); i++) {
			CustomRoute route = routes.get(i);
			if (route.id().equals(id)) {
				routes.set(i, route.withOn(on));
			} else if (on && route.on() && route.area().equalsIgnoreCase(wanted.get().area())) {
				routes.set(i, route.withOn(false));
			}
		}
	}

	// the route to show where the player is: places is where they are from the smallest out, the tunnels and then the island
	// one made for the first place wins, then one for the next, then one that shows everywhere
	public Optional<CustomRoute> activeIn(List<String> places) {
		for (String place : places) {
			String here = place == null ? "" : place.strip();
			if (here.isEmpty()) {
				continue;
			}
			for (CustomRoute route : routes) {
				if (route.on() && route.area().equalsIgnoreCase(here)) {
					return Optional.of(route);
				}
			}
		}
		return routes.stream().filter(route -> route.on() && route.everywhere()).findFirst();
	}

	public Optional<CustomRoute> activeIn(String area) {
		return activeIn(List.of(area == null ? "" : area));
	}

	private String freeId(String name) {
		String slug = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
		String base = "custom_" + (slug.isEmpty() ? "route" : slug);
		String id = base;
		for (int n = 2; byId(id).isPresent(); n++) {
			id = base + "_" + n;
		}
		return id;
	}
}
