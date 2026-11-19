package io.github.filipovych.filobulus.route;

import java.util.List;

// id is what you type in /filo load, routeKey is a type code or CRYSTAL
public record LibraryRoute(String id, String title, String routeKey, List<Waypoint> stops) {
	public LibraryRoute {
		stops = List.copyOf(stops);
	}
}
