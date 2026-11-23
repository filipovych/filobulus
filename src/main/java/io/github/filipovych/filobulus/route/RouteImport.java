package io.github.filipovych.filobulus.route;

import io.github.filipovych.filobulus.shaft.ShaftLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// reads whatever the player pasted: our collection string, a coleweight route or a skyblocker export
public final class RouteImport {
	private RouteImport() {
	}

	// routes from the other formats only know their shaft when their name is a type code, the rest come back without one
	public static RouteCollection read(String text) throws RouteFormatException {
		if (text == null || text.isBlank()) {
			throw new RouteFormatException("The clipboard is empty. Copy a route first.");
		}
		String trimmed = text.strip();
		if (trimmed.startsWith(CollectionCodec.PREFIX) || trimmed.startsWith("{")) {
			return CollectionCodec.parse(trimmed);
		}
		List<SharedRoute> routes = new ArrayList<>();
		for (Route route : RouteCodec.parseAny(trimmed, "")) {
			Optional<ShaftLayout> layout = ShaftLayout.named(route.name());
			// a name that is only the type code says nothing more, so it is dropped
			routes.add(new SharedRoute(
					layout.map(ShaftLayout::key).orElse(""), layout.isPresent() ? "" : route.name(), "", route.stops()));
		}
		return new RouteCollection("", routes);
	}

	// gives every route its shaft: one that names its own keeps it, a single one without goes to the target
	// target is the row the player clicked or the shaft they stand in, null when there is neither
	public static List<SharedRoute> assign(RouteCollection read, ShaftLayout target) throws RouteFormatException {
		long loose = read.routes().stream().filter(route -> !route.hasShaft()).count();
		if (loose > 1) {
			throw new RouteFormatException(
					"That has " + loose + " routes and does not say which shafts they are for. Import them one at a time.");
		}
		if (loose == 1 && target == null) {
			throw new RouteFormatException(
					"That route does not say which shaft it is for. Use Import on the row of its shaft.");
		}
		List<SharedRoute> assigned = new ArrayList<>();
		for (SharedRoute route : read.routes()) {
			assigned.add(route.hasShaft()
					? route
					: new SharedRoute(target.key(), route.name(), route.author(), route.stops()));
		}
		return assigned;
	}
}
