package io.github.filipovych.filobulus.client.route;

import io.github.filipovych.filobulus.client.FilobulusClient;
import io.github.filipovych.filobulus.client.config.FilobulusConfig;
import io.github.filipovych.filobulus.client.config.RoutesConfig;
import io.github.filipovych.filobulus.client.detect.GameText;
import io.github.filipovych.filobulus.detect.LocationParser;
import io.github.filipovych.filobulus.route.LibraryRoute;
import io.github.filipovych.filobulus.route.Location;
import io.github.filipovych.filobulus.route.RouteProgress;
import io.github.filipovych.filobulus.route.RouteSelector;
import io.github.filipovych.filobulus.route.Waypoint;
import java.util.List;
import net.minecraft.client.Minecraft;

// runs every client tick: works out where we are, keeps the selector up to date and walks the active route
public final class RouteTracker {
	// tab list and sidebar only change now and then, twice a second is plenty
	private static final int LOCATION_INTERVAL = 10;

	private final RouteSelector selector;
	private Location location = Location.elsewhere();
	// dev client only, stands in for a real shaft so routes can be tried in single player
	private Location pretend;
	private LibraryRoute tracked;
	private RouteProgress progress;
	private int ticks;
	// read by the renderer, so it is swapped as a whole and never changed in place
	private volatile List<Waypoint> upcoming = List.of();

	public RouteTracker(RouteSelector selector) {
		this.selector = selector;
	}

	public List<Waypoint> upcoming() {
		return upcoming;
	}

	public Location location() {
		return location;
	}

	public void pretend(Location location) {
		pretend = location;
	}

	// /filo load of the route that is already showing starts it again from the first stop
	public void restart() {
		tracked = null;
	}

	public void tick(Minecraft minecraft) {
		if (minecraft.player == null || minecraft.level == null || !FilobulusConfig.enabled) {
			location = Location.elsewhere();
			selector.onLocation(location);
			upcoming = List.of();
			return;
		}
		if (ticks++ % LOCATION_INTERVAL == 0) {
			location = pretend != null
					? pretend
					: LocationParser.parse(GameText.tabLines(minecraft), GameText.sidebarLines(minecraft));
			selector.onLocation(location).ifPresent(route -> announce(minecraft, route));
		}

		LibraryRoute active = selector.active().orElse(null);
		boolean changed = active != tracked;
		if (changed) {
			tracked = active;
			progress = active == null ? null : new RouteProgress(active.stops());
		}
		if (progress == null) {
			upcoming = List.of();
			return;
		}
		// measured from the feet, the same point f3 shows
		changed |= progress.update(
				minecraft.player.getX(), minecraft.player.getY(), minecraft.player.getZ(), RoutesConfig.reach);
		if (changed) {
			upcoming = List.copyOf(progress.remaining());
		}
		if (progress.finished()) {
			// gemstones do not respawn in a shaft, so a finished route is cleared instead of looped
			selector.unload();
		}
	}

	private void announce(Minecraft minecraft, LibraryRoute route) {
		if (!RoutesConfig.announceLoad) {
			return;
		}
		String code = location.shaft().map(type -> type.code()).orElse(route.title());
		minecraft.gui.hud.getChat().addClientSystemMessage(
				FilobulusClient.chatLine("Loaded " + code + " route (" + route.stops().size() + " stops)"));
	}
}
