package io.github.filipovych.filobulus.route;

import java.util.List;

// a set of routes handed around as one string
public record RouteCollection(String name, List<SharedRoute> routes) {
	public RouteCollection {
		name = name == null ? "" : name;
		routes = List.copyOf(routes);
	}
}
