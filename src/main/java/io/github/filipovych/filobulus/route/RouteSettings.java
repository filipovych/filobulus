package io.github.filipovych.filobulus.route;

import io.github.filipovych.filobulus.shaft.ShaftType;
import java.util.List;
import java.util.Optional;

public interface RouteSettings {
	boolean autoLoad(ShaftType type);

	// the route the player picked for a shaft, routeKey is a type code or CRYSTAL
	String preferredRouteId(String routeKey);

	// the player's own route for where they are, places is the tunnels and then the island as the tab list names it
	default Optional<LibraryRoute> customRoute(List<String> places) {
		return Optional.empty();
	}
}
