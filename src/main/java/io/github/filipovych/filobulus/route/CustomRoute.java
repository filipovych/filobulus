package io.github.filipovych.filobulus.route;

import java.util.List;

// a route of the player's own that is not for a shaft, like a loop in the crystal hollows
// icon is a key of the icon list, area is the island it shows on as the tab list names it, empty means everywhere
// loop starts it over after the last stop, on is whether it is the one in use for its area
public record CustomRoute(LibraryRoute route, String icon, String area, boolean loop, boolean on) {
	public static final String KEY = "CUSTOM";

	public CustomRoute {
		icon = icon == null ? "" : icon;
		area = area == null ? "" : area.strip();
	}

	public static CustomRoute of(String id, String name, String author, String icon, String area, boolean loop,
			List<Waypoint> stops) {
		return new CustomRoute(new LibraryRoute(id, name, KEY, stops, name, author, false), icon, area, loop, false);
	}

	public String id() {
		return route.id();
	}

	public String name() {
		return route.name();
	}

	public List<Waypoint> stops() {
		return route.stops();
	}

	public boolean everywhere() {
		return area.isEmpty();
	}

	// the same route object is kept, so switching it on does not start it over
	public CustomRoute withOn(boolean on) {
		return new CustomRoute(route, icon, area, loop, on);
	}

	public CustomRoute edited(String name, String author, String icon, String area, boolean loop) {
		boolean same = name.equals(route.name()) && author.equals(route.author());
		LibraryRoute kept = same ? route : new LibraryRoute(route.id(), name, KEY, route.stops(), name, author, false);
		return new CustomRoute(kept, icon, area, loop, on);
	}
}
