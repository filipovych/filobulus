package io.github.filipovych.filobulus.route;

import io.github.filipovych.filobulus.route.Location.Area;
import io.github.filipovych.filobulus.shaft.ShaftType;
import java.util.Optional;

// picks the one route that is on screen, knows nothing about minecraft
public final class RouteSelector {
	public sealed interface LoadResult {
		record Loaded(LibraryRoute route) implements LoadResult {
		}

		record NotInMineshaft() implements LoadResult {
		}

		record UnknownRoute(String name) implements LoadResult {
		}
	}

	private final RouteLibrary library;
	private final RouteSettings settings;

	private Location location = Location.elsewhere();
	private LibraryRoute shaftRoute;
	// true once this shaft was handled, by auto-load or by the player
	private boolean decided;

	public RouteSelector(RouteLibrary library, RouteSettings settings) {
		this.library = library;
		this.settings = settings;
	}

	public Optional<LibraryRoute> active() {
		return switch (location.area()) {
			case MINESHAFT -> Optional.ofNullable(shaftRoute);
			case GLACITE_TUNNELS -> settings.tunnelsRouteId().flatMap(library::byId);
			case ELSEWHERE -> Optional.empty();
		};
	}

	// returns the route it just auto-loaded so the caller can announce it
	public Optional<LibraryRoute> onLocation(Location now) {
		boolean newShaft = now.area() == Area.MINESHAFT
				&& (location.area() != Area.MINESHAFT || isDifferentShaft(location, now));
		if (now.area() != Area.MINESHAFT || newShaft) {
			shaftRoute = null;
			decided = false;
		}
		location = now;

		if (now.area() != Area.MINESHAFT || decided || now.shaft().isEmpty()) {
			return Optional.empty();
		}
		decided = true;
		ShaftType type = now.shaft().get();
		if (!settings.autoLoad(type)) {
			return Optional.empty();
		}
		Optional<LibraryRoute> route = library.routeFor(type, settings.preferredRouteId(type));
		shaftRoute = route.orElse(null);
		return route;
	}

	public LoadResult load(String name) {
		if (location.area() != Area.MINESHAFT) {
			return new LoadResult.NotInMineshaft();
		}
		Optional<LibraryRoute> route = library.find(name);
		if (route.isEmpty()) {
			return new LoadResult.UnknownRoute(name);
		}
		shaftRoute = route.get();
		decided = true;
		return new LoadResult.Loaded(route.get());
	}

	public void unload() {
		if (location.area() == Area.MINESHAFT) {
			shaftRoute = null;
			decided = true;
		}
	}

	// two different known types without leaving, so the server moved us to another shaft
	private static boolean isDifferentShaft(Location before, Location now) {
		return before.shaft().isPresent() && now.shaft().isPresent() && !before.shaft().equals(now.shaft());
	}
}
