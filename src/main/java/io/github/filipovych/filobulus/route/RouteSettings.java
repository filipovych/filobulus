package io.github.filipovych.filobulus.route;

import io.github.filipovych.filobulus.shaft.ShaftType;
import java.util.Optional;

public interface RouteSettings {
	boolean autoLoad(ShaftType type);

	// the route the player picked for a shaft, routeKey is a type code or CRYSTAL
	String preferredRouteId(String routeKey);

	Optional<String> tunnelsRouteId();

	// the player's own route for an island, area is its name in the tab list or empty when that is not known
	default Optional<LibraryRoute> customRoute(String area) {
		return Optional.empty();
	}
}
