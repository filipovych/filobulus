package io.github.filipovych.filobulus.route;

import io.github.filipovych.filobulus.route.Location.Area;
import io.github.filipovych.filobulus.shaft.ShaftType;
import java.util.List;
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

		// the shaft we are in has no route, code is empty while the type is not read yet
		record NoRouteHere(String code) implements LoadResult {
		}
	}

	private final RouteLibrary library;
	private final RouteSettings settings;

	private Location location = Location.elsewhere();
	// the island as the tab list names it, for the player's own routes
	private String areaName = "";
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
			case GLACITE_TUNNELS -> settings.customRoute(List.of(CustomRoute.TUNNELS, areaName));
			case ELSEWHERE -> settings.customRoute(List.of(areaName));
		};
	}

	public void onArea(String name) {
		areaName = name == null ? "" : name;
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
		Optional<LibraryRoute> route = picked(type.routeKey());
		shaftRoute = route.orElse(null);
		return route;
	}

	// /filo load without a name: the route picked for the shaft we are standing in
	public LoadResult loadHere() {
		if (location.area() != Area.MINESHAFT) {
			return new LoadResult.NotInMineshaft();
		}
		Optional<LibraryRoute> route = location.shaft().flatMap(type -> picked(type.routeKey()));
		if (route.isEmpty()) {
			return new LoadResult.NoRouteHere(location.shaft().map(ShaftType::code).orElse(""));
		}
		shaftRoute = route.get();
		decided = true;
		return new LoadResult.Loaded(route.get());
	}

	public LoadResult load(String name) {
		if (location.area() != Area.MINESHAFT) {
			return new LoadResult.NotInMineshaft();
		}
		Optional<LibraryRoute> route = library.find(name);
		if (route.isEmpty()) {
			return new LoadResult.UnknownRoute(name);
		}
		// the first route of a shaft answers to the shaft's name, so jasper means whatever is picked for jasper
		// a longer id like jasper_failsayfe always means exactly that route
		if (library.routesFor(route.get().routeKey()).get(0) == route.get()) {
			route = picked(route.get().routeKey());
		}
		shaftRoute = route.get();
		decided = true;
		return new LoadResult.Loaded(route.get());
	}

	private Optional<LibraryRoute> picked(String routeKey) {
		return library.routeFor(routeKey, settings.preferredRouteId(routeKey));
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
