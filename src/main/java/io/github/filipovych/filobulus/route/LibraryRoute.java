package io.github.filipovych.filobulus.route;

import java.util.List;

// id is what you type in /filo load, routeKey is a type code or CRYSTAL
// name tells the routes of one shaft apart, bundled is false for the ones the player imported
public record LibraryRoute(
		String id, String title, String routeKey, List<Waypoint> stops, String name, String author, boolean bundled) {
	public static final String DEFAULT_NAME = "Default";

	public LibraryRoute {
		stops = List.copyOf(stops);
		author = author == null ? "" : author;
	}

	public LibraryRoute(String id, String title, String routeKey, List<Waypoint> stops) {
		this(id, title, routeKey, stops, DEFAULT_NAME, "", true);
	}

	// same stops in the same order, labels do not matter
	public boolean sameStops(List<Waypoint> other) {
		if (other.size() != stops.size()) {
			return false;
		}
		for (int i = 0; i < stops.size(); i++) {
			Waypoint a = stops.get(i);
			Waypoint b = other.get(i);
			if (a.x() != b.x() || a.y() != b.y() || a.z() != b.z()) {
				return false;
			}
		}
		return true;
	}
}
