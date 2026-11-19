package io.github.filipovych.filobulus.route;

import java.util.List;

public record Route(String name, List<Waypoint> stops) {
	public Route {
		name = name == null ? "" : name;
		stops = List.copyOf(stops);
	}
}
