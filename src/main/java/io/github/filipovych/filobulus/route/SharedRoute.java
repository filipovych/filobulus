package io.github.filipovych.filobulus.route;

import java.util.List;

// a route as it travels in an export, shaft is a route key or empty when the text did not say what it is for
public record SharedRoute(String shaft, String name, String author, List<Waypoint> stops) {
	public SharedRoute {
		shaft = shaft == null ? "" : shaft;
		name = name == null ? "" : name;
		author = author == null ? "" : author;
		stops = List.copyOf(stops);
	}

	public boolean hasShaft() {
		return !shaft.isEmpty();
	}
}
